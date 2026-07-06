package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.ShadingMode;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.config.resources.CloudResourceLoader;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.shader.Std140SizeCalculator;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.lib.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider.WeatherState;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.DimensionProvider;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DMesh;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomCloudRenderer implements AutoCloseable {
    private final List<LayerState> layers = new ArrayList<>();
    
    protected static final IdentifierWrapper DEFAULT_TEXTURE_LOCATION = 
        IdentifierWrapper.of("minecraft", "textures/environment/clouds.png");

    private Dimension previousDimension = null;
    private boolean textureInitialized = false;
    private boolean shouldReset = false;

    private AbstractPipeline[] noDepthPipelines;
    private AbstractPipeline[] depthPipelines;

    public CustomCloudRenderer() {
        super();

        rebuildLayerStates();
        applyTexture();
    }

    private void rebuildLayerStates() {
        for (LayerState layer : layers) {
            layer.close();
        }
        layers.clear();
        textureInitialized = false;
        shouldReset = false;

        PipelineManager manager = PipelineManager.getInstance();

        int layerCount = CloudsConfiguration.getInstance().getLayerCount();
        for (int i = 0; i < layerCount; i++) {
            LayerState layerState = new LayerState();
            layerState.texture = null;
            layerState.needsRebuild = true;
            layerState.cellInitialized = false;
            layerState.prevStatus = null;
            layers.add(layerState);
            layerState.transforms = manager.createDataBuffer("CloudsTransforms", TRANSFORMS_SIZE);
            layerState.cloudsInfo = manager.createDataBuffer("CloudsCloudInfo", CLOUDS_INFO_SIZE);
            layerState.lighting = manager.createDataBuffer("CloudsLighting", LIGHTING_SIZE);
            layerState.camera = manager.createDataBuffer("CloudsCamera", CAMERA_SIZE);

            noDepthPipelines = new AbstractPipeline[] {
                manager.getPipeline("POSITION_COLOR_NO_DEPTH")
            };

            depthPipelines = new AbstractPipeline[] {
                manager.getPipeline("POSITION_COLOR_DEPTH_ONLY"),
                manager.getPipeline("CUSTOM_POSITION_COLOR")
            };
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
            return Texture.buildTexture(inputStream);
        }
        catch (IOException exception) {
            LoggerProvider.get().info("Failed to build cloud texture, discarding, error: {}", exception);
            return Optional.empty();
        }
    }

    public boolean isTextureInitialized() {
        return this.textureInitialized;
    }

    @SuppressWarnings("unused")
    public void apply(Optional<Texture.TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Texture.TextureData baseTexture = optional.orElse(null);
        int layerCount = CloudsConfiguration.getInstance().getLayerCount();
        for (int i = 0; i < layerCount && i < layers.size(); i++) {
            LayerState layer = layers.get(i);
            CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.getInstance().getLayer(i);
            layer.texture = resolveTextureForLayer(i, layerConfig, baseTexture);
            layer.needsRebuild = true;
        }
    }

    @Nullable
    private Texture.TextureData resolveTextureForLayer(int layerIndex, CloudsConfiguration.LayerConfiguration layerConfig, @Nullable Texture.TextureData fallback) {
        String textureName = layerConfig.APPEARANCE.TEXTURE_NAME;
        String textureNamespace = layerConfig.APPEARANCE.TEXTURE_NAMESPACE;
        boolean flipX = layerConfig.APPEARANCE.TEXTURE_HORIZONTAL_FLIP;
        boolean flipY = layerConfig.APPEARANCE.TEXTURE_VERTICAL_FLIP;
        
        if (textureName != null && !textureName.isEmpty() && 
            textureNamespace != null && !textureNamespace.isEmpty()) {
            
            Optional<Texture.TextureData> customTexture = CloudResourceLoader.loadLayerTexture(textureNamespace, textureName, flipX, flipY);
            if (customTexture.isPresent()) {
                return customTexture.get();
            }
        }
        
        // Fall back to the default/base texture if custom texture not found or configured
        return fallback;
    }

    public void applyTexture() {
        if (ClientHelper.getLevel() == null) return;

        if (!textureInitialized) {
            for (int i = 0; i < layers.size(); i++) {
                LayerState layer = layers.get(i);
                CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.getInstance().getLayer(i);
                layer.texture = resolveTextureForLayer(i, layerConfig, null);
            }
            textureInitialized = true;
        }
    }

    public Optional<Texture.TextureData> getCurrentTexture() {
        return Optional.empty();
    }
    
    long last = 0;

    public void render(CloudStatus status, Vec3 cam, float tickDelta) {
        double baseDx = cam.x;
        double baseDz = cam.z + 3.96F;

        int activeLayers = CloudsConfiguration.getInstance().getLayerCount();

        if (activeLayers < 0) return;

        Dimension currentDimension = DimensionProvider.getCurrentDimension();

        if (!CloudsConfiguration.getInstance().getCloudRendered(currentDimension)) return;

        if (activeLayers != layers.size() || previousDimension != currentDimension || shouldReset) {
            rebuildLayerStates();
            applyTexture();
        }

        previousDimension = currentDimension;

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < activeLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float)(CloudsConfiguration.getInstance().getLayer(a).LAYER_HEIGHT - cam.y);
            float yb = (float)(CloudsConfiguration.getInstance().getLayer(b).LAYER_HEIGHT - cam.y);
            return Float.compare(Math.abs(yb), Math.abs(ya));
        });

        ClientLevel level = ClientHelper.getLevel();
        WeatherState weather = WeatherState.from(level, tickDelta);

        // sky color in other dimensions are returned as Black from Vanilla
        final int skyColor = 
            DimensionProvider.isOverworld() ?
            CloudColorProvider.getCloudColor(weather) :
            ARGB.WHITE;

        final long gameTime = level.getGameTime();
        final long dayTime = level.getDayTime();
        final float time = (gameTime + tickDelta) / 20.0F;
        startRender();

        final int range = CloudsConfiguration.getInstance().getCloudGridRange();
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
            float offY = (float) (layerConfiguration.LAYER_HEIGHT + layerConfiguration.APPEARANCE.LAYER_HEIGHT_OFFSET - cam.y);
            float offZ = (float)(dzLayer - currentLayer.baseCellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            boolean needs = 
                currentLayer.needsRebuild
                || status != currentLayer.prevStatus;

            if (needs) {
                currentLayer.needsRebuild  = false;
                currentLayer.prevStatus    = status;
                tryBuildClouds(currentLayer, relY, layer, ARGB.toRGBA(ARGB.WHITE));
            }

            int shaderColor = ColorUtils.getCloudShaderColor(layer, skyColor);
            try {
                if (!type.doDepthWrite()) {
                    drawLayer(noDepthPipelines,
                        offX, offY, offZ,
                        layer, shaderColor, relY, dayTime, cam);
                    continue;
                }

                drawLayer(
                    depthPipelines,
                    offX, offY, offZ,
                    layer, shaderColor, relY, dayTime, cam);
            }
            catch (Exception e) {
                LoggerProvider.get().error("Error while rendering: {}", e);
            }
        }
        finishRender();
    }

    private void tryBuildClouds(LayerState currentLayer, float relY, int layer, int colorModifier) {
        if (currentLayer.buffer != null) {
            currentLayer.buffer.close();
        }

        var mesh = buildMeshForLayer(
            currentLayer.texture,
            currentLayer.baseCellX, currentLayer.baseCellZ,
            currentLayer.prevStatus,
            relY, layer, colorModifier);

        if (mesh == null) throw new IllegalStateException("Mesh was null");
        currentLayer.buffer = mesh;
        currentLayer.layerIndexCount = mesh.getIndexCount();
        LoggerProvider.get().info("Built cloud mesh for layer {} with {} vertices, {} indices", layer, mesh.getVertexCount(), mesh.getIndexCount());
    }

    @Nullable
    private AbstractStaticMesh<?> buildMeshForLayer(Texture.TextureData tex,
                                       int baseCx, int baseCz,
                                       CloudStatus status,
                                       float relYCenter, int currentLayer,
                                       int colorModifier) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);
        
        LayerState state = layers.get(currentLayer);
        MeshTypeBuilder builder = null;
        try {
            builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);
        }
        catch (Exception exception) {
            LoggerProvider.get().error("Exception building mesh: {}", exception);
            return null;
        }

        AbstractStaticMesh.Builder<?, ?> mesh = new B3DMesh.Builder(VertexFormat.POSITION_COLOR_NORMAL, GLPrimitive.QUADS);

        builder.build(
            mesh, 
            state, 
            baseCx, baseCz, 
            relYCenter, 
            currentLayer, 
            colorModifier);

        return mesh.build();
    }

    private void startRender() {
        // no-op
    }

    private void finishRender() {
        // no-op
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
        shouldReset = true;
        if (layer >= 0 && layer < layers.size()) {
            layers.get(layer).needsRebuild = true;
        }
    }

    public void markForRebuild() {
        shouldReset = true;
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).needsRebuild = true;
        }
    }

    @Override
    public void close() {
        for (LayerState layer : layers) {
            if (layer != null) {
                layer.close();
            }
        }
    }
    
    public static class LayerState implements Closeable {
        int index;
        float offsetX;
        float offsetZ;

        int baseCellX;
        int baseCellZ;

        Texture.TextureData texture;
        AbstractStaticMesh<?> buffer;

        boolean needsRebuild;

        boolean cellInitialized = false;
        CloudStatus prevStatus;

        boolean bufferEmpty;
        int layerIndexCount;

        AbstractUBODataBuffer<?, ?>  transforms;
        AbstractUBODataBuffer<?, ?>  cloudsInfo;
        AbstractUBODataBuffer<?, ?>  lighting;
        AbstractUBODataBuffer<?, ?>  camera;

        public Texture.TextureData texture() { return this.texture; }

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }

        void closeBuffers() {
            transforms.close();
            cloudsInfo.close();
            lighting.close();
            camera.close();
        }

        @Override
        public void close() {
            if (buffer != null) {
                buffer.close();
            }
            closeBuffers();
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
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16;

    private void drawLayer(
        AbstractPipeline[] pipelines,
        float ox, float oy, float oz,
        int layer,
        int skyColor,
        float relY,
        long timeTicks,
        Vec3 camPos) {

        LayerState currentLayer = layers.get(layer);

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.getInstance().getLayer(layer);

        PipelineManager manager = PipelineManager.getInstance();

        CloudsConfiguration.LightingParameters lightingParameters = 
            CloudsConfiguration.getInstance().LIGHTING;
        List<DiffuseLight> lights = lightingParameters.lights;
        int maxLightCount = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
        float lightAmbientFactor = lightingParameters.AMBIENT_LIGHTING_STRENGTH;
        float lightShadingStrength = lightingParameters.MAX_LIGHTING_SHADING;

        currentLayer.transforms.reset();
        currentLayer.cloudsInfo.reset();
        currentLayer.lighting.reset();
        currentLayer.camera.reset();

        Matrix4f proj = RenderSystem.getModelViewMatrix();
        Matrix4f mv = RenderSystem.getModelViewMatrix();

        currentLayer.transforms.putVec4(-ox, oy, -oz, 1.0f);
        currentLayer.transforms.putMat4(proj);
        currentLayer.transforms.putMat4(mv);

        float heightInBlocks = MeshBuilder.HEIGHT_IN_BLOCKS *
                (layerConfiguration.IS_ENABLED
                    ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                    : 1.0f);

        // Info0
        currentLayer.cloudsInfo.putIVec4(
            packConfig(layer),
            (int) layerConfiguration.FOG.FOG_START_DISTANCE,
            (int) layerConfiguration.FOG.FOG_END_DISTANCE,
            layerConfiguration.APPEARANCE.BASE_ALPHA
        );

        // Info1
        currentLayer.cloudsInfo.putVec4(
            layerConfiguration.FADE.FADE_ALPHA,
            layerConfiguration.FADE.TRANSITION_RANGE,
            heightInBlocks,
            relY
        );

        currentLayer.cloudsInfo.putVec4(
            ARGB.redFloat(skyColor),
            ARGB.greenFloat(skyColor),
            ARGB.blueFloat(skyColor),
            1.0f
        );

        // vec4 fadeToColor
        currentLayer.cloudsInfo.putVec4(
            ARGB.redFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            ARGB.greenFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            ARGB.blueFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            1.0f
        );
                
        // vec4 fadeInfo
        currentLayer.cloudsInfo.putVec4(
            layerConfiguration.FADE.STATIC_FADE_REL_Y,
            0.0f, // unused
            0.0f, // unused
            0.0f  // unused
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

                currentLayer.lighting.putVec4(
                    lightPos[0],
                    lightPos[1],
                    lightPos[2],
                    l.intensity()
                );
            } else {
                currentLayer.lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightColors
        for (int i = 0; i < maxLightCount; i++) {
            if (i < lightCount) {
                Vector3f c = lights.get(i).color();
                currentLayer.lighting.putVec4(c.x(), c.y(), c.z(), 1.0f);
            } else {
                currentLayer.lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightInformation
        currentLayer.lighting.putVec4(
            lightCount,
            lightShadingStrength,
            lightAmbientFactor,
            lightingParameters.SHADING_MODE == ShadingMode.GOURAUD ? 0 : 1
        );

        currentLayer.camera.putVec4((float) camPos.x, (float) camPos.y, (float) camPos.z, 1.0f);

        try (AbstractRenderPass<?> pass = manager.createRenderPass("Clouds", pipelines)) {
            pass.setMesh(currentLayer.buffer, currentLayer.layerIndexCount);

            pass.bindUniformBlock("Transforms", currentLayer.transforms);
            pass.bindUniformBlock("CloudInfo", currentLayer.cloudsInfo);
            pass.bindUniformBlock("Lighting", currentLayer.lighting);
            pass.bindUniformBlock("Camera", currentLayer.camera);

            pass.setup();
            pass.render();
            pass.cleanup();
        }
    }
}
