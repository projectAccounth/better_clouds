package net.not_thefirst.story_mode_clouds.renderer;

import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;
import net.not_thefirst.lib.gl_render_system.mesh.CompiledMesh;
import net.not_thefirst.lib.gl_render_system.mesh.GpuMesh;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedBuildingMesh;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedCompiledMesh;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedGpuMesh;
import net.not_thefirst.lib.gl_render_system.mesh.MeshUploader;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedBuildingMesh.IndexPattern;
import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.Std140BufferBuilder;
import net.not_thefirst.lib.gl_render_system.shader.Std140SizeCalculator;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.lib.gl_render_system.shader.UniformBufferObject;
import net.not_thefirst.lib.gl_render_system.state.ShaderRenderType;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.TestBuilder;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider.WeatherState;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomCloudRenderer implements AutoCloseable {

    private Optional<Texture.TextureData> currentTexture = Optional.empty();
    private final List<LayerState> layers = new ArrayList<>();
    
    protected static final IdentifierWrapper TEXTURE_LOCATION = 
        IdentifierWrapper.of("minecraft", "textures/environment/clouds.png");

    public CustomCloudRenderer() {
        super();

        rebuildLayerStates();
    }

    private void rebuildLayerStates() {
        layers.clear();

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
    
    long last = 0;

    public void render(CloudStatus status, Vec3 cam, float tickDelta) {
        double baseDx = cam.x;
        double baseDz = cam.z + 3.96F;

        int activeLayers = CloudsConfiguration.getInstance().getLayerCount();

        if (activeLayers < 0) return;

        if (activeLayers != layers.size()) {
            rebuildLayerStates();
            applyTexture();
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < activeLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float)(CloudsConfiguration.getInstance().getLayer(a).LAYER_HEIGHT - cam.y);
            float yb = (float)(CloudsConfiguration.getInstance().getLayer(b).LAYER_HEIGHT - cam.y);
            return Float.compare(Math.abs(yb), Math.abs(ya));
        });

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        WeatherState weather = WeatherState.from(level, tickDelta);

        final int skyColor = CloudColorProvider.getCloudColor(weather);
        final long gameTime = level.getGameTime();
        final long dayTime = level.getDayTime();
        final float time = (gameTime + tickDelta) / 20.0F;

        startRender();

        final int range = CloudsConfiguration.getInstance().CLOUD_GRID_SIZE;
        final int slack = range * 3 / 4;

        if (System.currentTimeMillis() - last > 1000) {
            last = System.currentTimeMillis();
        }

        for (int layer : order) {
            LayerState currentLayer = this.layers.get(layer);

            CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);

            MeshType type = MeshTypeRegistry.getInstance().tryGetObject(layerConfiguration.MODE);

            if (!layerConfiguration.LAYER_RENDERED || currentLayer.texture == null || type == null) continue;
            
            currentLayer.offsetX = layerConfiguration.APPEARANCE.LAYER_OFFSET_X;
            currentLayer.offsetZ = layerConfiguration.APPEARANCE.LAYER_OFFSET_Z;

            double timeOffsetX = time * layerConfiguration.APPEARANCE.LAYER_SPEED_X;
            double timeOffsetZ = time * layerConfiguration.APPEARANCE.LAYER_SPEED_Z;

            double dxLayer = baseDx + currentLayer.offsetX + timeOffsetX;
            double dzLayer = baseDz + currentLayer.offsetZ + timeOffsetZ;

            float cloudChunkHeight = MeshBuilder.HEIGHT_IN_BLOCKS;
            if (layerConfiguration.IS_ENABLED) {
                cloudChunkHeight *= layerConfiguration.APPEARANCE.CLOUD_Y_SCALE;
            }

            float layerY = (float)(layerConfiguration.LAYER_HEIGHT - cam.y);
            float relYTop = layerY + cloudChunkHeight;
            float relY = relYTop - cloudChunkHeight / 2.0f;

            int cellX = (int) Math.floor(dxLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            int cellZ = (int) Math.floor(dzLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);

            int dxCell = cellX - currentLayer.baseCellX;
            int dzCell = cellZ - currentLayer.baseCellZ;

            if (!currentLayer.cellInitialized ||
                (Math.abs(dxCell) > slack || Math.abs(dzCell) > slack)
            ) {
                currentLayer.baseCellX = cellX;
                currentLayer.baseCellZ = cellZ;
                currentLayer.needsRebuild = true;
                currentLayer.cellInitialized = true;
            }
                        
            float offX = (float)(dxLayer - currentLayer.baseCellX * MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offY = (float) (layerConfiguration.LAYER_HEIGHT - cam.y);
            float offZ = (float)(dzLayer - currentLayer.baseCellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            boolean needs = 
                currentLayer.needsRebuild
                || status != currentLayer.prevStatus;

            if (needs) {
                currentLayer.needsRebuild  = false;
                currentLayer.prevStatus    = status;
                tryBuildClouds(currentLayer, relY, layer, skyColor);
            }

            int shaderColor = ColorUtils.getCloudShaderColor(layer, skyColor);
            try {
                if (!type.doDepthWrite()) {
                    drawLayer(ModRenderPipelines.POSITION_COLOR_NO_DEPTH,
                        offX, offY, offZ, layer,
                        shaderColor, relY, dayTime, cam);
                    continue;
                }

                drawLayer(
                    ModRenderPipelines.POSITION_COLOR_DEPTH_ONLY,
                        offX, offY, offZ, layer,
                        shaderColor, relY, dayTime, cam);
                drawLayer(
                    ModRenderPipelines.CUSTOM_POSITION_COLOR,
                        offX, offY, offZ, layer,
                        shaderColor, relY, dayTime, cam);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        finishRender();
    }

    private void tryBuildClouds(LayerState currentLayer, float relY, int layer, int skyColor) {
        if (currentLayer.buffer != null) {
            currentLayer.buffer.close();
        }

        CompiledMesh mesh = buildMeshForLayer(
            currentLayer.texture,
            currentLayer.baseCellX, currentLayer.baseCellZ,
            currentLayer.prevStatus,
            relY, layer, skyColor);
        
        if (mesh != null) {
            currentLayer.buffer = MeshUploader.uploadOrdered(mesh);
            currentLayer.bufferEmpty = false;
        } else {
            currentLayer.bufferEmpty = true;
        }
    }

    @Nullable
    private CompiledMesh buildMeshForLayer(Texture.TextureData tex,
                                       int baseCx, int baseCz,
                                       CloudStatus status,
                                       float relYCenter, int currentLayer,
                                       int skyColor) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);

        BuildingMesh mesh = 
            new BuildingMesh(
                VertexFormat.POSITION_COLOR_NORMAL, 
                GL11.GL_TRIANGLES);

        MeshTypeBuilder builder = null;
        try {
            builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }

        builder.build(
            mesh, 
            state, 
            baseCx, baseCz, 
            relYCenter, 
            currentLayer, 
            skyColor);

        LoggerProvider.get().info("Built mesh for layer {} with {} vertices ",
            currentLayer, mesh.vertexCount());

        return mesh.compile();
    }

    private void startRender() {
        ModRenderPipelines.DEFAULT_RESET_STATE.clear();
    }

    private void finishRender() {
        ModRenderPipelines.DEFAULT_RESET_STATE.apply();
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
            if (layer.buffer != null) {
                layer.buffer.close();
            }
        }
    }
    
    public static class LayerState {
        int index;
        float offsetX;
        float offsetZ;

        int baseCellX;
        int baseCellZ;

        public Texture.TextureData texture;
        GpuMesh buffer;

        boolean needsRebuild;

        boolean cellInitialized = false;
        CloudStatus prevStatus;

        boolean bufferEmpty;
        int layerIndexCount;

        UniformBufferObject transformsBuffer = new UniformBufferObject(0, TRANSFORMS_SIZE);
        UniformBufferObject cloudsInfoBuffer = new UniformBufferObject(1, CLOUDS_INFO_SIZE);
        UniformBufferObject lightingBuffer   = new UniformBufferObject(2, LIGHTING_SIZE);
        UniformBufferObject cameraBuffer     = new UniformBufferObject(3, CAMERA_SIZE);

        public Texture.TextureData texture() { return this.texture; }

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }
    }

    private static final int CAMERA_SIZE =
        new Std140SizeCalculator()
            .addVec4()
            .finish().offset();

    private static final int TRANSFORMS_SIZE =
        new Std140SizeCalculator()
            .addVec4()
            .addMat4()
            .addMat4()
            .finish().offset();

    private static final int CLOUDS_INFO_SIZE =
        new Std140SizeCalculator()
            .addIVec4()
            .addVec4()
            .addVec4()
            .addVec4()
            .addVec4()
            .finish().offset();

    private static final int LIGHTING_SIZE = 
        Std140SizeCalculator.getVec4Size() * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        Std140SizeCalculator.getVec4Size() * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        Std140SizeCalculator.getVec4Size();

    private void drawLayer(
        ShaderRenderType rt,
        float ox, float oy, float oz,
        int layer,
        int skyColor,
        float relY,
        long timeTicks,
        Vec3 camPos) {
        rt.setup();
        int vanilla = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        LayerState currentLayer = layers.get(layer);
        GpuMesh buf = currentLayer.buffer;

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.getInstance().getLayer(layer);

        GLProgram shader = rt.program();

        Std140BufferBuilder transforms = new Std140BufferBuilder(TRANSFORMS_SIZE);
        Std140BufferBuilder cloudsInfo = new Std140BufferBuilder(CLOUDS_INFO_SIZE);
        Std140BufferBuilder lighting   = new Std140BufferBuilder(LIGHTING_SIZE);
        Std140BufferBuilder camera     = new Std140BufferBuilder(CAMERA_SIZE);

        CloudsConfiguration.LightingParameters lightingParameters = 
            CloudsConfiguration.getInstance().LIGHTING;
        List<DiffuseLight> lights = lightingParameters.lights;
        int maxLightCount = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
        float lightAmbientFactor = lightingParameters.AMBIENT_LIGHTING_STRENGTH;
        float lightShadingStrength = lightingParameters.MAX_LIGHTING_SHADING;

        Matrix4f proj = RenderSystem.getProjectionMatrix();
        Matrix4f mv = RenderSystem.getModelViewMatrix();

        transforms.putMat4(proj);
        transforms.putMat4(mv);
        transforms.putVec4(-ox, oy, -oz, 1.0f);

        float heightInBlocks = MeshBuilder.HEIGHT_IN_BLOCKS *
                (layerConfiguration.IS_ENABLED
                    ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                    : 1.0f);

        // Info0
        cloudsInfo.putIVec4(
            packConfig(layer),
            (int) layerConfiguration.FOG.FOG_START_DISTANCE,
            (int) layerConfiguration.FOG.FOG_END_DISTANCE,
            layerConfiguration.APPEARANCE.BASE_ALPHA
        );

        // Info1
        cloudsInfo.putVec4(
            layerConfiguration.FADE.FADE_ALPHA,
            layerConfiguration.FADE.TRANSITION_RANGE,
            heightInBlocks,
            relY
        );

        cloudsInfo.putVec4(
            ARGB.redFloat(skyColor),
            ARGB.greenFloat(skyColor),
            ARGB.blueFloat(skyColor),
            1.0f
        );

        int lightCount = Math.min(lights.size(), maxLightCount);
        float[] lightPos = new float[3];

        for (int i = 0; i < maxLightCount; i++) {
            if (i < lightCount) {
                DiffuseLight l = lights.get(i);
                lightPos[0] = l.getXDirection();
                lightPos[1] = l.getYDirection();
                lightPos[2] = l.getZDirection();

                if (CloudsConfiguration
                        .getInstance().LIGHTING.LIGHTING_TYPE == LightingType.DYNAMIC)
                    l.evaluate(timeTicks, lightPos);

                lighting.putVec4(
                    lightPos[0],
                    lightPos[1],
                    lightPos[2],
                    l.intensity()
                );
            } else {
                lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightColors
        for (int i = 0; i < maxLightCount; i++) {
            if (i < lightCount) {
                Vector3f c = lights.get(i).color();
                lighting.putVec4(c.x(), c.y(), c.z(), 1.0f);
            } else {
                lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightInformation
        lighting.putVec4(
            lightCount,
            lightShadingStrength,
            lightAmbientFactor,
            lightingParameters.SHADING_MODE.ordinal()
        );

        camera.putVec4((float) camPos.x, (float) camPos.y, (float) camPos.z, 1.0f);

        currentLayer.transformsBuffer.bind();
        currentLayer.cloudsInfoBuffer.bind();
        currentLayer.lightingBuffer.bind();
        currentLayer.cameraBuffer.bind();

        currentLayer.transformsBuffer.update(transforms.build());
        currentLayer.cloudsInfoBuffer.update(cloudsInfo.build());
        currentLayer.lightingBuffer.update(lighting.build());
        currentLayer.cameraBuffer.update(camera.build());

        shader.setFloat("CloudBottomY", layerConfiguration.LAYER_HEIGHT);
        shader.setFloat("CloudTopY", layerConfiguration.LAYER_HEIGHT + heightInBlocks);

        buf.draw();
        GL30.glBindVertexArray(vanilla);
        
        rt.clear();
    }
}
