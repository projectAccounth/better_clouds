package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.render_types.ModRenderTypes;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;

    // === Per-layer rendering state ===
    private VertexBuffer[] layerBuffers;
    private boolean[] layerBufferEmpty;
    private boolean[] layerNeedsRebuild;
    private int[] prevCellX;
    private int[] prevCellZ;
    private RelativeCameraPos[] prevRelativePos;
    private CloudStatus[] prevStatus;
    private TextureData[] layerTextures;
    private long[] lastFadeRebuild;

    private int maxLayerCount = CloudsConfiguration.MAX_LAYER_COUNT;

    // Random offsets
    private float[][] layerOffsets;

    public CustomCloudRenderer() {
        super();

        this.layerBuffers = new VertexBuffer[maxLayerCount];
        this.layerBufferEmpty = new boolean[maxLayerCount];
        this.layerNeedsRebuild = new boolean[maxLayerCount];
        this.prevCellX = new int[maxLayerCount];
        this.prevCellZ = new int[maxLayerCount];
        this.prevRelativePos = new RelativeCameraPos[maxLayerCount];
        this.prevStatus = new CloudStatus[maxLayerCount];
        this.layerTextures = new TextureData[maxLayerCount];
        this.layerOffsets = new float[maxLayerCount][2];
        this.lastFadeRebuild = new long[maxLayerCount];

        for (int i = 0; i < 10; i++) {
            layerBuffers[i] = new VertexBuffer(com.mojang.blaze3d.buffers.BufferUsage.STATIC_WRITE);
            layerBufferEmpty[i] = true;
            layerNeedsRebuild[i] = true;
            prevCellX[i] = Integer.MIN_VALUE;
            prevCellZ[i] = Integer.MIN_VALUE;
            prevRelativePos[i] = RelativeCameraPos.INSIDE_CLOUDS;
            prevStatus[i] = null;
            lastFadeRebuild[i] = System.currentTimeMillis();
        }

        if (CloudsConfiguration.INSTANCE.CLOUD_RANDOM_LAYERS) {
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                layerOffsets[i][0] = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
                layerOffsets[i][1] = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
            }
        }
    }

    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData baseTexture = optional.orElse(super.prepare(resourceManager, profilerFiller).orElse(null));
        for (int i = 0; i < 10; i++) {
            // Default: same texture per layer (can replace with config getter)
            this.layerTextures[i] = resolveTextureForLayer(i, baseTexture);
            this.layerNeedsRebuild[i] = true;
        }
    }

    @Nullable
    private TextureData resolveTextureForLayer(int layer, @Nullable TextureData fallback) {
        // Hook for per-layer texture overrides from config
        return fallback;
    }

    private static int getColor(long cell) { return (int)(cell >> 4 & 0xFFFFFFFFL); }
    private static boolean isNorthEmpty(long c) { return (c >> 3 & 1L) != 0L; }
    private static boolean isEastEmpty(long c)  { return (c >> 2 & 1L) != 0L; }
    private static boolean isSouthEmpty(long c) { return (c >> 1 & 1L) != 0L; }
    private static boolean isWestEmpty(long c)  { return (c & 1L) != 0L; }

    // === Rendering ===
    @Override
    public void render(int i, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta) {
        int layers = CloudsConfiguration.INSTANCE.CLOUD_LAYERS;
        if (layers <= 0) return;

        double dx = cam.x + tickDelta * 0.03F;
        double dz = cam.z + 3.96F;

        int layerCount = CloudsConfiguration.INSTANCE.CLOUD_LAYERS; // or however many you have
        List<Integer> layerIndices = new ArrayList<>();
        for (int ind = 0; ind < layerCount; ind++) {
            layerIndices.add(ind);
        }

        // compute relative distance for sorting
        layerIndices.sort((a, b) -> {
            float ay = (float)(cloudHeight - cam.y) + a * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float by = (float)(cloudHeight - cam.y) + b * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            // we want farthest drawn first, nearest last
            return Float.compare(Math.abs(by), Math.abs(ay));
        });

        for (int layer : layerIndices) {
            TextureData tex = this.layerTextures[layer];
            if (tex == null) continue;

            double wrapX = tex.width() * CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height() * CELL_SIZE_IN_BLOCKS;
            double dxLayer = dx + layerOffsets[layer][0];
            double dzLayer = dz + layerOffsets[layer][1];
            dxLayer -= Mth.floor(dxLayer / wrapX) * wrapX;
            dzLayer -= Mth.floor(dzLayer / wrapZ) * wrapZ;

            float cloudChunkHeight = HEIGHT_IN_BLOCKS * 
                (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0F);
            float layerY = (float)(cloudHeight - cam.y) + layer * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float relYTop = layerY + cloudChunkHeight;

            RelativeCameraPos layerPos =
                (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                (layerY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            int cellX = Mth.floor(dxLayer / CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dzLayer / CELL_SIZE_IN_BLOCKS);
            float offX = (float)(dxLayer - cellX * CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dzLayer - cellZ * CELL_SIZE_IN_BLOCKS);

            if (layerPos == RelativeCameraPos.INSIDE_CLOUDS && 
                lastFadeRebuild[layer] - System.currentTimeMillis() > 40 &&
                CloudsConfiguration.INSTANCE.IS_ENABLED) {
                markForRebuild(layer);
            }

            if (layerNeedsRebuild[layer] ||
                cellX != prevCellX[layer] || cellZ != prevCellZ[layer] ||
                layerPos != prevRelativePos[layer] || status != prevStatus[layer]) {

                layerNeedsRebuild[layer] = false;
                prevCellX[layer] = cellX;
                prevCellZ[layer] = cellZ;
                prevRelativePos[layer] = layerPos;
                prevStatus[layer] = status;

                RenderType rt = status == CloudStatus.FANCY ? ModRenderTypes.customCloudsFancy : RenderType.flatClouds();
                MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, rt, (float)(relYTop - cloudChunkHeight / 2.0f));
                if (mesh != null) {
                    layerBuffers[layer].bind();
                    layerBuffers[layer].upload(mesh);
                    VertexBuffer.unbind();
                    layerBufferEmpty[layer] = false;
                } else {
                    layerBufferEmpty[layer] = true;
                }
            }

            if (!layerBufferEmpty[layer]) {
                float CUSTOM_BRIGHTNESS = CloudsConfiguration.INSTANCE.BRIGHTNESS;
                 if (CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED)
                    RenderSystem.setShaderColor(CUSTOM_BRIGHTNESS, CUSTOM_BRIGHTNESS,CUSTOM_BRIGHTNESS, 1.0F);
                else 
                    RenderSystem.setShaderColor(ARGB.redFloat(i), ARGB.greenFloat(i), ARGB.blueFloat(i), 1.0F);
                RenderSystem.setShaderFog(new FogParameters(Float.MAX_VALUE, 0.0F, FogShape.SPHERE, 0.0F, 0.0F, 0.0F, 0.0F));

                RenderType rt = status == CloudStatus.FANCY ? ModRenderTypes.customCloudsFancy : RenderType.flatClouds();
                layerBuffers[layer].bind();
                drawWithRenderType(rt, proj, modelView, offX, layerY, offZ, layerBuffers[layer]);
                
                VertexBuffer.unbind();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }
    }

    private void drawWithRenderType(RenderType rt, Matrix4f proj, Matrix4f mv, float ox, float oy, float oz, VertexBuffer buf) {
        rt.setupRenderState();
        var shader = RenderSystem.getShader();
        
        if (shader != null && shader.MODEL_OFFSET != null) {
            shader.MODEL_OFFSET.set(-ox, oy, -oz);
        }
        buf.drawWithShader(proj, mv, shader);
        rt.clearRenderState();
    }

    @Nullable
    private MeshData buildMeshForLayer(TextureData tex, Tesselator tess, int cx, int cz,
                                       CloudStatus status, RelativeCameraPos pos, RenderType rt, float relY) {
        int top = ARGB.colorFromFloat(0.8F, 1, 1, 1);
        int bottom = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int side = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int inner = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);

        BufferBuilder bb = tess.begin(rt.mode(), rt.format());
        buildMesh(tex, pos, bb, cx, cz, side, top, bottom, inner, status == CloudStatus.FANCY, relY);
        return bb.build();
    }

    private void buildMesh(TextureData tex, RelativeCameraPos pos, BufferBuilder bb,
                           int cx, int cz, int bottom, int top, int side, int inner, boolean fancy, float relY) {
        int range = 32;
        long[] cells = tex.cells();
        int w = tex.width();
        int h = tex.height();

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                if (cell != 0L) {
                    int color = getColor(cell);
                    if (fancy) {
                        buildExtrudedCell(pos, bb,
                            ARGB.multiply(bottom, color),
                            ARGB.multiply(top, color),
                            ARGB.multiply(side, color),
                            ARGB.multiply(inner, color),
                            dx, dz, cell, relY);
                    } else {
                        buildFlatCell(bb, ARGB.multiply(top, color), dx, dz);
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder bb, int color, int cx, int cz) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;
        bb.addVertex(x0, 0, z0).setColor(recolor(color, 0));
        bb.addVertex(x0, 0, z1).setColor(recolor(color, 0));
        bb.addVertex(x1, 0, z1).setColor(recolor(color, 0));
        bb.addVertex(x1, 0, z0).setColor(recolor(color, 0));
    }

    private void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int bottomColor, int topColor, int sideColor, int innerColor,
                                   int cx, int cz, long cell, float relY) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = HEIGHT_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;

        float scaledY1 = y1 * (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f);

        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(topColor, scaledY1, pos, relY));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(topColor, scaledY1, pos, relY));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(topColor, scaledY1, pos, relY));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(topColor, scaledY1, pos, relY));
        }
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            bb.addVertex(x1, y0, z0).setColor(recolor(bottomColor, y0, pos, relY));
            bb.addVertex(x1, y0, z1).setColor(recolor(bottomColor, y0, pos, relY));
            bb.addVertex(x0, y0, z1).setColor(recolor(bottomColor, y0, pos, relY));
            bb.addVertex(x0, y0, z0).setColor(recolor(bottomColor, y0, pos, relY));
        }
        // Sides
        if (isNorthEmpty(cell)) {
            bb.addVertex(x0, y0, z0).setColor(recolor(sideColor, y0, pos, relY));
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x1, y0, z0).setColor(recolor(sideColor, y0, pos, relY));
        }
        if (isSouthEmpty(cell)) {
            bb.addVertex(x1, y0, z1).setColor(recolor(sideColor, y0, pos, relY));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x0, y0, z1).setColor(recolor(sideColor, y0, pos, relY));
        }
        if (isWestEmpty(cell)) {
            bb.addVertex(x0, y0, z1).setColor(recolor(sideColor, y0, pos, relY));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x0, y0, z0).setColor(recolor(sideColor, y0, pos, relY));
        }
        if (isEastEmpty(cell)) {
            bb.addVertex(x1, y0, z0).setColor(recolor(sideColor, y0, pos, relY));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos, relY));
            bb.addVertex(x1, y0, z1).setColor(recolor(sideColor, y0, pos, relY));
        }
    }

    private int recolor(int color, float vertexY) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLOR;

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // Apply custom tinting
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        }

        return ARGB.colorFromFloat(baseAlpha, r, g, b);
    }

    private int recolor(int color, float vertexY, RelativeCameraPos pos, float relY) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        float fadeAlpha  = CloudsConfiguration.INSTANCE.FADE_ALPHA;
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLOR;

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // Apply custom tinting
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        }

        if (!CloudsConfiguration.INSTANCE.FADE_ENABLED) return ARGB.colorFromFloat(baseAlpha, g, g, b);

        // Cloud vertical thickness
        float cloudHeight = HEIGHT_IN_BLOCKS * CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE;

        // Normalize vertexY relative to full cloud height [0..1]
        float normalized = Mth.clamp(vertexY / cloudHeight, 0.0f, 1.0f);

        float fade;
        switch (pos) {
            case BELOW_CLOUDS -> {
                // Looking up: bottom solid → top fades
                fade = Mth.lerp(normalized, 1.0f, fadeAlpha);
            }
            case ABOVE_CLOUDS -> {
                // Looking down: top solid → bottom fades
                fade = Mth.lerp(1.0f - normalized, 1.0f, fadeAlpha);
            }
            default -> {
                // Inside: fade depends on how deep inside you are
                // relY = distance from camera to cloud center (positive = above, negative = below)
                float halfHeight = cloudHeight * 0.5f;

                // Map relY to [-1..1] inside cloud
                float insideNorm = Mth.clamp(relY / halfHeight, -1.0f, 1.0f);

                // Make it denser in the middle, fade near boundaries
                // (1 - abs(insideNorm)) gives 1 at center, 0 at edges
                float density = Math.abs(insideNorm);

                // Blend density into fade range
                fade = Mth.lerp(density, fadeAlpha, 1.0f);
            }
        }

        float finalAlpha = baseAlpha * fade;
        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }

    public void markForRebuild(int layer) {
        if (layer >= 0 && layer < layerNeedsRebuild.length) {
            layerNeedsRebuild[layer] = true;
        }
    }

    public void markForRebuild() {
        for (int i = 0; i < layerNeedsRebuild.length; i++) {
            layerNeedsRebuild[i] = true;
        }
    }

    @Override
    public void close() {
        for (VertexBuffer vb : layerBuffers) {
            if (vb != null) vb.close();
        }
    }

    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }
}
