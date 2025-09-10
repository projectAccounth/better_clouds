package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.StoryModeClouds;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.render_types.ModRenderTypes;
import net.not_thefirst.story_mode_clouds.utils.ARGB;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer {
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;
    private LayerState[] layers;

    private int maxLayerCount = CloudsConfiguration.MAX_LAYER_COUNT;

    protected static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.tryBuild("minecraft", "textures/environment/clouds.png");

    @Nullable
    public Optional<TextureData> currentTexture = Optional.empty();

    public CustomCloudRenderer() {
        super();
        
        this.layers = new LayerState[maxLayerCount];

        for (int i = 0; i < 10; i++) {
            layers[i] = new LayerState(i);

            layers[i].buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            layers[i].bufferEmpty = true;
            layers[i].needsRebuild = true;
            layers[i].prevCellX = Integer.MIN_VALUE;
            layers[i].prevCellZ = Integer.MIN_VALUE;
            layers[i].prevPos = RelativeCameraPos.INSIDE_CLOUDS;
            layers[i].prevStatus = null;
            layers[i].lastFadeRebuildMs = System.currentTimeMillis();
        }

        if (CloudsConfiguration.INSTANCE.CLOUD_RANDOM_LAYERS) {
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                layers[i].offsetX = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
                layers[i].offsetZ = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
            }
        }
    }

    public Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        try (InputStream inputStream = resourceManager.open(TEXTURE_LOCATION);
             NativeImage nativeImage = NativeImage.read(inputStream)) {

            int w = nativeImage.getWidth();
            int h = nativeImage.getHeight();
            long[] cells = new long[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = nativeImage.getPixelRGBA(x, y);
                    if (ARGB.alpha(pixel) < 10) {
                        cells[x + y * w] = 0L;
                    } else {
                        boolean north = ARGB.alpha(nativeImage.getPixelRGBA(x, Math.floorMod(y - 1, h))) < 10;
                        boolean east  = ARGB.alpha(nativeImage.getPixelRGBA(Math.floorMod(x + 1, w), y)) < 10;
                        boolean south = ARGB.alpha(nativeImage.getPixelRGBA(x, Math.floorMod(y + 1, h))) < 10;
                        boolean west  = ARGB.alpha(nativeImage.getPixelRGBA(Math.floorMod(x - 1, w), y)) < 10;
                        cells[x + y * w] = packCellData(pixel, north, east, south, west);
                    }
                }
            }

            currentTexture = Optional.of(new TextureData(cells, w, h));
            return currentTexture;
        } catch (IOException e) {
            System.out.println("Failed to load cloud texture" + e);
            return Optional.empty();
        }
    }

    private static long packCellData(int color, boolean north, boolean east, boolean south, boolean west) {
        return (long) color << 4 |
               (north ? 1 : 0) << 3 |
               (east ? 1 : 0) << 2 |
               (south ? 1 : 0) << 1 |
               (west ? 1 : 0);
    }
    
    public void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData baseTexture = optional.orElse(prepare(resourceManager, profilerFiller).orElse(null));
        for (int i = 0; i < 10; i++) {
            layers[i].texture = resolveTextureForLayer(i, baseTexture);
            layers[i].needsRebuild = true;
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

    /**
    * Renders cloud.
    * @param cloudColor The passed in cloud color.
    * @param status The current cloud status.
    * @param cloudHeight The world's height of cloud.
    * @param proj Projection matrix.
    * @param modelView Model view matrix.
    * @param cam The camera's position.
    * @param tickDelta The frame delta.
    */
    public void render(int cloudColor, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta, PoseStack poseStack) {
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
            LayerState currentLayer = this.layers[layer];
            TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width() * CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height() * CELL_SIZE_IN_BLOCKS;
            double dxLayer = dx + currentLayer.offsetX;
            double dzLayer = dz + currentLayer.offsetZ;
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

            long now = System.currentTimeMillis();

            float relY = (float)(relYTop - cloudChunkHeight / 2.0f);

            if (CloudsConfiguration.INSTANCE.IS_ENABLED &&
                CloudsConfiguration.INSTANCE.FADE_ENABLED &&
                Math.abs(relY) <= CloudsConfiguration.INSTANCE.TRANSITION_RANGE && 
                now - currentLayer.lastFadeRebuildMs > 40) {

                currentLayer.needsRebuild = true;
                currentLayer.lastFadeRebuildMs = now;
            }

            if (currentLayer.needsRebuild ||
                cellX != currentLayer.prevCellX || cellZ != currentLayer.prevCellZ ||
                layerPos != currentLayer.prevPos || status != currentLayer.prevStatus) {

                currentLayer.needsRebuild = false;
                currentLayer.prevCellX = cellX;
                currentLayer.prevCellZ = cellZ;
                currentLayer.prevPos = layerPos;
                currentLayer.prevStatus = status;

                RenderType rt = status == CloudStatus.FANCY ? ModRenderTypes.customCloudsFancy : ModRenderTypes.customCloudsFast;
                BufferBuilder.RenderedBuffer mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, rt, relY, layer);
                if (mesh != null) {
                    currentLayer.buffer.bind();
                    currentLayer.buffer.upload(mesh);
                    VertexBuffer.unbind();
                    currentLayer.bufferEmpty = false;
                } else {
                    currentLayer.bufferEmpty = true;
                }
            }

            poseStack.pushPose();
			poseStack.mulPose(proj);
			poseStack.translate(-offX, layerY, -offZ);

            if (!currentLayer.bufferEmpty) {
                float CUSTOM_BRIGHTNESS = CloudsConfiguration.INSTANCE.BRIGHTNESS;

                int appliedColor = 0;
                
                if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
                    appliedColor = cloudColor;
                }
                else if (CloudsConfiguration.INSTANCE.APPEARS_SHADED) {
                    appliedColor = cloudColor;
                }
                else if (CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS) {
                    appliedColor = ARGB.colorFromFloat(1, 1, 1, 1);
                }
                else {
                    appliedColor = cloudColor;
                }
                
                if (CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED)
                    RenderSystem.setShaderColor(
                        ARGB.redFloat(appliedColor) * CUSTOM_BRIGHTNESS,
                        ARGB.greenFloat(appliedColor) * CUSTOM_BRIGHTNESS,
                        ARGB.blueFloat(appliedColor) * CUSTOM_BRIGHTNESS,
                        1.0F
                    );
                else 
                    RenderSystem.setShaderColor(
                        ARGB.redFloat(appliedColor),
                        ARGB.greenFloat(appliedColor),
                        ARGB.blueFloat(appliedColor),
                        1.0F
                    );

                if (!CloudsConfiguration.INSTANCE.FOG_ENABLED) {
                    RenderSystem.setShaderFogStart(Float.MAX_VALUE);
                    RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
                }

                RenderType rt = status == CloudStatus.FANCY ? ModRenderTypes.customCloudsFancy : ModRenderTypes.customCloudsFast;
                currentLayer.buffer.bind();

                RenderSystem.colorMask(false, false, false, false);
                drawWithRenderType(rt, poseStack.last().pose(), modelView, offX, layerY, offZ, currentLayer.buffer);

                RenderSystem.colorMask(true, true, true, true);
                drawWithRenderType(rt, poseStack.last().pose(), modelView, offX, layerY, offZ, currentLayer.buffer);
                
                VertexBuffer.unbind();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }

            poseStack.popPose();
        }
    }

    private void drawWithRenderType(RenderType rt, Matrix4f proj, Matrix4f mv, float ox, float oy, float oz, VertexBuffer buf) {
        rt.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();

        buf.drawWithShader(proj, mv, shader);
        rt.clearRenderState();
    }

    @Nullable
    private BufferBuilder.RenderedBuffer buildMeshForLayer(TextureData tex, Tesselator tess, int cx, int cz,
                                       CloudStatus status, RelativeCameraPos pos, RenderType rt, float relY, int currentLayer) {
        int top = ARGB.colorFromFloat(0.8F, 1, 1, 1);
        int bottom = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int side = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int inner = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);

        BufferBuilder bb = tess.getBuilder();
        bb.begin(rt.mode(), rt.format());
        buildMesh(tex, pos, bb, cx, cz, side, top, bottom, inner, status == CloudStatus.FANCY, relY, currentLayer);
        return bb.end();
    }

    private void buildMesh(TextureData tex, RelativeCameraPos pos, BufferBuilder bb,
                           int cx, int cz, int bottom, int top, int side, int inner, boolean fancy, float relY, int currentLayer) {
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
                            dx, dz, cell, relY, currentLayer);
                    } else {
                        buildFlatCell(bb, ARGB.multiply(top, color), dx, dz, currentLayer);
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
        bb.vertex(x0, 0, z0).color(recolor(color, 0, currentLayer)).endVertex();
        bb.vertex(x0, 0, z1).color(recolor(color, 0, currentLayer)).endVertex();
        bb.vertex(x1, 0, z1).color(recolor(color, 0, currentLayer)).endVertex();
        bb.vertex(x1, 0, z0).color(recolor(color, 0, currentLayer)).endVertex();
    }

    private void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int bottomColor, int topColor, int sideColor, int innerColor,
                                   int cx, int cz, long cell, float relY, int currentLayer) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = HEIGHT_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;

        float scaledY1 = y1 * (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f);

        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            bb.vertex(x0, scaledY1, z0).color(recolor(topColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, scaledY1, z1).color(recolor(topColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z1).color(recolor(topColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z0).color(recolor(topColor, scaledY1, pos, relY, currentLayer)).endVertex();
        }
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            bb.vertex(x1, y0, z0).color(recolor(bottomColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, y0, z1).color(recolor(bottomColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, y0, z1).color(recolor(bottomColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, y0, z0).color(recolor(bottomColor, y0, pos, relY, currentLayer)).endVertex();
        }
        // Sides
        if (isNorthEmpty(cell)) {
            bb.vertex(x0, y0, z0).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, scaledY1, z0).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z0).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, y0, z0).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
        }
        if (isSouthEmpty(cell)) {
            bb.vertex(x1, y0, z1).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z1).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, scaledY1, z1).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, y0, z1).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
        }
        if (isWestEmpty(cell)) {
            bb.vertex(x0, y0, z1).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, scaledY1, z1).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, scaledY1, z0).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x0, y0, z0).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
        }
        if (isEastEmpty(cell)) {
            bb.vertex(x1, y0, z0).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z0).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, scaledY1, z1).color(recolor(sideColor, scaledY1, pos, relY, currentLayer)).endVertex();
            bb.vertex(x1, y0, z1).color(recolor(sideColor, y0, pos, relY, currentLayer)).endVertex();
        }
    }

    private int recolor(int color, float vertexY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

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

    private int recolor(int color, float vertexY, RelativeCameraPos pos, float relY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        float fadeAlpha  = CloudsConfiguration.INSTANCE.FADE_ALPHA;
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // === Tint logic ===
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        } else if (!shaded) {
            r = g = b = 1.0f;
        }

        if (!CloudsConfiguration.INSTANCE.FADE_ENABLED) {
            return ARGB.colorFromFloat(baseAlpha, r, g, b);
        }

        // Cloud vertical thickness
        float cloudHeight = HEIGHT_IN_BLOCKS * CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE;

        // Normalize vertex Y position [0..1]
        float normalizedY = Mth.clamp(vertexY / cloudHeight, 0.0f, 1.0f);

        // === Smooth direction logic ===
        float transitionRange = CloudsConfiguration.INSTANCE.TRANSITION_RANGE; // e.g. 10.0f
        float dir = Mth.clamp(relY / transitionRange, -1.0f, 1.0f);

        // Fade if camera is well below (dir ≈ -1)
        float fadeBelow = Mth.lerp(normalizedY, 1.0f, fadeAlpha);
        // Fade if camera is well above (dir ≈ +1)
        float fadeAbove = Mth.lerp(1.0f - normalizedY, 1.0f, fadeAlpha);

        // Blend between below/above depending on dir
        float mix = (dir + 1.0f) / 2.0f; // [-1..1] → [0..1]
        float fade = Mth.lerp(mix, fadeBelow, fadeAbove);

        // Final alpha
        float finalAlpha = baseAlpha * (1.0f - fade);
        // System.out.printf("Layer %d relY=%.2f dir=%.2f fade=%.2f alpha=%.2f%n", currentLayer, relY, dir, fade, finalAlpha);
        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }

    public void markForRebuild(int layer) {
        if (layer >= 0 && layer < layers.length) {
            layers[layer].needsRebuild = true;
        }
    }

    public void markForRebuild() {
        for (int i = 0; i < layers.length; i++) {
            layers[i].needsRebuild = true;
        }
    }

    
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
        public VertexBuffer buffer;
        public int indexCount;
        public boolean needsRebuild;
        public int prevCellX, prevCellZ;
        public RelativeCameraPos prevPos;
        public CloudStatus prevStatus;
        public long lastFadeRebuildMs;
        public float prevFadeMix;
        public boolean bufferEmpty;

        public LayerState(int index) {
            this.index = index;
        }
    }

    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }

    public record TextureData(long[] cells, int width, int height) { }
}
