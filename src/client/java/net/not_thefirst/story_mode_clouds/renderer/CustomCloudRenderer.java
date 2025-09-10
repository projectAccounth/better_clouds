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
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;

    private LayerState[] layers = new LayerState[CloudsConfiguration.MAX_LAYER_COUNT];

    private final int maxLayerCount;

    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    public CustomCloudRenderer() {
        super();
        this.maxLayerCount = CloudsConfiguration.MAX_LAYER_COUNT;
        for (int i = 0; i < maxLayerCount; i++) {
            layers[i] = new LayerState(i);
        }

        for (int i = 0; i < maxLayerCount; i++) {
            LayerState current      = layers[i];
            current.buffer          = null;
            current.needsRebuild    = true;
            current.layerIndexCount = 0;

            current.prevCellX  = Integer.MIN_VALUE;
            current.prevCellZ  = Integer.MIN_VALUE;
            current.prevPos    = RelativeCameraPos.INSIDE_CLOUDS;
            current.prevStatus = null;

            current.lastFadeRebuildMs = 0L;
            current.prevFadeMix = Float.NaN;

            if (CloudsConfiguration.INSTANCE.CLOUD_RANDOM_LAYERS) {
                Random r = new Random(i * 9127L ^ 0x9E3779B9L); // golden seed
                layers[i].offsetX = r.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
                layers[i].offsetZ = r.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
            } else {
                layers[i].offsetX = 0.0f;
                layers[i].offsetZ = 0.0f;
            }
        }
    }

    // ==== reload ====
    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData base = optional.orElse(null);
        for (int i = 0; i < maxLayerCount; i++) {
            layers[i].texture      = resolveTextureForLayer(i, base);
            layers[i].needsRebuild = true;
        }
    }

    @Nullable
    private TextureData resolveTextureForLayer(int layer, @Nullable TextureData fallback) {
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
            LayerState currentLayer = layers[layer];
            TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            // wrapping
            double wrapX = texW(tex) * CELL_SIZE_IN_BLOCKS;
            double wrapZ = texH(tex) * CELL_SIZE_IN_BLOCKS;

            double dx = baseDx + currentLayer.offsetX;
            double dz = baseDz + currentLayer.offsetZ;
            dx -= Mth.floor(dx / wrapX) * wrapX;
            dz -= Mth.floor(dz / wrapZ) * wrapZ;

            int cellX = Mth.floor(dx / CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dz / CELL_SIZE_IN_BLOCKS);
            float offX = (float)(dx - cellX * CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dz - cellZ * CELL_SIZE_IN_BLOCKS);

            float yScale = CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f;
            float chunkHeight = HEIGHT_IN_BLOCKS * yScale;

            float layerBaseY = (float)(cloudHeight - cam.y) + layer * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float relYTop = layerBaseY + chunkHeight;
            RelativeCameraPos pos =
                    (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                    (layerBaseY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            float relYCenter = layerBaseY + (chunkHeight * 0.5f);
            float transition = Math.max(0.0001f, CloudsConfiguration.INSTANCE.TRANSITION_RANGE);
            float dir = Mth.clamp(relYCenter / transition, -1.0f, 1.0f);

            boolean needs = currentLayer.needsRebuild
                    || cellX  != currentLayer.prevCellX
                    || cellZ  != currentLayer.prevCellZ
                    || pos    != currentLayer.prevPos
                    || status != currentLayer.prevStatus;

            long now = System.currentTimeMillis();
            if (!needs && CloudsConfiguration.INSTANCE.FADE_ENABLED) {
                if (Math.abs(relYCenter) <= CloudsConfiguration.INSTANCE.TRANSITION_RANGE) {
                    float old = currentLayer.prevFadeMix;
                    if ((Float.isNaN(old) || Math.abs(dir - old) > 0.02f) && (now - currentLayer.lastFadeRebuildMs > 40L)) {
                        needs = true;
                        currentLayer.lastFadeRebuildMs = now;
                    }
                } else {
                    if (Float.isNaN(currentLayer.prevFadeMix) || Math.signum(currentLayer.prevFadeMix) != Math.signum(dir)) {
                        needs = true;
                    }
                }
            }

            if (needs) {
                currentLayer.needsRebuild = false;
                currentLayer.prevCellX    = cellX;
                currentLayer.prevCellZ    = cellZ;
                currentLayer.prevPos      = pos;
                currentLayer.prevStatus   = status;
                currentLayer.prevFadeMix  = dir;

                try (MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, pos, relYCenter, layer)) {
                    if (mesh == null) {
                        currentLayer.layerIndexCount = 0;
                    } else {
                        if (currentLayer.buffer != null && currentLayer.buffer.size >= mesh.vertexBuffer().remaining()) {
                            RenderSystem.getDevice().createCommandEncoder()
                                    .writeToBuffer(currentLayer.buffer, mesh.vertexBuffer(), 0);
                        } else {
                            if (currentLayer.buffer != null) currentLayer.buffer.close();
                            currentLayer.buffer = RenderSystem.getDevice().createBuffer(
                                    () -> "Cloud layer " + layer,
                                    BufferType.VERTICES,
                                    BufferUsage.DYNAMIC_WRITE,
                                    mesh.vertexBuffer()
                            );
                        }
                        currentLayer.layerIndexCount = mesh.drawState().indexCount();
                    }
                }
            }

            if (currentLayer.layerIndexCount > 0) {
                float rr = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.redFloat(skyColor);
                float gg = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.greenFloat(skyColor);
                float bb = CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED
                        ? globalBrightness : ARGB.blueFloat(skyColor);
                RenderSystem.setShaderColor(rr, gg, bb, 1.0F);

                if (fancy) {
                    drawLayer(RenderPipelines.CLOUDS_DEPTH_ONLY, offX, layerBaseY, offZ, colorTex, depthTex, layer, indices, currentLayer);
                }
                drawLayer(pipeline, offX, layerBaseY, offZ, colorTex, depthTex, layer, indices, currentLayer);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private void drawLayer(RenderPipeline pipeline,
                           float offX, float offY, float offZ,
                           GpuTexture colorTex, GpuTexture depthTex,
                           int layer,
                           RenderSystem.AutoStorageIndexBuffer indices, LayerState currentLayer) {
        RenderSystem.setModelOffset(-offX, offY, -offZ);

        GpuBuffer indexBuf = indices.getBuffer(currentLayer.layerIndexCount);
        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(colorTex, OptionalInt.empty(), depthTex, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.setIndexBuffer(indexBuf, indices.type());
            pass.setVertexBuffer(0, currentLayer.buffer);
            pass.drawIndexed(0, currentLayer.layerIndexCount);
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

        int c0 = recolor(sideColor, y0, pos, relYCenter, currentLayer);
        int c1 = recolor(sideColor, sy1, pos, relYCenter, currentLayer);

        // sides
        if (isNorthEmpty(cell)) {
            bb.addVertex(x0, y0, z0).setColor(c0);
            bb.addVertex(x0, sy1, z0).setColor(c1);
            bb.addVertex(x1, sy1, z0).setColor(c1);
            bb.addVertex(x1, y0, z0).setColor(c0);
        }
        if (isSouthEmpty(cell)) {
            bb.addVertex(x1, y0, z1).setColor(c0);
            bb.addVertex(x1, sy1, z1).setColor(c1);
            bb.addVertex(x0, sy1, z1).setColor(c1);
            bb.addVertex(x0, y0, z1).setColor(c0);
        }
        if (isWestEmpty(cell)) {
            bb.addVertex(x0, y0, z1).setColor(c0);
            bb.addVertex(x0, sy1, z1).setColor(c1);
            bb.addVertex(x0, sy1, z0).setColor(c1);
            bb.addVertex(x0, y0, z0).setColor(c0);
        }
        if (isEastEmpty(cell)) {
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
        if (layer >= 0 && layer < layers.length) 
            layers[layer].needsRebuild = true;
    }
    @Override
    public void markForRebuild() {
        for (int i = 0; i < layers.length; i++) 
            layers[i].needsRebuild = true;
    }

    @Override
    public void close() {
        for (LayerState layer : layers) {
            if (layer.buffer != null) layer.buffer.close();
        }
    }

    @SuppressWarnings("unused")
    private class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public TextureData texture;
        public GpuBuffer buffer;
        public int indexCount;
        public boolean needsRebuild;
        public int prevCellX, prevCellZ;
        public RelativeCameraPos prevPos;
        public CloudStatus prevStatus;
        public long lastFadeRebuildMs;
        public float prevFadeMix;
        public boolean bufferEmpty;
        public int layerIndexCount;

        public LayerState(int index) {
            this.index = index;
        }
    }

    // mirror vanilla's enum visibility for our own usage
    @Environment(EnvType.CLIENT)
    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }
}
