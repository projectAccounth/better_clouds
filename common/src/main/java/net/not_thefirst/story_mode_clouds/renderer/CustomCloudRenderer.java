package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingType;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider.WeatherState;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CustomCloudRenderer implements AutoCloseable {

    private final List<LayerState> layers = new ArrayList<>();
    private Optional<Texture.TextureData> currentTexture = Optional.empty();
    
    protected static final IdentifierWrapper TEXTURE_LOCATION = 
        IdentifierWrapper.of("minecraft", "textures/environment/clouds.png");

    public CustomCloudRenderer() {
        rebuildLayerStates();
    }

    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    private void rebuildLayerStates() {
        layers.clear();
        currentTexture = Optional.empty();

        int layerCount = CloudsConfiguration.getInstance().getLayerCount();
        for (int i = 0; i < layerCount; i++) {
            LayerState layerState = new LayerState();
            layerState.texture = null;
            layerState.needsRebuild = true;
            layerState.cellInitialized = false;
            layerState.prevStatus = null;
            layers.add(layerState);
        }

        for (int i = 0; i < layerCount; i++) {
            LayerState layer = layers.get(i);
            CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.getInstance().getLayer(i);
            layer.offsetX = layerConfig.APPEARANCE.LAYER_OFFSET_X;
            layer.offsetZ = layerConfig.APPEARANCE.LAYER_OFFSET_Z;
        }
    }

    @SuppressWarnings("unused")
    public Optional<Texture.TextureData> prepare(
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller,
        IdentifierWrapper textureLocation
    ) {
        try (InputStream inputStream = 
            resourceManager.open(textureLocation.getDelegate())) {

            this.currentTexture = Texture.buildTexture(inputStream);
            return this.currentTexture;
        }
        catch (IOException exception) {
            exception.printStackTrace();
            LoggerProvider.get().info("Failed to build cloud texture, discarding");
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    public void apply(Optional<Texture.TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Texture.TextureData baseTexture = optional.orElse(null);
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
            prepare(client.getResourceManager(), Profiler.get(), TEXTURE_LOCATION);
        }

        apply(currentTexture, client.getResourceManager(), Profiler.get());
    }

    public Optional<Texture.TextureData> getCurrentTexture() {
        return this.currentTexture;
    }

    public void render(CloudStatus status, Vec3 cam, float tickDelta) {
        double baseDx = cam.x;
        double baseDz = cam.z + 3.96F;

        int activeLayers = CloudsConfiguration.getInstance().getLayerCount();
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;

        if (level == null) return;
        if (activeLayers <= 0) return;

        if (activeLayers != layers.size()) {
            rebuildLayerStates();
            applyTexture();
        }

        WeatherState weather = WeatherState.from(level, tickDelta);
        final int skyColor = CloudColorProvider.getCloudColor(weather);
        final long timeTicks = level.getGameTime();
        final float time = (timeTicks + tickDelta) / 20.0F;

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < activeLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float) (CloudsConfiguration.getInstance().getLayer(a).LAYER_HEIGHT - cam.y);
            float yb = (float) (CloudsConfiguration.getInstance().getLayer(b).LAYER_HEIGHT - cam.y);
            return Float.compare(Math.abs(yb), Math.abs(ya));
        });

        for (int layerIdx : order) {
            LayerState state = layers.get(layerIdx);
            var config = CloudsConfiguration.getInstance().getLayer(layerIdx);
            MeshType meshType = MeshTypeRegistry.getInstance().tryGetObject(config.MODE);
            
            if (meshType == null || !config.LAYER_RENDERED) continue;

            state.offsetX = config.APPEARANCE.LAYER_OFFSET_X;
            state.offsetZ = config.APPEARANCE.LAYER_OFFSET_Z;

            double timeOffsetX = time * config.APPEARANCE.LAYER_SPEED_X;
            double timeOffsetZ = time * config.APPEARANCE.LAYER_SPEED_Z;

            double dxLayer = baseDx + state.offsetX + timeOffsetX;
            double dzLayer = baseDz + state.offsetZ + timeOffsetZ;

            float cloudChunkHeight = MeshBuilder.HEIGHT_IN_BLOCKS;
            if (config.IS_ENABLED) {
                cloudChunkHeight *= config.APPEARANCE.CLOUD_Y_SCALE;
            }

            float layerY = (float)(config.LAYER_HEIGHT - cam.y);
            float relYTop = layerY + cloudChunkHeight;
            float relY = relYTop - cloudChunkHeight / 2.0f;

            int cellX = (int) Math.floor(dxLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            int cellZ = (int) Math.floor(dzLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);

            int dxCell = cellX - state.baseCellX;
            int dzCell = cellZ - state.baseCellZ;

            int range = CloudsConfiguration.getInstance().CLOUD_GRID_SIZE;
            int slack = range * 3 / 4; // rebuild at 3/4 the grid size

            boolean outOfRange = Math.abs(dxCell) > slack || Math.abs(dzCell) > slack;

            if (!state.cellInitialized || outOfRange) {
                state.baseCellX = cellX;
                state.baseCellZ = cellZ;
                state.needsRebuild = true;
                state.cellInitialized = true;
            }
                        
            float offX = (float)(dxLayer - state.baseCellX * MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offY = (float) (config.LAYER_HEIGHT - cam.y);
            float offZ = (float)(dzLayer - state.baseCellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            boolean needs = 
                state.needsRebuild||
                status != state.prevStatus;

            if (needs) {
                state.needsRebuild  = false;
                state.prevStatus    = status;
                updateGpuMesh(state, relY, layerIdx, skyColor);
            }

            if (state.buffer == null) continue;

            RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
            GpuTexture colorTex = target.getColorTexture();
            GpuTexture depthTex = target.getDepthTexture();

            if (!meshType.doDepthWrite()) {
                drawLayer(ModRenderPipelines.COLOR_ONLY_CLOUDS, offX, offY, offZ, colorTex, depthTex, layerIdx);
                continue;
            }

            drawLayer(ModRenderPipelines.DEPTH_ONLY_CLOUDS, offX, offY, offZ, colorTex, depthTex, layerIdx);
            drawLayer(ModRenderPipelines.NORMAL_CLOUDS, offX, offY, offZ, colorTex, depthTex, layerIdx);
        }
    }

    private void drawLayer(RenderPipeline pipeline,
            float offX, float offY, float offZ,
            GpuTexture colorTex, GpuTexture depthTex,
            int layer) {

        LayerState state = layers.get(layer);

        GpuBuffer indexBuf = indices.getBuffer(state.layerIndexCount);
        var config = CloudsConfiguration.getInstance().getLayer(layer);
        var lp = CloudsConfiguration.getInstance().LIGHTING;
        var client = Minecraft.getInstance();
        var level = client.level;
        
        float height = MeshBuilder.HEIGHT_IN_BLOCKS * (config.IS_ENABLED ? config.APPEARANCE.CLOUD_Y_SCALE : 1.0f);
        float relY = (offY + height) - (height / 2.0f);
        int skyColor = CloudColorProvider.getCloudColor(WeatherState.from(level, level.getGameTime()));
        Vec3 cam = client.gameRenderer.getMainCamera().getPosition();

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(colorTex, OptionalInt.empty(), depthTex, OptionalDouble.empty())) {
            
            pass.setPipeline(pipeline);
            pass.setIndexBuffer(indexBuf, indices.type());
            pass.setVertexBuffer(0, state.buffer);
            
            pass.setUniform("u_ProjMat", RenderSystem.getProjectionMatrix());
            pass.setUniform("u_ModelViewMat", RenderSystem.getModelViewMatrix());
            pass.setUniform("u_ModelOffset", -offX, offY, -offZ, 1.0f);

            pass.setUniform("u_CloudsInfo0",
                // ivec4 config
                (float)(packConfig(layer)),
                (int)config.FOG.FOG_START_DISTANCE,
                (int)config.FOG.FOG_END_DISTANCE,
                config.APPEARANCE.BASE_ALPHA
            );

            pass.setUniform("u_CloudsInfo1",
                // vec4 fade
                config.FADE.FADE_ALPHA,
                config.FADE.TRANSITION_RANGE,
                height,
                relY
            );
            
            pass.setUniform("u_CloudColor",
                // vec4 sky
                ARGB.redFloat(skyColor),
                ARGB.greenFloat(skyColor),
                ARGB.blueFloat(skyColor),
                1.0f
            );

            pass.setUniform("u_FadeToColor",
                // vec4 fade to
                ARGB.redFloat(config.FADE.FADE_TO_COLOR),
                ARGB.greenFloat(config.FADE.FADE_TO_COLOR),
                ARGB.blueFloat(config.FADE.FADE_TO_COLOR),
                1.0f
            );

            pass.setUniform("u_CameraPos",
                (float)cam.x,
                (float)cam.y,
                (float)cam.z,
                1.0f
            );

            int maxL = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
            int activeL = Math.min(lp.lights.size(), maxL);

            for (int i = 0; i < maxL; i++) {
                if (i < activeL) {
                    var l = lp.lights.get(i);

                    float lx = l.getXDirection();
                    float ly = l.getYDirection();
                    float lz = l.getZDirection();

                    if (lp.LIGHTING_TYPE == LightingType.DYNAMIC) {
                        float[] tmp = {lx, ly, lz};
                        l.evaluate(level.getGameTime(), tmp);
                        lx = tmp[0];
                        ly = tmp[1];
                        lz = tmp[2];
                    }

                    pass.setUniform("u_LightPos[" + i + "]", lx, ly, lz, l.intensity());

                    continue;
                }

                pass.setUniform("u_LightPos[" + i + "]", 0.0f, 0.0f, 0.0f, 0.0f);
            }

            for (int i = 0; i < maxL; i++) {
                if (i < activeL) {
                    Vector3f c = lp.lights.get(i).color();
                    pass.setUniform("u_LightColor[" + i + "]", c.x(), c.y(), c.z(), 1.0f);

                    continue;
                }
                pass.setUniform("u_LightColor[" + i + "]",  1.0f, 1.0f, 1.0f, 1.0f);
            }

            pass.setUniform("u_LightMeta",
                    activeL,
                    lp.MAX_LIGHTING_SHADING,
                    lp.AMBIENT_LIGHTING_STRENGTH,
                    lp.SHADING_MODE.ordinal()
            );

            pass.setUniform("u_CloudHeight",
                config.LAYER_HEIGHT,
                config.LAYER_HEIGHT + height
            );

            pass.drawIndexed(0, state.layerIndexCount);
        }
    }

    private void updateGpuMesh(LayerState state, float relY, int layer, int skyColor) {
        if (state.buffer != null) state.buffer.close();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        var meshBuilder = MeshBuilderRegistry.getInstance()
                .tryGetObject(CloudsConfiguration.getInstance().getLayer(layer).MODE);

        if (meshBuilder != null) {
            meshBuilder.build(builder, state, state.baseCellX, state.baseCellZ, relY, layer, skyColor);

            MeshData data = builder.buildOrThrow();
            if (data != null) {
                state.buffer = RenderSystem.getDevice().createBuffer(
                        () -> "Cloud layer " + layer,
                        BufferType.VERTICES,
                        BufferUsage.DYNAMIC_WRITE,
                        data.vertexBuffer()
                );
                state.layerIndexCount = data.drawState().indexCount();
            }
        }

        state.needsRebuild = false;
    }

    private int packConfig(int layer) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);
        
        int config = 0;

        if (layerConfiguration.FOG_ENABLED)                       config |= 1 << 0;
        if (layerConfiguration.APPEARANCE.SHADING_ENABLED)        config |= 1 << 1;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA)      config |= 1 << 2;
        if (layerConfiguration.APPEARANCE.CUSTOM_BRIGHTNESS)      config |= 1 << 3;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR)      config |= 1 << 4;
        if (layerConfiguration.FADE.FADE_ENABLED)                 config |= 1 << 5;
        if (layerConfiguration.FADE.COLOR_FADE)                   config |= 1 << 6;
        if (layerConfiguration.FADE.INVERTED_FADE)                config |= 1 << 7;
        if (layerConfiguration.FADE.FADE_TYPE == FadeType.STATIC) config |= 1 << 8;

        return config;
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
        public final int index;

        public int baseCellX;
        public int baseCellZ;
        public boolean cellInitialized = false;

        public float offsetX;
        public float offsetZ;

        public Texture.TextureData texture;

        public GpuBuffer buffer;
        public int layerIndexCount;
        public boolean bufferEmpty;

        public boolean needsRebuild = true;
        public CloudStatus prevStatus;

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }
    }
}