package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.util.Mth;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder.PuffMode;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.ColorUtils;

public class PuffMeshBuilder implements MeshTypeBuilder {
    private static final long PHI = 0x9E3779B97F4A7C15L;
    private static final int SPLITMIX_TABLE_SIZE = 1 << 10;

    // ture
    private static final float[] sinTable = new float[SPLITMIX_TABLE_SIZE];
    private static final float[] cosTable = new float[SPLITMIX_TABLE_SIZE];
    static {
        for (int i = 0; i < SPLITMIX_TABLE_SIZE; i++) {
            double a = (2.0 * Math.PI * i) / SPLITMIX_TABLE_SIZE;
            sinTable[i] = (float)Math.sin(a);
            cosTable[i] = (float)Math.cos(a);
        }
    }

    private static final Map<String, LayerCache> LayerCaches = new ConcurrentHashMap<>(4);

    private static long splitmix64(long x) {
        x += PHI;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    // produce a float in [0,1) from a 64-bit value
    private static float uint64ToFloat01(long v) {
        // use top 53 bits to double precision mantissa style, then cast to float
        long top = (v >>> 11) & ((1L << 53) - 1);
        double d = top * 0x1.0p-53; // double in [0,1)
        return (float) d;
    }

    // Puff descriptor (relative to cell origin, px/pz are in [0, CELL_SIZE_IN_BLOCKS) if compact cluster centers used)
    private static final class PuffDesc {
        final float localX; // local offset inside cell (can be slightly negative for overlap)
        final float localZ;
        final float hr;
        final float vr;
        final float baseY; // base Y before tiny per-p puff jitter; constrained so baseY + vr <= PUFF_MAX_VERTICAL
        PuffDesc(float localX, float localZ, float hr, float vr, float baseY) {
            this.localX = localX;
            this.localZ = localZ;
            this.hr = hr;
            this.vr = vr;
            this.baseY = baseY;
        }
    }

    // LayerCache holds cached PuffDesc[] for a texture cell
    @SuppressWarnings("unused")
    private static final class LayerCache {
        final int texWidth, texHeight;
        final Object texCellsRef; // identity of tex.cells for fast change detection
        final int puffCount;
        final PuffDesc[][] puffByCell; // index = (tx + tz * texWidth) to PuffDesc[puffCount]
        final int currentLayer; // cache per layer

        LayerCache(int texWidth, int texHeight, Object texCellsRef, int puffCount, int currentLayer) {
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.texCellsRef = texCellsRef;
            this.puffCount = puffCount;
            this.puffByCell = new PuffDesc[texWidth * texHeight][];
            this.currentLayer = currentLayer;
        }
    }

    // Build or return existing LayerCache for given tex n layer
    private static LayerCache ensureCache(CustomCloudRenderer.TextureData tex, int currentLayer) {
        String key = Integer.toHexString(System.identityHashCode(tex)) + ":" + currentLayer;
        LayerCache cache = LayerCaches.get(key);
        Object cellsRef = tex.cells(); // use reference identity to detect if texture changes
        if (cache != null) {
            if (cache.texWidth == tex.width() && cache.texHeight == tex.height() && cache.texCellsRef == cellsRef) {
                return cache;
            }
            // else regenerate
        }

        // Build new cache
        final int PUFFS_PER_CELL = 6;
        final float PUFF_MIN_SIZE = 1.8f;
        final float PUFF_MAX_SIZE = 5.2f;
        final float PUFF_HEIGHT_FACTOR = 0.45f;
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.INSTANCE.getLayer(currentLayer);
        final float PUFF_MAX_VERTICAL = MeshBuilder.HEIGHT_IN_BLOCKS * (layerConfiguration.IS_ENABLED ? layerConfiguration.CLOUD_Y_SCALE : 1.0f);

        LayerCache pc = new LayerCache(tex.width(), tex.height(), cellsRef, PUFFS_PER_CELL, currentLayer);

        long[] cells = tex.cells();
        int w = tex.width();
        int h = tex.height();

        for (int tz = 0; tz < h; tz++) {
            for (int tx = 0; tx < w; tx++) {
                int idx = tx + tz * w;
                long cell = cells[idx];
                if (cell == 0L) {
                    pc.puffByCell[idx] = null;
                    continue;
                }
                PuffDesc[] arr = new PuffDesc[PUFFS_PER_CELL];

                long cellSeed = (((long)tx & 0xFFFFL) << 32) |
                                (((long)tz & 0xFFFFL) << 16) |
                                (currentLayer & 0xFFFFL);

                // precompute cluster centers
                for (int p = 0; p < PUFFS_PER_CELL; p++) {
                    long puffSeed = cellSeed + (p * PHI);

                    long s0 = splitmix64(puffSeed);
                    float rr0 = uint64ToFloat01(s0);
                    long s1 = splitmix64(s0);
                    float rr1 = uint64ToFloat01(s1);
                    long s2 = splitmix64(s1);
                    float rr2 = uint64ToFloat01(s2);

                    float hr = Mth.lerp(rr0, PUFF_MIN_SIZE, PUFF_MAX_SIZE);
                    float vr = hr * PUFF_HEIGHT_FACTOR;

                    float localX, localZ;
                    if (MeshBuilder.PUFF_MODE == PuffMode.COMPACT) {
                        // clusterCount 1..3, uses one per-cell rng but deterministic cluster centers should be consistent
                        int clusterCount = 1 + (int)(uint64ToFloat01(splitmix64(cellSeed)) * 3.0f);
                        int clusterIndex = p % clusterCount;
                        long clusterSeed = cellSeed + clusterIndex * 0xD1ABF00DL;
                        long cs0 = splitmix64(clusterSeed);
                        long cs1 = splitmix64(cs0);
                        float cxOffset = uint64ToFloat01(cs0) * MeshBuilder.CELL_SIZE_IN_BLOCKS;
                        float czOffset = uint64ToFloat01(cs1) * MeshBuilder.CELL_SIZE_IN_BLOCKS;

                        // per-puff jitter around cluster center
                        float angleF = rr1 * (float)Math.PI * 2.0f;
                        // map angleF to table index
                        int idxTable = (int)((angleF / (2.0f * Math.PI)) * SPLITMIX_TABLE_SIZE) & (SPLITMIX_TABLE_SIZE - 1);
                        float cosv = cosTable[idxTable];
                        float sinv = sinTable[idxTable];

                        float dist = rr2 * (MeshBuilder.CELL_SIZE_IN_BLOCKS * 0.25f);
                        float jitterX = cosv * dist;
                        float jitterZ = sinv * dist;

                        localX = cxOffset + jitterX;
                        localZ = czOffset + jitterZ;
                    } else {
                        float OVERLAP = hr * 1.25f;
                        float localXRaw = rr1 * (MeshBuilder.CELL_SIZE_IN_BLOCKS + OVERLAP * 2f) - OVERLAP;
                        float localZRaw = rr2 * (MeshBuilder.CELL_SIZE_IN_BLOCKS + OVERLAP * 2f) - OVERLAP;
                        localX = localXRaw;
                        localZ = localZRaw;
                    }

                    float baseY = uint64ToFloat01(splitmix64(s2)) * (PUFF_MAX_VERTICAL - vr);
                    baseY = Mth.clamp(baseY, 0.0f, PUFF_MAX_VERTICAL - vr);

                    arr[p] = new PuffDesc(localX, localZ, hr, vr, baseY);
                }

                pc.puffByCell[idx] = arr;
            }
        }

        LayerCaches.put(key, pc);
        return pc;
    }

    @SuppressWarnings("unused")
    @Override
    public BufferBuilder Build(
        BufferBuilder bb, CustomCloudRenderer.TextureData tex, 
        RelativeCameraPos pos, LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor,
        float offX, float offZ) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.INSTANCE.getLayer(currentLayer);
        
        final int RANGE = 32;
        final int PUFFS_PER_CELL = 6;
        final float PUFF_MIN_SIZE = 1.8f;
        final float PUFF_MAX_SIZE = 5.2f;
        final float PUFF_HEIGHT_FACTOR = 0.45f;
        final float Y_JITTER = 0.2f;

        long[] cells = tex.cells();
        int w = tex.width();
        int h = tex.height();

        LayerCache pc = ensureCache(tex, currentLayer);
        final float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;

        for (int dz = -RANGE; dz <= RANGE; dz++) {
            for (int dx = -RANGE; dx <= RANGE; dx++) {

                int tx = Math.floorMod(cx + dx, w);
                int tz = Math.floorMod(cz + dz, h);
                long cell = cells[tx + tz * w];
                if (cell == 0L) continue;

                float baseX = dx * cellSize;
                float baseZ = dz * cellSize;

                int alpha = (int) ((cell >> 36) & 0xFF);
                if (alpha <= 3) continue;

                PuffDesc[] puffs = pc.puffByCell[tx + tz * w];
                if (puffs == null) continue;

                for (int p = 0; p < PUFFS_PER_CELL; p++) {
                    PuffDesc pd = puffs[p];
                    if (pd == null) continue;

                    // world-space puff center
                    float px = baseX + pd.localX;
                    float pz = baseZ + pd.localZ;

                    float hr = pd.hr;
                    float vr = pd.vr;

                    // what did I add the jitter for
                    float py = pd.baseY + (p * Y_JITTER * 0.1f + p * 0.0001f);

                    // clamp as original
                    CloudsConfiguration.LayerConfiguration lc = layerConfiguration;
                    final float PUFF_MAX_VERTICAL = MeshBuilder.HEIGHT_IN_BLOCKS * (lc.IS_ENABLED ? lc.CLOUD_Y_SCALE : 1.0f);
                    py = Mth.clamp(py, 0.0f, PUFF_MAX_VERTICAL - vr);

                    int topColor = ColorUtils.recolor(MeshBuilder.topColor, py + vr, pos, relY, currentLayer, skyColor);
                    int bottomColor = ColorUtils.recolor(MeshBuilder.innerColor, py, pos, relY, currentLayer, skyColor);
                    int sideColorTop = ColorUtils.recolor(MeshBuilder.sideColor, py + vr, pos, relY, currentLayer, skyColor);
                    int sideColorBottom = ColorUtils.recolor(MeshBuilder.sideColor, py, pos, relY, currentLayer, skyColor);

                    float topR = ARGB.redFloat(topColor);
                    float topG = ARGB.greenFloat(topColor);
                    float topB = ARGB.blueFloat(topColor);
                    float topA = ARGB.alphaFloat(topColor);

                    float bottomR = ARGB.redFloat(bottomColor);
                    float bottomG = ARGB.greenFloat(bottomColor);
                    float bottomB = ARGB.blueFloat(bottomColor);
                    float bottomA = ARGB.alphaFloat(bottomColor);

                    float sideTopR = ARGB.redFloat(sideColorTop);
                    float sideTopG = ARGB.greenFloat(sideColorTop);
                    float sideTopB = ARGB.blueFloat(sideColorTop);
                    float sideTopA = ARGB.alphaFloat(sideColorTop);

                    float sideBottomR = ARGB.redFloat(sideColorBottom);
                    float sideBottomG = ARGB.greenFloat(sideColorBottom);
                    float sideBottomB = ARGB.blueFloat(sideColorBottom);
                    float sideBottomA = ARGB.alphaFloat(sideColorBottom);

                    // Draw the puff based on the shape
                    switch (MeshBuilder.SHAPE) {
                        case CROSS:
                            drawCross(bb, px, py, pz, hr, vr, topR, topG, topB, topA, bottomR, bottomG, bottomB, bottomA);
                            break;
                        case CUBE:
                        default:
                            drawCube(bb, px, py, pz, hr, vr, topR, topG, topB, topA, bottomR, bottomG, bottomB, bottomA, sideTopR, sideTopG, sideTopB, sideTopA, sideBottomR, sideBottomG, sideBottomB, sideBottomA);
                            break;
                    }
                }
            }
        }

        return bb;
    }

    private static void drawCube(
        BufferBuilder bb,
        float cx, float cy, float cz,
        float hr, float vr,
        float topR, float topG, float topB, float topA,
        float bottomR, float bottomG, float bottomB, float bottomA,
        float sideTopR, float sideTopG, float sideTopB, float sideTopA,
        float sideBottomR, float sideBottomG, float sideBottomB, float sideBottomA) {

        float x0 = cx - hr, x1 = cx + hr;
        float y0 = cy,      y1 = cy + vr;
        float z0 = cz - hr, z1 = cz + hr;

        // Top face
        bb.addVertex(x0, y1, z1).setColor(topR, topG, topB, topA);
        bb.addVertex(x1, y1, z1).setColor(topR, topG, topB, topA);
        bb.addVertex(x1, y1, z0).setColor(topR, topG, topB, topA);
        bb.addVertex(x0, y1, z0).setColor(topR, topG, topB, topA);

        // Bottom face
        bb.addVertex(x0, y0, z0).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(x1, y0, z0).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(x1, y0, z1).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(x0, y0, z1).setColor(bottomR, bottomG, bottomB, bottomA);

        bb.addVertex(x0, y0, z1).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x1, y0, z1).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x1, y1, z1).setColor(sideTopR, sideTopG, sideTopB, sideTopA);
        bb.addVertex(x0, y1, z1).setColor(sideTopR, sideTopG, sideTopB, sideTopA);

        bb.addVertex(x1, y0, z0).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x0, y0, z0).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x0, y1, z0).setColor(sideTopR, sideTopG, sideTopB, sideTopA);
        bb.addVertex(x1, y1, z0).setColor(sideTopR, sideTopG, sideTopB, sideTopA);

        bb.addVertex(x0, y0, z0).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x0, y0, z1).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x0, y1, z1).setColor(sideTopR, sideTopG, sideTopB, sideTopA);
        bb.addVertex(x0, y1, z0).setColor(sideTopR, sideTopG, sideTopB, sideTopA);

        bb.addVertex(x1, y0, z1).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x1, y0, z0).setColor(sideBottomR, sideBottomG, sideBottomB, sideBottomA);
        bb.addVertex(x1, y1, z0).setColor(sideTopR, sideTopG, sideTopB, sideTopA);
        bb.addVertex(x1, y1, z1).setColor(sideTopR, sideTopG, sideTopB, sideTopA);
    }

    private static void drawCross(
        BufferBuilder bb,
        float cx, float cy, float cz,
        float hr, float vr,
        float topR, float topG, float topB, float topA,
        float bottomR, float bottomG, float bottomB, float bottomA) {

        float y0 = cy;
        float y1 = cy + vr;

        // float diag = hr * 0.7071f;

        bb.addVertex(cx - hr, y0, cz).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(cx + hr, y0, cz).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(cx + hr, y1, cz).setColor(topR, topG, topB, topA);
        bb.addVertex(cx - hr, y1, cz).setColor(topR, topG, topB, topA);

        bb.addVertex(cx, y0, cz - hr).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(cx, y0, cz + hr).setColor(bottomR, bottomG, bottomB, bottomA);
        bb.addVertex(cx, y1, cz + hr).setColor(topR, topG, topB, topA);
        bb.addVertex(cx, y1, cz - hr).setColor(topR, topG, topB, topA);
    }
}