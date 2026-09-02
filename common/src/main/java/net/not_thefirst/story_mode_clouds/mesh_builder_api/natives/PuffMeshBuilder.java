package net.not_thefirst.story_mode_clouds.mesh_builder_api.natives;

import java.util.HashMap;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh.Builder;
import net.not_thefirst.lib.gl_render_system.vertex.GLVertexBuilder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.utils.math.MathUtils;
import net.not_thefirst.story_mode_clouds.utils.math.NoiseCache;
import net.not_thefirst.story_mode_clouds.utils.math.PerlinNoise;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class PuffMeshBuilder implements MeshTypeBuilder {
private static final long PHI = 0x9E3779B97F4A7C15L;
    private static final int SPLITMIX_TABLE_SIZE = 1 << 10;

    private static final float[] sinTable = new float[SPLITMIX_TABLE_SIZE];
    private static final float[] cosTable = new float[SPLITMIX_TABLE_SIZE];
    static {
        for (int i = 0; i < SPLITMIX_TABLE_SIZE; i++) {
            double a = (2.0 * Math.PI * i) / SPLITMIX_TABLE_SIZE;
            sinTable[i] = (float) Math.sin(a);
            cosTable[i] = (float) Math.cos(a);
        }
    }

    private static final Map<Integer, LayerCache> layerCaches = new HashMap<>(4);

    private static long splitmix64(long x) {
        x += PHI;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private static float uint64ToFloat01(long v) {
        long top = (v >>> 11) & ((1L << 53) - 1);
        double d = top * 0x1.0p-53;
        return (float) d;
    }

    private static final class PuffDesc {
        final float localX;
        final float localZ;
        final float hr;
        final float vr;
        final float y; // baked height

        PuffDesc(float localX, float localZ, float hr, float vr, float y) {
            this.localX = localX;
            this.localZ = localZ;
            this.hr = hr;
            this.vr = vr;
            this.y = y;
        }
    }

    private record GenParams(
        int puffCount,
        float minSize,
        float maxSize,
        float heightFactor,
        boolean compactMode,
        int clusterMaxCount,
        float clusterJitterFraction,
        float scatterOverlapFactor,
        boolean noiseEnabled,
        float noiseScale,
        int noiseSeed
    ) {}

    private static final class LayerCache {
        final int texWidth;
        final int texHeight;
        final Object texCellsRef;
        final GenParams params;
        final PuffDesc[][] puffByCell;

        LayerCache(int texWidth, int texHeight, Object texCellsRef, GenParams params) {
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.texCellsRef = texCellsRef;
            this.params = params;
            this.puffByCell = new PuffDesc[texWidth * texHeight][];
        }
    }

    private static GenParams readGenParams(CloudsConfiguration.LayerConfiguration layerConfiguration) {
        var config = layerConfiguration.getTypeConfig("POPULATED_ND");

        return new GenParams(
            (Integer) config.getValue("PUFF_COUNT").value(),
            (Float) config.getValue("PUFF_MIN_SIZE").value(),
            (Float) config.getValue("PUFF_MAX_SIZE").value(),
            (Float) config.getValue("PUFF_HEIGHT_FACTOR").value(),
            (Boolean) config.getValue("COMPACT_MODE").value(),
            (Integer) config.getValue("CLUSTER_MAX_COUNT").value(),
            (Float) config.getValue("CLUSTER_JITTER_FRACTION").value(),
            (Float) config.getValue("SCATTER_OVERLAP_FACTOR").value(),
            (Boolean) config.getValue("NOISE_HEIGHT_ENABLED").value(),
            (Float) config.getValue("NOISE_HEIGHT_SCALE").value(),
            (Integer) config.getValue("NOISE_HEIGHT_SEED").value()
        );
    }

    private static LayerCache ensureCache(Texture.TextureData tex, int currentLayer,
                                           CloudsConfiguration.LayerConfiguration layerConfiguration) {
        GenParams params = readGenParams(layerConfiguration);
        Object cellsRef = tex.cells;

        LayerCache existing = layerCaches.get(currentLayer);
        if (existing != null &&
            existing.texWidth == tex.width &&
            existing.texHeight == tex.height &&
            existing.texCellsRef == cellsRef &&
            existing.params.equals(params)) {
            return existing;
        }

        LayerCache pc = new LayerCache(tex.width, tex.height, cellsRef, params);

        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        final float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;
        final float cellHeight = MeshBuilder.HEIGHT_IN_BLOCKS * layerConfiguration.APPEARANCE.CLOUD_Y_SCALE;
        PerlinNoise noise = params.noiseEnabled() ? NoiseCache.getNoiseInstance(params.noiseSeed()) : null;

        final float maxVr = params.maxSize() * params.heightFactor();
        final float usableHeight = Math.max(cellHeight - maxVr, 0f);

        for (int tz = 0; tz < h; tz++) {
            for (int tx = 0; tx < w; tx++) {
                int idx = tx + tz * w;
                long cell = cells[idx];
                if (cell == 0L) {
                    pc.puffByCell[idx] = null;
                    continue;
                }
                PuffDesc[] arr = new PuffDesc[params.puffCount()];

                long cellSeed = ((tx & 0xFFFFL) << 32) |
                                ((tz & 0xFFFFL) << 16) |
                                (currentLayer & 0xFFFFL);

                float heightFrac = params.noiseEnabled()
                    ? (float) noise.noise(tx * cellSize * params.noiseScale(), tz * cellSize * params.noiseScale()) * 0.5f + 0.5f
                    : 0.5f;
                float centerY = heightFrac * usableHeight + maxVr * 0.5f;

                for (int p = 0; p < params.puffCount(); p++) {
                    long puffSeed = cellSeed + (p * PHI);

                    long s0 = splitmix64(puffSeed);
                    float rr0 = uint64ToFloat01(s0);
                    long s1 = splitmix64(s0);
                    float rr1 = uint64ToFloat01(s1);
                    long s2 = splitmix64(s1);
                    float rr2 = uint64ToFloat01(s2);

                    float hr = MathUtils.lerp(rr0, params.minSize(), params.maxSize());
                    float vr = hr * params.heightFactor();

                    float localX;
                    float localZ;
                    if (params.compactMode()) {
                        int clusterCount = 1 + (int) (uint64ToFloat01(splitmix64(cellSeed)) * params.clusterMaxCount());
                        int clusterIndex = p % clusterCount;
                        long clusterSeed = cellSeed + clusterIndex * 0xD1ABF00DL;
                        long cs0 = splitmix64(clusterSeed);
                        long cs1 = splitmix64(cs0);
                        float cxOffset = uint64ToFloat01(cs0) * cellSize;
                        float czOffset = uint64ToFloat01(cs1) * cellSize;

                        float angleF = rr1 * (float) Math.PI * 2.0f;
                        int idxTable = (int) ((angleF / (2.0f * Math.PI)) * SPLITMIX_TABLE_SIZE) & (SPLITMIX_TABLE_SIZE - 1);
                        float cosv = cosTable[idxTable];
                        float sinv = sinTable[idxTable];

                        float dist = rr2 * (cellSize * params.clusterJitterFraction());
                        float jitterX = cosv * dist;
                        float jitterZ = sinv * dist;

                        localX = cxOffset + jitterX;
                        localZ = czOffset + jitterZ;
                    } else {
                        float overlap = hr * params.scatterOverlapFactor();
                        localX = rr1 * (cellSize + overlap * 2f) - overlap;
                        localZ = rr2 * (cellSize + overlap * 2f) - overlap;
                    }

                    float baseY01 = uint64ToFloat01(splitmix64(s2));
                    float jitter = (baseY01 - 0.5f) * 0.5f;

                    float y = centerY - vr * 0.5f + jitter;
                    y = MathUtils.clamp(y, 0.0f, Math.max(cellHeight - vr, 0f));

                    arr[p] = new PuffDesc(localX, localZ, hr, vr, y);
                }

                pc.puffByCell[idx] = arr;
            }
        }

        layerCaches.put(currentLayer, pc);
        return pc;
    }

    @Override
    public AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz,
        int currentLayer, int colorModifier) {

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.getInstance().getLayer(currentLayer);

        final int RANGE = CloudsConfiguration.getInstance().getCloudGridRange();

        Texture.TextureData tex = state.texture();
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        LayerCache pc = ensureCache(tex, currentLayer, layerConfiguration);
        final float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;

        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, RANGE, w, h);

        for (int dz = -RANGE; dz <= RANGE; dz++) {
            for (int dx = -RANGE; dx <= RANGE; dx++) {

                int cellIdx = wrapped.getCellIndex(dx, dz, RANGE);
                long cell = cells[cellIdx];
                PuffDesc[] puffs = pc.puffByCell[cellIdx];

                if (cell == 0L || puffs == null) continue;

                float baseX = dx * cellSize;
                float baseZ = dz * cellSize;
                int color = Texture.getColor(cell);

                for (int p = 0; p < puffs.length; p++) {
                    PuffDesc description = puffs[p];
                    if (description == null) continue;

                    float px = baseX + description.localX;
                    float pz = baseZ + description.localZ;
                    float py = description.y;
                    float hr = description.hr;
                    float vr = description.vr;

                    int drawColor = layerConfiguration.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR ? color : colorModifier;

                    switch (MeshBuilder.SHAPE) {
                        case CROSS -> drawCross(bb, px, py, pz, hr, vr, currentLayer, drawColor);
                        default -> drawCube(bb, px, py, pz, hr, vr, currentLayer, drawColor);
                    }
                }
            }
        }

        return bb;
    }

    private static void drawCube(
        AbstractStaticMesh.Builder<?, ?> bb,
        float cx, float cy, float cz,
        float hr, float vr,
        int layer, int colorModifier) {

        float x0 = cx - hr;
        float x1 = cx + hr;
        float y0 = cy;
        float y1 = cy + vr;
        float z0 = cz - hr;
        float z1 = cz + hr;

        GLVertexBuilder.quad(bb, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, colorModifier);
        GLVertexBuilder.quad(bb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, colorModifier);
        GLVertexBuilder.quad(bb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, colorModifier);
        GLVertexBuilder.quad(bb, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, colorModifier);
        GLVertexBuilder.quad(bb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, colorModifier);
        GLVertexBuilder.quad(bb, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, colorModifier);
    }

    private static void drawCross(
        AbstractStaticMesh.Builder<?, ?> bb,
        float cx, float cy, float cz,
        float hr, float vr,
        int layer, int colorModifier) {

        float y0 = cy;
        float y1 = cy + vr;

        GLVertexBuilder.quad(bb, cx - hr, y0, cz, cx + hr, y0, cz, cx + hr, y1, cz, cx - hr, y1, cz, colorModifier);
        GLVertexBuilder.quad(bb, cx, y0, cz - hr, cx, y0, cz + hr, cx, y1, cz + hr, cx, y1, cz - hr, colorModifier);
    }

    @Override
    public Builder<?, ?> buildOutline(Builder<?, ?> bb, LayerState state, int cx, int cz, int currentLayer,
            int colorModifier) {
        return bb;
    }
}