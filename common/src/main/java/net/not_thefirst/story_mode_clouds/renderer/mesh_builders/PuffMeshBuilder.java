package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;

import net.minecraft.util.Mth;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder.PuffMode;
import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexBuilder;
import net.not_thefirst.story_mode_clouds.utils.MiscUtils.CacheKey;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

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

    private static final Map<CacheKey, LayerCache> LayerCaches = new ConcurrentHashMap<>(4);

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
        final float baseY; // base Y before tiny per-p puff jitter;; constrained so baseY + vr <= PUFF_MAX_VERTICAL
        final float yBias; // stable per-puff depth bias

        PuffDesc(float localX, float localZ, float hr, float vr, float baseY, float yBias) {
            this.localX = localX;
            this.localZ = localZ;
            this.hr = hr;
            this.vr = vr;
            this.baseY = baseY;
            this.yBias = yBias;
        }
    }

    // LayerCache holds cached PuffDesc[] for a texture cell
    @SuppressWarnings("unused")
    private static final class LayerCache {
        final int texWidth;
        final int texHeight;
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
    private static LayerCache ensureCache(Texture.TextureData tex, int currentLayer) {
        CacheKey key = new CacheKey(System.identityHashCode(tex), currentLayer);
        LayerCache cache = LayerCaches.get(key);
        Object cellsRef = tex.cells; // use reference identity to detect if texture changes
        if (cache != null && 
            cache.texWidth == tex.width && 
            cache.texHeight == tex.height && 
            cache.texCellsRef == cellsRef) {
                return cache;
            }
            // else regenerate
        

        // Build new cache
        final int PUFFS_PER_CELL = 6;
        final float PUFF_MIN_SIZE = 1.8f;
        final float PUFF_MAX_SIZE = 5.2f;
        final float PUFF_HEIGHT_FACTOR = 0.45f;
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.getInstance().getLayer(currentLayer);
        final float PUFF_MAX_VERTICAL = MeshBuilder.HEIGHT_IN_BLOCKS * (layerConfiguration.IS_ENABLED ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE : 1.0f);

        LayerCache pc = new LayerCache(tex.width, tex.height, cellsRef, PUFFS_PER_CELL, currentLayer);

        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        for (int tz = 0; tz < h; tz++) {
            for (int tx = 0; tx < w; tx++) {
                int idx = tx + tz * w;
                long cell = cells[idx];
                if (cell == 0L) {
                    pc.puffByCell[idx] = null;
                    continue;
                }
                PuffDesc[] arr = new PuffDesc[PUFFS_PER_CELL];

                long cellSeed = ((tx & 0xFFFFL) << 32) |
                                ((tz & 0xFFFFL) << 16) |
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

                    float localX; 
                    float localZ;
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
                        float overlap = hr * 1.25f;
                        float localXRaw = rr1 * (MeshBuilder.CELL_SIZE_IN_BLOCKS + overlap * 2f) - overlap;
                        float localZRaw = rr2 * (MeshBuilder.CELL_SIZE_IN_BLOCKS + overlap * 2f) - overlap;
                        localX = localXRaw;
                        localZ = localZRaw;
                    }

                    float baseY = uint64ToFloat01(splitmix64(s2)) * (PUFF_MAX_VERTICAL - vr);
                    baseY = Mth.clamp(baseY, 0.0f, PUFF_MAX_VERTICAL - vr);

                    long biasSeed = splitmix64(puffSeed ^ 0xA5A5A5A5A5A5A5A5L);
                    float bias01 = uint64ToFloat01(biasSeed);

                    // 64 discrete layers, total span approx .032 blocks
                    float yBias = ((int)(bias01 * 64)) * 0.0005f;

                    arr[p] = new PuffDesc(localX, localZ, hr, vr, baseY, yBias);
                }

                pc.puffByCell[idx] = arr;
            }
        }

        LayerCaches.put(key, pc);
        return pc;
    }

    @SuppressWarnings("unused")
    @Override
    public BuildingMesh build(
        BuildingMesh bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.getInstance().getLayer(currentLayer);
        
        final int RANGE = CloudsConfiguration.getInstance().CLOUD_GRID_SIZE;
        final int PUFFS_PER_CELL = 6;
        final float PUFF_MIN_SIZE = 1.8f;
        final float PUFF_MAX_SIZE = 5.2f;
        final float PUFF_HEIGHT_FACTOR = 0.45f;

        Texture.TextureData tex = state.texture;
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        LayerCache pc = ensureCache(tex, currentLayer);
        final float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;
        
        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, RANGE, w, h);

        for (int dz = -RANGE; dz <= RANGE; dz++) {
            for (int dx = -RANGE; dx <= RANGE; dx++) {

                int cellIdx = wrapped.getCellIndex(dx, dz, RANGE);
                long cell = cells[cellIdx];

                int alpha = (int) ((cell >> 36) & 0xFF);

                PuffDesc[] puffs = pc.puffByCell[cellIdx];

                if (alpha <= 3 || cell == 0L || puffs == null) continue;

                float baseX = dx * cellSize;
                float baseZ = dz * cellSize;

                for (int p = 0; p < PUFFS_PER_CELL; p++) {
                    PuffDesc description = puffs[p];
                    if (description == null) continue;

                    // world-space puff center
                    float px = baseX + description.localX;
                    float pz = baseZ + description.localZ;

                    float hr = description.hr;
                    float vr = description.vr;
                    float py = description.baseY + description.yBias;

                    CloudsConfiguration.LayerConfiguration lc = layerConfiguration;
                    float maxVerticalHeight = MeshBuilder.HEIGHT_IN_BLOCKS;
                    if (lc.IS_ENABLED) maxVerticalHeight *= lc.APPEARANCE.CLOUD_Y_SCALE;
                    py = Mth.clamp(py, 0.0f, maxVerticalHeight - vr);

                    switch (MeshBuilder.SHAPE) {
                        case CROSS:
                            drawCross(bb, px, py, pz, hr, vr, currentLayer, relY, skyColor);
                            break;
                        case CUBE:
                        default:
                            drawCube(bb, px, py, pz, hr, vr, currentLayer, relY, skyColor);
                            break;
                    }
                }
            }
        }

        return bb;
    }

    private static void drawCube(
        BuildingMesh bb,
        float cx, float cy, float cz,
        float hr, float vr,
        int layer, float relY, int skyColor) {

        float x0 = cx - hr; 
        float x1 = cx + hr;
        float y0 = cy     ; 
        float y1 = cy + vr;
        float z0 = cz - hr; 
        float z1 = cz + hr;

        VertexBuilder.quad(
                bb,
                x0, y1, z1,
                x1, y1, z1,
                x1, y1, z0,
                x0, y1, z0,
                layer, relY, skyColor
        );

        VertexBuilder.quad(
                bb,
                x0, y0, z0,
                x1, y0, z0,
                x1, y0, z1,
                x0, y0, z1,
                layer, relY, skyColor
        );

        VertexBuilder.quad(
                bb,
                x0, y0, z1,
                x1, y0, z1,
                x1, y1, z1,
                x0, y1, z1,
                layer, relY, skyColor
        );

        VertexBuilder.quad(
                bb,
                x1, y0, z0,
                x0, y0, z0,
                x0, y1, z0,
                x1, y1, z0,
                layer, relY, skyColor
        );

        VertexBuilder.quad(
                bb,
                x0, y0, z0,
                x0, y0, z1,
                x0, y1, z1,
                x0, y1, z0,
                layer, relY, skyColor
        );

        VertexBuilder.quad(
                bb,
                x1, y0, z1,
                x1, y0, z0,
                x1, y1, z0,
                x1, y1, z1,
                layer, relY, skyColor
        );
    }

    private static void drawCross(
        BuildingMesh bb,
        float cx, float cy, float cz,
        float hr, float vr,
        int layer, float relY, int skyColor) {

        float y0 = cy;
        float y1 = cy + vr;

        VertexBuilder.quad(bb, 
            cx - hr, y0, cz,
            cx + hr, y0, cz,
            cx + hr, y1, cz,
            cx - hr, y1, cz,
            layer, relY, skyColor
        );

        VertexBuilder.quad(bb, 
            cx, y0, cz - hr,
            cx, y0, cz + hr,
            cx, y1, cz + hr,
            cx, y1, cz - hr,
            layer, relY, skyColor
        );
    }
}