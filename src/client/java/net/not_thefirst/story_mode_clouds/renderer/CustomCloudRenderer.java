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
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    public static enum CloudMode {
        NORMAL,
        POPULATED
    }

    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

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
        return super.prepare(resourceManager, profilerFiller);
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
    public void render(int skyColor, CloudStatus status, float cloudHeight, Vec3 cam, float tickDelta) {
        double baseDx = cam.x + tickDelta * 0.03F;
        double baseDz = cam.z + 3.96F;

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

        boolean fancy = status == CloudStatus.FANCY;
        RenderPipeline pipeline = fancy ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;

        // pick render targets like vanilla
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget cloudsTarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuTexture colorTex = (cloudsTarget != null ? cloudsTarget.getColorTexture() : main.getColorTexture());
        GpuTexture depthTex = (cloudsTarget != null ? cloudsTarget.getDepthTexture() : main.getDepthTexture());

        for (int layer : order) {
            LayerState currentLayer = this.layers.get(layer);

            CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);

            if (!layerConfiguration.LAYER_RENDERED) continue;

            currentLayer.offsetX = layerConfiguration.LAYER_OFFSET_X;
            currentLayer.offsetZ = layerConfiguration.LAYER_OFFSET_Z;

            TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width() * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height() * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double dxLayer = baseDx + currentLayer.offsetX;
            double dzLayer = baseDz + currentLayer.offsetZ;
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
            float transition = Math.max(0.0001f, layerConfiguration.TRANSITION_RANGE);
            float dir = Mth.clamp(relY / transition, -1.0f, 1.0f);

            boolean needs = currentLayer.needsRebuild
                    || cellX    != currentLayer.prevCellX
                    || cellZ    != currentLayer.prevCellZ
                    || layerPos != currentLayer.prevPos
                    || status   != currentLayer.prevStatus
                    || skyColor != prevSkyColor;

            if (!needs && layerConfiguration.FADE_ENABLED) {
                if (Math.abs(relY) <= layerConfiguration.TRANSITION_RANGE) {
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
                currentLayer.needsRebuild  = false;
                currentLayer.prevCellX     = cellX;
                currentLayer.prevCellZ     = cellZ;
                currentLayer.prevPos       = layerPos;
                currentLayer.prevStatus    = status;
                currentLayer.currentStatus = status;
                currentLayer.prevFadeMix   = dir;
                prevSkyColor               = skyColor;

                try (MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, relY, layer, skyColor, offX, offZ)) {
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
                float rr = layerConfiguration.CUSTOM_BRIGHTNESS && layerConfiguration.IS_ENABLED
                        ? layerConfiguration.BRIGHTNESS : ARGB.redFloat(skyColor);
                float gg = layerConfiguration.CUSTOM_BRIGHTNESS && layerConfiguration.IS_ENABLED
                        ? layerConfiguration.BRIGHTNESS : ARGB.greenFloat(skyColor);
                float bb = layerConfiguration.CUSTOM_BRIGHTNESS && layerConfiguration.IS_ENABLED
                        ? layerConfiguration.BRIGHTNESS : ARGB.blueFloat(skyColor);
                RenderSystem.setShaderColor(rr, gg, bb, 1.0F);

                if (fancy) {
                    drawLayer(RenderPipelines.CLOUDS_DEPTH_ONLY, offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, colorTex, depthTex, layer, indices, currentLayer);
                }
                drawLayer(pipeline, offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, colorTex, depthTex, layer, indices, currentLayer);

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
                                       float relYCenter, int currentLayer,
                                       int skyColor, float offX, float offZ) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);


        boolean fancy = status == CloudStatus.FANCY;
        RenderPipeline pipe = fancy ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;

        BufferBuilder bb = tess.begin(pipe.getVertexFormatMode(), pipe.getVertexFormat());
        MeshTypeBuilder builder = 
            MeshBuilderRegistry.getBuilder(layerConfiguration.MODE.name());

        builder.Build(bb, tex, pos, state, cx, cz, relYCenter, currentLayer, skyColor, offX, offZ);
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

    @SuppressWarnings("unused")
    public static class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public TextureData texture;
        public GpuBuffer buffer;
        public int indexCount;
        public boolean needsRebuild;
        public int prevCellX, prevCellZ;
        public RelativeCameraPos prevPos;
        public CloudStatus prevStatus;
        public CloudStatus currentStatus;
        public long lastFadeRebuildMs;
        public float prevFadeMix;
        public boolean bufferEmpty;
        public int layerIndexCount;

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }
    }

    @Environment(EnvType.CLIENT)
    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }
}
