package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    public static enum CloudMode {
        NORMAL,
        POPULATED
    }

    @Nullable
    public Optional<TextureData> currentTexture = Optional.empty();
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

    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        this.currentTexture = super.prepare(resourceManager, profilerFiller);
        return this.currentTexture;
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData baseTexture = optional.orElse(prepare(resourceManager, profilerFiller).orElse(null));
        for (LayerState layer : layers) {
            layer.texture = resolveTextureForLayer(layer, baseTexture);
            layer.needsRebuild = true;
        }
    }

    @Nullable
    private TextureData resolveTextureForLayer(LayerState layer, @Nullable TextureData fallback) {
        return fallback;
    }

    private void applyTexture() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if (!currentTexture.isPresent()) {
            prepare(client.getResourceManager(), Profiler.get());
        }

        apply(currentTexture, client.getResourceManager(), Profiler.get());
    }

    @Override
    public void render(int skyColor, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta) {
        double dx = cam.x + tickDelta * 0.03F;
        double dz = cam.z + 3.96F;

        int activeLayers = CloudsConfiguration.INSTANCE.getLayerCount();

        if (activeLayers < 0) return;

        if (activeLayers != layers.size()) {
            rebuildLayerStates();
            applyTexture();
        }

        // Back-to-front ordering using each layer's world Y (LAYER_HEIGHT)
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

            TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width() * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height() * MeshBuilder.CELL_SIZE_IN_BLOCKS;
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

            boolean isCloudFancy = status == CloudStatus.FANCY;
            RenderType rt = isCloudFancy ? RenderType.clouds() : RenderType.flatClouds();

            if (currentLayer.needsRebuild ||
                cellX != currentLayer.prevCellX || cellZ != currentLayer.prevCellZ ||
                layerPos != currentLayer.prevPos || status != currentLayer.prevStatus ||
                skyColor != prevSkyColor) {

                if (currentLayer.buffer != null) {
                    currentLayer.buffer.close();
                }

                currentLayer.needsRebuild = false;
                currentLayer.prevCellX = cellX;
                currentLayer.prevCellZ = cellZ;
                currentLayer.prevPos = layerPos;
                currentLayer.prevStatus = status;
                currentLayer.currentStatus = status;
                prevSkyColor = skyColor;
                
                currentLayer.buffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
                MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, rt, relY, layer, skyColor, offX, offZ);
                if (mesh != null) {
                    currentLayer.buffer.bind();
                    currentLayer.buffer.upload(mesh);
                    VertexBuffer.unbind();
                    currentLayer.bufferEmpty = false;
                } else {
                    currentLayer.bufferEmpty = true;
                }
            }

            if (!currentLayer.bufferEmpty) {
                float CUSTOM_BRIGHTNESS = layerConfiguration.BRIGHTNESS;
                
                if (layerConfiguration.CUSTOM_BRIGHTNESS && layerConfiguration.IS_ENABLED)
                    RenderSystem.setShaderColor(CUSTOM_BRIGHTNESS, CUSTOM_BRIGHTNESS, CUSTOM_BRIGHTNESS, 1.0F);
                else 
                    RenderSystem.setShaderColor(ARGB.redFloat(skyColor), ARGB.greenFloat(skyColor), ARGB.blueFloat(skyColor), 1.0F);

                if (!layerConfiguration.FOG_ENABLED)
                    RenderSystem.setShaderFog(new FogParameters(Float.MAX_VALUE, 0.0F, FogShape.SPHERE, 0.0F, 0.0F, 0.0F, 0.0F));

                currentLayer.buffer.bind();

                if (isCloudFancy)
                    drawWithRenderType(RenderType.cloudsDepthOnly(), proj, modelView, offX, layerY, offZ, currentLayer.buffer);

                drawWithRenderType(rt, proj, modelView, offX, layerY, offZ, currentLayer.buffer);
                
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
                                       CloudStatus status, RelativeCameraPos pos, RenderType rt, 
                                       float relY, int currentLayer,
                                       int skyColor, float offX, float offZ) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);

        BufferBuilder bb = tess.begin(rt.mode(), rt.format());
        MeshTypeBuilder builder = 
            MeshBuilderRegistry.getBuilder(layerConfiguration.MODE.name());

        builder.Build(bb, tex, pos, state, cx, cz, relY, currentLayer, skyColor, offX, offZ);
        return bb.build();
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

    @Override
    public void close() {
        for (LayerState layer : layers) {
            if (layer.buffer != null) layer.buffer.close();
        }
    }

    public static class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public TextureData texture;
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
