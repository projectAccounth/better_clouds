package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    // ==== constants (match vanilla, keep your scale) ====
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;

    // ==== per-layer state ====
    private final GpuBuffer[] layerBuffers;
    private final int[]       layerIndexCount;
    private final boolean[]   layerNeedsRebuild;

    private final int[]       prevCellX;
    private final int[]       prevCellZ;
    private final RelativeCameraPos[] prevRelativePos;
    private final CloudStatus[] prevStatus;

    private final TextureData[] layerTextures;
    private final float[][]     layerOffsets;       // [layer][0=x,1=z]
    private final long[]        lastFadeRebuildMs;
    private final float[]       prevFadeMix;        // for rebuild-on-change

    private final int maxLayerCount;

    // shared index buffer like vanilla
    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    public CustomCloudRenderer() {
        super();
        this.maxLayerCount = CloudsConfiguration.MAX_LAYER_COUNT;

        layerBuffers       = new GpuBuffer[maxLayerCount];
        layerIndexCount    = new int[maxLayerCount];
        layerNeedsRebuild  = new boolean[maxLayerCount];

        prevCellX          = new int[maxLayerCount];
        prevCellZ          = new int[maxLayerCount];
        prevRelativePos    = new RelativeCameraPos[maxLayerCount];
        prevStatus         = new CloudStatus[maxLayerCount];

        layerTextures      = new TextureData[maxLayerCount];
        layerOffsets       = new float[maxLayerCount][2];
        lastFadeRebuildMs  = new long[maxLayerCount];
        prevFadeMix        = new float[maxLayerCount];

        for (int i = 0; i < maxLayerCount; i++) {
            layerBuffers[i]      = null;
            layerIndexCount[i]   = 0;
            layerNeedsRebuild[i] = true;

            prevCellX[i] = Integer.MIN_VALUE;
            prevCellZ[i] = Integer.MIN_VALUE;
            prevRelativePos[i] = RelativeCameraPos.INSIDE_CLOUDS;
            prevStatus[i] = null;

            lastFadeRebuildMs[i] = 0L;
            prevFadeMix[i] = Float.NaN;

            // random offsets (optional)
            if (CloudsConfiguration.INSTANCE.CLOUD_RANDOM_LAYERS) {
                Random r = new Random(i * 9127L ^ 0x9E3779B9L);
                layerOffsets[i][0] = r.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
                layerOffsets[i][1] = r.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
            } else {
                layerOffsets[i][0] = 0f;
                layerOffsets[i][1] = 0f;
            }
        }
    }

    // ==== reload ====
    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        // Let vanilla load the base texture (records width/height/cells)
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData base = optional.orElse(null);
        for (int i = 0; i < maxLayerCount; i++) {
            layerTextures[i] = resolveTextureForLayer(i, base); // hook for per-layer texture later
            layerNeedsRebuild[i] = true;
        }
    }

    @Nullable
    private TextureData resolveTextureForLayer(int layer, @Nullable TextureData fallback) {
        // Placeholder: return fallback for now; wire per-layer texture here if desired.
        return fallback;
    }

    // ==== helpers to read TextureData record ====
    private static int texW(TextureData t) { return t.width(); }
    private static int texH(TextureData t) { return t.height(); }
    private static long[] texCells(TextureData t) { return t.cells(); }

    private static int getColor(long cell) { return (int)(cell >> 4 & 0xFFFFFFFFL); }
    private static boolean isNorthEmpty(long c){ return (c >> 3 & 1L) != 0L; }
    private static boolean isEastEmpty (long c){ return (c >> 2 & 1L) != 0L; }
    private static boolean isSouthEmpty(long c){ return (c >> 1 & 1L) != 0L; }
    private static boolean isWestEmpty (long c){ return (c      & 1L) != 0L; }

    // ==== main render (1.21.5 signature) ====
    @Override
    public void render(int skyColor, CloudStatus status, float cloudHeight, Vec3 cam, float tickDelta) {
        int cfgLayers = Mth.clamp(CloudsConfiguration.INSTANCE.CLOUD_LAYERS, 0, maxLayerCount);
        if (cfgLayers <= 0) return;

        // camera-based scroll (match vanilla)
        double baseDx = cam.x + tickDelta * 0.03F;
        double baseDz = cam.z + 3.96F;

        // sort layers back-to-front by |camera distance|
        List<Integer> order = new ArrayList<>(cfgLayers);
        for (int i = 0; i < cfgLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float)(cloudHeight - cam.y) + a * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float yb = (float)(cloudHeight - cam.y) + b * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            return Float.compare(Math.abs(yb), Math.abs(ya)); // far first
        });

        // choose pipelines
        boolean fancy = status == CloudStatus.FANCY;
        RenderPipeline pipeline = fancy ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;

        // pick render targets like vanilla
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget cloudsTarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuTexture colorTex = (cloudsTarget != null ? cloudsTarget.getColorTexture() : main.getColorTexture());
        GpuTexture depthTex = (cloudsTarget != null ? cloudsTarget.getDepthTexture() : main.getDepthTexture());

        float globalBrightness = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                ? CloudsConfiguration.INSTANCE.BRIGHTNESS : 1.0f;

        for (int layer : order) {
            TextureData tex = layerTextures[layer];
            if (tex == null) continue;

            // wrapping
            double wrapX = texW(tex) * CELL_SIZE_IN_BLOCKS;
            double wrapZ = texH(tex) * CELL_SIZE_IN_BLOCKS;

            double dx = baseDx + layerOffsets[layer][0];
            double dz = baseDz + layerOffsets[layer][1];
            dx -= Mth.floor(dx / wrapX) * wrapX;
            dz -= Mth.floor(dz / wrapZ) * wrapZ;

            int cellX = Mth.floor(dx / CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dz / CELL_SIZE_IN_BLOCKS);
            float offX = (float)(dx - cellX * CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dz - cellZ * CELL_SIZE_IN_BLOCKS);

            // per-layer Y
            float yScale = CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f;
            float chunkHeight = HEIGHT_IN_BLOCKS * yScale;

            float layerBaseY = (float)(cloudHeight - cam.y) + layer * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float relYTop = layerBaseY + chunkHeight;
            RelativeCameraPos pos =
                    (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                    (layerBaseY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            // center distance (for fade direction blend)
            float relYCenter = layerBaseY + (chunkHeight * 0.5f);

            // compute fadeMix in [-1..1] → blend selector for recolor()
            float transition = Math.max(0.0001f, CloudsConfiguration.INSTANCE.TRANSITION_RANGE);
            float dir = Mth.clamp(relYCenter / transition, -1.0f, 1.0f);

            // throttle mesh rebuilds:
            //  - rebuild when cell origin or status/pos changes
            //  - also rebuild when fade mix changes noticeably near transition, but not faster than every ~40ms
            boolean needs = layerNeedsRebuild[layer]
                    || cellX != prevCellX[layer]
                    || cellZ != prevCellZ[layer]
                    || pos   != prevRelativePos[layer]
                    || status!= prevStatus[layer];

            long now = System.currentTimeMillis();
            if (!needs && CloudsConfiguration.INSTANCE.FADE_ENABLED) {
                // only watch dir when |relYCenter| is within transition range
                if (Math.abs(relYCenter) <= CloudsConfiguration.INSTANCE.TRANSITION_RANGE) {
                    float old = prevFadeMix[layer];
                    // if first time, or changed more than epsilon, and enough time has passed, rebuild
                    if ((Float.isNaN(old) || Math.abs(dir - old) > 0.02f) && (now - lastFadeRebuildMs[layer] > 40L)) {
                        needs = true;
                        lastFadeRebuildMs[layer] = now;
                    }
                } else {
                    // away from transition range: only rebuild once when we cross boundary
                    if (Float.isNaN(prevFadeMix[layer]) || Math.signum(prevFadeMix[layer]) != Math.signum(dir)) {
                        needs = true;
                    }
                }
            }

            if (needs) {
                layerNeedsRebuild[layer] = false;
                prevCellX[layer] = cellX;
                prevCellZ[layer] = cellZ;
                prevRelativePos[layer] = pos;
                prevStatus[layer] = status;
                prevFadeMix[layer] = dir;

                try (MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, pos, relYCenter, layer)) {
                    if (mesh == null) {
                        layerIndexCount[layer] = 0;
                    } else {
                        if (layerBuffers[layer] != null && layerBuffers[layer].size >= mesh.vertexBuffer().remaining()) {
                            RenderSystem.getDevice().createCommandEncoder()
                                    .writeToBuffer(layerBuffers[layer], mesh.vertexBuffer(), 0);
                        } else {
                            if (layerBuffers[layer] != null) layerBuffers[layer].close();
                            layerBuffers[layer] = RenderSystem.getDevice().createBuffer(
                                    () -> "Cloud layer " + layer,
                                    BufferType.VERTICES,
                                    BufferUsage.DYNAMIC_WRITE,
                                    mesh.vertexBuffer()
                            );
                        }
                        layerIndexCount[layer] = mesh.drawState().indexCount();
                    }
                }
            }

            if (layerIndexCount[layer] > 0) {
                // brightness override (keep sky color otherwise)
                float rr = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.redFloat(skyColor);
                float gg = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.greenFloat(skyColor);
                float bb = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.blueFloat(skyColor);
                RenderSystem.setShaderColor(rr, gg, bb, 1.0F);

                // depth-only prepass for fancy (like vanilla)
                if (fancy) {
                    drawLayer(RenderPipelines.CLOUDS_DEPTH_ONLY, offX, layerBaseY, offZ, colorTex, depthTex, layer, indices);
                }
                drawLayer(pipeline, offX, layerBaseY, offZ, colorTex, depthTex, layer, indices);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private void drawLayer(RenderPipeline pipeline,
                           float offX, float offY, float offZ,
                           GpuTexture colorTex, GpuTexture depthTex,
                           int layer,
                           RenderSystem.AutoStorageIndexBuffer indices) {
        RenderSystem.setModelOffset(-offX, offY, -offZ);

        GpuBuffer indexBuf = indices.getBuffer(layerIndexCount[layer]);
        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(colorTex, OptionalInt.empty(), depthTex, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.setIndexBuffer(indexBuf, indices.type());
            pass.setVertexBuffer(0, layerBuffers[layer]);
            pass.drawIndexed(0, layerIndexCount[layer]);
        }

        RenderSystem.resetModelOffset();
    }

    // ==== mesh building (vanilla pattern, but with your recolor + scaling) ====
    @Nullable
    private MeshData buildMeshForLayer(TextureData tex,
                                       Tesselator tess, int cx, int cz,
                                       CloudStatus status, RelativeCameraPos pos,
                                       float relYCenter, int currentLayer) {
        // vanilla-ish base tints
        int top =    ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
        int bottom = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int side =   ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int inner =  ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);

        boolean fancy = status == CloudStatus.FANCY;
        RenderPipeline pipe = fancy ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;

        BufferBuilder bb = tess.begin(pipe.getVertexFormatMode(), pipe.getVertexFormat());
        buildMesh(tex, pos, bb, cx, cz, side, top, bottom, inner, fancy, relYCenter, currentLayer);
        return bb.build();
    }

    private void buildMesh(TextureData tex, RelativeCameraPos pos, BufferBuilder bb,
                           int cx, int cz, int bottom, int top, int side, int inner,
                           boolean fancy, float relYCenter, int currentLayer) {
        final int range = 32;
        long[] cells = texCells(tex);
        int w = texW(tex), h = texH(tex);

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                if (cell != 0L) {
                    int mul = getColor(cell);
                    if (fancy) {
                        buildExtrudedCell(pos, bb,
                                ARGB.multiply(bottom, mul),
                                ARGB.multiply(top,    mul),
                                ARGB.multiply(side,   mul),
                                ARGB.multiply(inner,  mul),
                                dx, dz, cell, relYCenter, currentLayer);
                    } else {
                        buildFlatCell(bb, ARGB.multiply(top, mul), dx, dz, currentLayer);
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder bb, int color, int cx, int cz, int currentLayer) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;
        int c = recolor(color, 0f, currentLayer);
        bb.addVertex(x0, 0, z0).setColor(c);
        bb.addVertex(x0, 0, z1).setColor(c);
        bb.addVertex(x1, 0, z1).setColor(c);
        bb.addVertex(x1, 0, z0).setColor(c);
    }

    private void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int bottomColor, int topColor, int sideColor, int innerColor,
                                   int cx, int cz, long cell, float relYCenter, int currentLayer) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = HEIGHT_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;

        float yScale = CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f;
        float sy1 = y1 * yScale;

        // top
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            int c = recolor(topColor, sy1, pos, relYCenter, currentLayer);
            bb.addVertex(x0, sy1, z0).setColor(c);
            bb.addVertex(x0, sy1, z1).setColor(c);
            bb.addVertex(x1, sy1, z1).setColor(c);
            bb.addVertex(x1, sy1, z0).setColor(c);
        }
        // bottom
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            int c = recolor(bottomColor, y0, pos, relYCenter, currentLayer);
            bb.addVertex(x1, y0, z0).setColor(c);
            bb.addVertex(x1, y0, z1).setColor(c);
            bb.addVertex(x0, y0, z1).setColor(c);
            bb.addVertex(x0, y0, z0).setColor(c);
        }
        // sides
        if (isNorthEmpty(cell)) {
            int c0 = recolor(sideColor, y0, pos, relYCenter, currentLayer);
            int c1 = recolor(sideColor, sy1, pos, relYCenter, currentLayer);
            bb.addVertex(x0, y0, z0).setColor(c0);
            bb.addVertex(x0, sy1, z0).setColor(c1);
            bb.addVertex(x1, sy1, z0).setColor(c1);
            bb.addVertex(x1, y0, z0).setColor(c0);
        }
        if (isSouthEmpty(cell)) {
            int c0 = recolor(sideColor, y0, pos, relYCenter, currentLayer);
            int c1 = recolor(sideColor, sy1, pos, relYCenter, currentLayer);
            bb.addVertex(x1, y0, z1).setColor(c0);
            bb.addVertex(x1, sy1, z1).setColor(c1);
            bb.addVertex(x0, sy1, z1).setColor(c1);
            bb.addVertex(x0, y0, z1).setColor(c0);
        }
        if (isWestEmpty(cell)) {
            int c0 = recolor(sideColor, y0, pos, relYCenter, currentLayer);
            int c1 = recolor(sideColor, sy1, pos, relYCenter, currentLayer);
            bb.addVertex(x0, y0, z1).setColor(c0);
            bb.addVertex(x0, sy1, z1).setColor(c1);
            bb.addVertex(x0, sy1, z0).setColor(c1);
            bb.addVertex(x0, y0, z0).setColor(c0);
        }
        if (isEastEmpty(cell)) {
            int c0 = recolor(sideColor, y0, pos, relYCenter, currentLayer);
            int c1 = recolor(sideColor, sy1, pos, relYCenter, currentLayer);
            bb.addVertex(x1, y0, z0).setColor(c0);
            bb.addVertex(x1, sy1, z0).setColor(c1);
            bb.addVertex(x1, sy1, z1).setColor(c1);
            bb.addVertex(x1, y0, z1).setColor(c0);
        }
    }

    // ==== your recolor logic (per-layer tint + fade) ====
    private int recolor(int inColor, float vertexY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) return inColor;

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(inColor);
        int   tint       = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

        float r = ARGB.redFloat(inColor);
        float g = ARGB.greenFloat(inColor);
        float b = ARGB.blueFloat(inColor);

        if (!shaded && useColor) {
            r = ARGB.redFloat(tint);
            g = ARGB.greenFloat(tint);
            b = ARGB.blueFloat(tint);
        } else if (useColor) {
            r *= ARGB.redFloat(tint);
            g *= ARGB.greenFloat(tint);
            b *= ARGB.blueFloat(tint);
        } else if (!shaded) {
            r = g = b = 1.0f;
        }

        return ARGB.colorFromFloat(baseAlpha, r, g, b);
    }

    private int recolor(int inColor, float vertexY, RelativeCameraPos pos, float relYCenter, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) return inColor;

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(inColor);
        float fadeAlpha  = CloudsConfiguration.INSTANCE.FADE_ALPHA;

        int   tint       = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

        float r = ARGB.redFloat(inColor);
        float g = ARGB.greenFloat(inColor);
        float b = ARGB.blueFloat(inColor);

        // tint rules
        if (!shaded && useColor) {
            r = ARGB.redFloat(tint);
            g = ARGB.greenFloat(tint);
            b = ARGB.blueFloat(tint);
        } else if (useColor) {
            r *= ARGB.redFloat(tint);
            g *= ARGB.greenFloat(tint);
            b *= ARGB.blueFloat(tint);
        } else if (!shaded) {
            r = g = b = 1.0f;
        }

        if (!CloudsConfiguration.INSTANCE.FADE_ENABLED) {
            return ARGB.colorFromFloat(baseAlpha, r, g, b);
        }

        float yScale = CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE;
        float cloudHeight = HEIGHT_IN_BLOCKS * (CloudsConfiguration.INSTANCE.IS_ENABLED ? yScale : 1.0f);

        // vertex position normalized in [0..1]
        float t = Mth.clamp(vertexY / Math.max(0.0001f, cloudHeight), 0.0f, 1.0f);

        // blend direction based on camera center offset
        float transition = Math.max(0.0001f, CloudsConfiguration.INSTANCE.TRANSITION_RANGE);
        float dir = Mth.clamp(relYCenter / transition, -1.0f, 1.0f); // -1 below .. +1 above

        // fades: below = top fades, above = bottom fades
        float fadeBelow = Mth.lerp(t, 1.0f, fadeAlpha);
        float fadeAbove = Mth.lerp(1.0f - t, 1.0f, fadeAlpha);

        // mix fades smoothly as camera crosses the band
        float mix = (dir + 1.0f) * 0.5f; // [-1..1] -> [0..1]
        float fade = Mth.lerp(mix, fadeBelow, fadeAbove);

        float finalAlpha = baseAlpha * (1.0f - fade);
        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }

    // ==== external invalidation ====
    public void markForRebuild(int layer) {
        if (layer >= 0 && layer < layerNeedsRebuild.length) layerNeedsRebuild[layer] = true;
    }
    @Override
    public void markForRebuild() {
        for (int i = 0; i < layerNeedsRebuild.length; i++) layerNeedsRebuild[i] = true;
    }

    // ==== cleanup ====
    @Override
    public void close() {
        for (int i = 0; i < layerBuffers.length; i++) {
            if (layerBuffers[i] != null) {
                layerBuffers[i].close();
                layerBuffers[i] = null;
            }
        }
    }

    // mirror vanilla's enum visibility for our own usage
    @Environment(EnvType.CLIENT)
    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }
}
