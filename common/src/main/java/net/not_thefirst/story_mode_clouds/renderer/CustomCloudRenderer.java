package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.Texture;
import net.not_thefirst.story_mode_clouds.utils.Texture.TextureData;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer {
    public static enum CloudMode {
        NORMAL,
        POPULATED
    }

    protected static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");

    @Nullable
    public Optional<Texture.TextureData> currentTexture = Optional.empty();
    private int prevSkyColor = 0;
    private final List<LayerState> layers = new ArrayList<>();

    public CustomCloudRenderer() {
        super();

        rebuildLayerStates();
    }

    private void rebuildLayerStates() {
        layers.clear();

        int layerCount = CloudsConfiguration.INSTANCE.getLayerCount();
        for (int i = 0; i < layerCount; i++) {
            LayerState layerState = new LayerState();
            layerState.texture = null;
            layerState.needsRebuild = true;
            layerState.prevCellX = Integer.MIN_VALUE;
            layerState.prevCellZ = Integer.MIN_VALUE;
            layerState.prevPos = RelativeCameraPos.INSIDE_CLOUDS;
            layerState.prevStatus = null;
            layerState.lastFadeRebuildMs = System.currentTimeMillis();
            layers.add(layerState);
        }

        for (int i = 0; i < layerCount; i++) {
            LayerState layer = layers.get(i);
            CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.INSTANCE.getLayer(i);
            layer.offsetX = layerConfig.LAYER_OFFSET_X;
            layer.offsetZ = layerConfig.LAYER_OFFSET_Z;
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
    
    public void apply(Optional<Texture.TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Texture.TextureData baseTexture = optional.orElse(prepare(resourceManager, profilerFiller).orElse(null));
        for (LayerState layer : layers) {
            layer.texture = resolveTextureForLayer(layer, baseTexture);
            layer.needsRebuild = true;
        }
    }

    @Nullable
    private Texture.TextureData resolveTextureForLayer(LayerState layer, @Nullable Texture.TextureData fallback) {
        return fallback;
    }

    private void applyTexture() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if (!currentTexture.isPresent()) {
            prepare(client.getResourceManager(), client.getProfiler());
        }

        apply(currentTexture, client.getResourceManager(), client.getProfiler());
    }

    public void render(int cloudColor, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta, PoseStack poseStack) {
        double dx = cam.x + tickDelta * 0.03F;
        double dz = cam.z + 3.96F;

        int activeLayers = CloudsConfiguration.INSTANCE.getLayerCount();

        if (activeLayers < 0) return;

        if (activeLayers != layers.size()) {
            rebuildLayerStates();
            applyTexture();
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < activeLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float)(CloudsConfiguration.INSTANCE.getLayer(a).LAYER_HEIGHT - cam.y);
            float yb = (float)(CloudsConfiguration.INSTANCE.getLayer(b).LAYER_HEIGHT - cam.y);
            return Float.compare(Math.abs(yb), Math.abs(ya));
        });

        for (int layer : order) {
            LayerState currentLayer = this.layers.get(layer);

            CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);

            if (!layerConfiguration.LAYER_RENDERED) continue;

            currentLayer.offsetX = layerConfiguration.LAYER_OFFSET_X;
            currentLayer.offsetZ = layerConfiguration.LAYER_OFFSET_Z;

            Texture.TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double dxLayer = dx + currentLayer.offsetX;
            double dzLayer = dz + currentLayer.offsetZ;
            dxLayer -= Mth.floor(dxLayer / wrapX) * wrapX;
            dzLayer -= Mth.floor(dzLayer / wrapZ) * wrapZ;

            float cloudChunkHeight = MeshBuilder.HEIGHT_IN_BLOCKS * 
                (layerConfiguration.IS_ENABLED ? layerConfiguration.CLOUD_Y_SCALE : 1.0F);
            float layerY = (float)( CloudsConfiguration.INSTANCE.getLayer(layer).LAYER_HEIGHT - cam.y);
            float relYTop = layerY + cloudChunkHeight;

            RelativeCameraPos layerPos =
                (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                (layerY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            int cellX = Mth.floor(dxLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dzLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offX = (float)(dxLayer - cellX * MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dzLayer - cellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            long now = System.currentTimeMillis();

            float relY = (float)(relYTop - cloudChunkHeight / 2.0f);

            if (layerConfiguration.IS_ENABLED &&
                layerConfiguration.FADE_ENABLED &&
                Math.abs(relY) <= layerConfiguration.TRANSITION_RANGE && 
                now - currentLayer.lastFadeRebuildMs > 40) {

                currentLayer.needsRebuild = true;
                currentLayer.lastFadeRebuildMs = now;
            }

            if (layerConfiguration.IS_ENABLED &&
                layerConfiguration.CUSTOM_BRIGHTNESS &&
                cloudColor != prevSkyColor) {

                currentLayer.needsRebuild = true;
                if (layer == 0) {
                    prevSkyColor = cloudColor;
                }
            }

            if (currentLayer.needsRebuild ||
                cellX != currentLayer.prevCellX || cellZ != currentLayer.prevCellZ ||
                layerPos != currentLayer.prevPos || status != currentLayer.prevStatus ||
                cloudColor != prevSkyColor) {

                currentLayer.needsRebuild = false;
                currentLayer.prevCellX = cellX;
                currentLayer.prevCellZ = cellZ;
                currentLayer.prevPos = layerPos;
                currentLayer.prevStatus = status;
                currentLayer.currentStatus = status;
                prevSkyColor = cloudColor;

                if (currentLayer.buffer != null) {
                    currentLayer.buffer.close();
                }

                currentLayer.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, relY, layer, cloudColor, offX, offZ);
                
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
                RenderSystem.setShaderColor(1, 1, 1, 1);
                float originalFogStart = RenderSystem.getShaderFogStart();
                float originalFogEnd   = RenderSystem.getShaderFogEnd();

                if (!layerConfiguration.FOG_ENABLED) {
                    RenderSystem.setShaderFogEnd(262144);
                    RenderSystem.setShaderFogStart(262144);
                }

                currentLayer.buffer.bind();

                if (status == CloudStatus.FANCY) {
                    drawWithRenderType(RenderType.cloudsDepthOnly(), poseStack.last().pose(), modelView, offX, layerY, offZ, currentLayer.buffer);
                }

                drawWithRenderType(RenderType.clouds(), poseStack.last().pose(), modelView, offX, layerY, offZ, currentLayer.buffer);

                VertexBuffer.unbind();
                RenderSystem.setShaderFogEnd(originalFogStart);
                RenderSystem.setShaderFogEnd(originalFogEnd);
            }

            poseStack.popPose();
        }
    }

    private void drawWithRenderType(RenderType rt, Matrix4f proj, Matrix4f mv, float ox, float oy, float oz, VertexBuffer buf) {
        rt.setupRenderState();
        buf.drawWithShader(proj, mv, GameRenderer.getPositionColorShader());
        rt.clearRenderState();
    }

    @Nullable
    private MeshData buildMeshForLayer(
            Texture.TextureData tex,
            Tesselator tess,
            int cx, int cz,
            CloudStatus status,
            RelativeCameraPos pos,
            float relY,
            int currentLayer,
            int skyColor,
            float offX, float offZ)
    {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);

        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        MeshTypeBuilder builder = 
            MeshBuilderRegistry.getBuilder(layerConfiguration.MODE.name());

        builder.Build(bb, tex, pos, state, cx, cz, relY, currentLayer, skyColor, offX, offZ);

        return bb.buildOrThrow();
    }

    public void markForRebuild(int layer) {
        if (layer >= 0 && layer < layers.size()) {
            layers.get(layer).needsRebuild = true;
        }
    }

    public void markForRebuild() {
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).needsRebuild = true;
        }
    }
    
    public void close() {
        for (LayerState layer : layers) {
            if (layer.buffer != null) layer.buffer.close();
        }
    }

    public class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public Texture.TextureData texture;
        public VertexBuffer buffer;
        public int indexCount;
        public boolean needsRebuild;
        public int prevCellX, prevCellZ;
        public RelativeCameraPos prevPos;
        public CloudStatus prevStatus;
        public CloudStatus currentStatus;
        public long lastFadeRebuildMs;
        public float prevFadeMix;
        public boolean bufferEmpty;

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }
    }

    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }
}
