package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.ShadingMode;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.resources.CloudResourceLoader;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshType;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.utils.glfw.GLFWWindowHelper;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderTarget;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.RenderTargetDescriptor;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.shader.Std140SizeCalculator;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.lib.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider.WeatherState;
import net.not_thefirst.story_mode_clouds.utils.memory.AssetHandle;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.DimensionProvider;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DMesh;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class CustomCloudRenderer implements AutoCloseable {

    // ------------------------------------------
    // defs and constants

    private final List<LayerState> layers = new ArrayList<>();
    private AbstractPipeline[] noDepthPipelines;
    private AbstractPipeline[] depthPipelines;
    private AbstractPipeline[] outlinePipelines;

    private AbstractRenderTarget cloudsTarget;
    private AbstractRenderTarget cloudsOutlineTarget;
    private AbstractRenderTarget mainTarget;
    
    private Dimension previousDimension = null;
    private boolean textureInitialized = false;
    private boolean shouldReset = false;

    private static final Vector4f CLEAR_COLOR = new Vector4f(0);
    private static final float    CLEAR_DEPTH = 0.0f;

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
            .addVec4()
            .finish().offset();

    private static final int LIGHTING_SIZE = 
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16;

    private static final int OUTLINE_SIZE =
        new Std140SizeCalculator()
            .addVec4()
            .addVec4()
            .finish().offset();

    // ------------------------------------------
    // constructor

    public CustomCloudRenderer() {
        super();

        LoggerProvider.get().info("Initializing CustomCloudRenderer");

        initResources();
        rebuildLayerStates();
        applyTexture();
    }


    // ------------------------------------------
    // resources

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
            layerState.outline = manager.createDataBuffer("CloudsOutline", OUTLINE_SIZE);
        }

        for (int i = 0; i < layerCount; i++) {
            LayerState layer = layers.get(i);
            CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.getInstance().getLayer(i);
            layer.offsetX = layerConfig.APPEARANCE.LAYER_OFFSET_X;
            layer.offsetZ = layerConfig.APPEARANCE.LAYER_OFFSET_Z;
        }
    }

    public boolean isTextureInitialized() {
        return this.textureInitialized;
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

    private void initResources() {
        PipelineManager manager = PipelineManager.getInstance();
        RenderTargetDescriptor descriptor = new RenderTargetDescriptor(
            GLFWWindowHelper.getWidth(ClientHelper.getGLFWHandle()), 
            GLFWWindowHelper.getHeight(ClientHelper.getGLFWHandle()));

        if (cloudsOutlineTarget == null) {
            cloudsOutlineTarget = manager.getRenderTargetFactory().create("CloudsOutline", descriptor);
        }

        if (cloudsTarget == null) {
            cloudsTarget = manager.getRenderTargetFactory().create("Clouds", descriptor);
        }

        if (mainTarget == null) {
            mainTarget = manager.getRenderTargetFactory().createDefault("Main", descriptor);
        }

        GLFWWindowHelper.chainCallback(ClientHelper.getGLFWHandle(), GLFWFramebufferSizeCallback.create((windowId, width, height) -> {
            cloudsOutlineTarget.resize(width, height);
            cloudsTarget.resize(width, height);
        }));

        noDepthPipelines = new AbstractPipeline[] {
            manager.getPipeline("POSITION_COLOR_NO_DEPTH"),
        };

        depthPipelines = new AbstractPipeline[] {
            manager.getPipeline("POSITION_COLOR_DEPTH_ONLY"),
            manager.getPipeline("CUSTOM_POSITION_COLOR")
        };

        outlinePipelines = new AbstractPipeline[] {
            manager.getPipeline("POSITION_COLOR_DEPTH_ONLY"),
            manager.getPipeline("OUTLINE")
        };
    }

    // ----------
    // render func

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
        final long dayTime = level.getOverworldClockTime();
        final float time = (gameTime + tickDelta) / 20.0F;

        final int range = CloudsConfiguration.getInstance().getCloudGridRange();
        final int slack = range * 3 / 4;

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
                tryBuildClouds(currentLayer, layer, ARGB.toRGBA(ARGB.WHITE));
            }

            pollPendingMeshes(layer);

            try {
                if (!type.doDepthWrite()) {
                    drawLayer(noDepthPipelines,
                        offX, offY, offZ,
                        relY, layer, skyColor, dayTime, cam);
                    continue;
                }

                drawLayer(
                    depthPipelines,
                    offX, offY, offZ,
                    relY, layer, skyColor, dayTime, cam);
            }
            catch (Exception e) {
                LoggerProvider.get().error("Error while rendering frame: {}", e);
            }
        }
    }

    private void drawLayer(
        AbstractPipeline[] pipelines,
        float ox, float oy, float oz,
        float relY,
        int layer,
        int skyColor,
        
        long timeTicks,
        Vec3 camPos) {

        LayerState currentLayer = layers.get(layer);

        if (!meshesFinishedBuilding(currentLayer) || currentLayer.mainBuffer == null) return;

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.getInstance().getLayer(layer);

        PipelineManager manager = PipelineManager.getInstance();

        CloudsConfiguration.LightingParameters lightingParameters = 
            CloudsConfiguration.getInstance().LIGHTING;
        List<DiffuseLight> lights = lightingParameters.lights;
        int maxLightCount = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
        float lightAmbientFactor = lightingParameters.AMBIENT_LIGHTING_STRENGTH;
        float lightShadingStrength = 1.0f; // lightingParameters.MAX_LIGHTING_SHADING;

        currentLayer.transforms.reset();
        currentLayer.cloudsInfo.reset();
        currentLayer.lighting.reset();
        currentLayer.camera.reset();
        currentLayer.outline.reset();

        Matrix4f proj = RenderSystem.getModelViewMatrixCopy();
        Matrix4f mv = RenderSystem.getModelViewMatrixCopy();

        currentLayer.transforms.putVec4(-ox, oy, -oz, 1.0f);
        currentLayer.transforms.putMat4(proj);
        currentLayer.transforms.putMat4(mv);

        float heightInBlocks = MeshBuilder.HEIGHT_IN_BLOCKS *
                (layerConfiguration.IS_ENABLED
                    ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                    : 1.0f);

        int shaderColor = ColorUtils.getCloudShaderColor(layer, skyColor);

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

        // CloudColor
        currentLayer.cloudsInfo.putVec4(
            ARGB.redFloat(shaderColor),
            ARGB.greenFloat(shaderColor),
            ARGB.blueFloat(shaderColor),
            1.0f
        );

        // vec4 fadeToColor
        currentLayer.cloudsInfo.putVec4(
            ARGB.redFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            ARGB.greenFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            ARGB.blueFloat(layerConfiguration.FADE.FADE_TO_COLOR),
            1.0f
        );
                
        // vec4 info2
        currentLayer.cloudsInfo.putVec4(
            layerConfiguration.FADE.STATIC_FADE_REL_Y,
            0.0f, // unused
            0.0f, // unused
            0.0f  // unused
        );

        // SkyColor
        currentLayer.cloudsInfo.putVec4(
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
            lightingParameters.SHADING_MODE == ShadingMode.FLAT ? 0 : 1
        );

        // outlineColor
        currentLayer.outline.putVec4(
            ARGB.redFloat(layerConfiguration.APPEARANCE.OUTLINE_COLOR),
            ARGB.greenFloat(layerConfiguration.APPEARANCE.OUTLINE_COLOR),
            ARGB.blueFloat(layerConfiguration.APPEARANCE.OUTLINE_COLOR),
            1.0f
        );

        // outlineInfo0
        currentLayer.outline.putVec4(
            packOutlineConfig(layer),
            layerConfiguration.APPEARANCE.OUTLINE_BRIGHTNESS,
            layerConfiguration.APPEARANCE.OUTLINE_ALPHA,
            0.0f
        );

        currentLayer.camera.putVec4((float) camPos.x, (float) camPos.y, (float) camPos.z, 1.0f);

        try (AbstractRenderPass<?> pass = manager.createRenderPass("Clouds", pipelines)) {
            pass.bindUniformBlock("Transforms", currentLayer.transforms);
            pass.bindUniformBlock("CloudInfo", currentLayer.cloudsInfo);
            pass.bindUniformBlock("Lighting", currentLayer.lighting);
            pass.bindUniformBlock("Camera", currentLayer.camera);

            pass.setRenderTarget(cloudsTarget);
            pass.setClearColor(CLEAR_COLOR);
            pass.setClearDepth(CLEAR_DEPTH);
            pass.setMesh(currentLayer.mainBuffer, currentLayer.layerIndexCount);

            pass.setup();
            pass.render();
            pass.cleanup();
        }

        if (currentLayer.outlineBuffer == null || currentLayer.outlineLayerIndexCount <= 0 || !layerConfiguration.APPEARANCE.OUTLINE_ENABLED) {
            cloudsTarget.renderTo(manager.getPipeline("BLIT"), mainTarget);
            return;
        }

        try (AbstractRenderPass<?> pass = manager.createRenderPass("CloudsOutline", outlinePipelines)) {
            pass.bindUniformBlock("Transforms", currentLayer.transforms);
            pass.bindUniformBlock("CloudInfo", currentLayer.cloudsInfo);
            pass.bindUniformBlock("Camera", currentLayer.camera);
            pass.bindUniformBlock("Outline", currentLayer.outline);

            pass.setRenderTarget(cloudsOutlineTarget);
            pass.setClearColor(CLEAR_COLOR);
            pass.setClearDepth(CLEAR_DEPTH);
            pass.setMesh(currentLayer.outlineBuffer, currentLayer.outlineLayerIndexCount);

            pass.setup();
            pass.render();
            pass.cleanup();
        }

        cloudsTarget.renderTo(manager.getPipeline("STENCIL"), cloudsOutlineTarget);
        cloudsOutlineTarget.renderTo(manager.getPipeline("BLIT"), mainTarget);
    }

    // ------------------------------------------
    // clouds building

    private void tryBuildClouds(LayerState currentLayer, int layer, int colorModifier) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);

        currentLayer.pendingMain = buildMeshForLayer(
            MeshBuilder::buildMesh,
            () -> true,
            layer, 
            colorModifier);

        currentLayer.pendingOutline = buildMeshForLayer(
            MeshBuilder::buildOutlineMesh,
            () -> layerConfiguration.APPEARANCE.OUTLINE_ENABLED,
            layer, 
            colorModifier);
    }

    /**
     * Polls the currently building meshes for the layer.
     * @param layerIdx layer index
     */
    private void pollPendingMeshes(int layerIdx) {
        LayerState currentLayer = layers.get(layerIdx);

        if (currentLayer.pendingMain != null && currentLayer.pendingMain.isReady()) {
            var mesh = currentLayer.pendingMain.get();
            try {
                if (currentLayer.mainBuffer != null) {
                    currentLayer.mainBuffer.close();
                }

                currentLayer.mainBuffer = mesh.build();
                currentLayer.layerIndexCount = currentLayer.mainBuffer.getIndexCount();
                LoggerProvider.get().info("Built cloud mesh for layer {} with {} vertices, {} indices", 
                    layerIdx, currentLayer.mainBuffer.getVertexCount(), currentLayer.mainBuffer.getIndexCount());
            }
            catch (Exception e) {
                LoggerProvider.get().error("Mesh building finished with exception: {}", e);
            }

            currentLayer.pendingMain = null;
        }
        else if (currentLayer.pendingMain != null && currentLayer.pendingMain.hasFailed()) {
            LoggerProvider.get().warn("Mesh building failed.");
            currentLayer.pendingMain = null;
        }

        if (currentLayer.pendingOutline != null && currentLayer.pendingOutline.isReady()) {
            var mesh = currentLayer.pendingOutline.get();
            try {
                if (currentLayer.outlineBuffer != null) {
                    currentLayer.outlineBuffer.close();
                }

                currentLayer.outlineBuffer = mesh.build();
                currentLayer.outlineLayerIndexCount = currentLayer.outlineBuffer.getIndexCount();
                LoggerProvider.get().info("Built cloud mesh outline for layer {} with {} vertices, {} indices", 
                    layerIdx, currentLayer.outlineBuffer.getVertexCount(), currentLayer.outlineBuffer.getIndexCount());
            }
            catch (Exception e) {
                LoggerProvider.get().error("Outline mesh building finished with exception: {}", e);
            }

            currentLayer.pendingOutline = null;
        }
        else if (currentLayer.pendingOutline != null && currentLayer.pendingOutline.hasFailed()) {
            LoggerProvider.get().warn("Outline mesh building failed.");
            currentLayer.pendingOutline = null;
        }
    }

    @Nullable
    private AssetHandle<AbstractStaticMesh.Builder<?, ?>> buildMeshForLayer(
                                       MeshBuilder.MeshCallback callback,
                                       BooleanSupplier meshBuilderCond,
                                       int currentLayer,
                                       int colorModifier) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);
        
        LayerState state = layers.get(currentLayer);
        
        AbstractStaticMesh.Builder<?, ?> mesh = new B3DMesh.Builder(VertexFormat.POSITION_COLOR_NORMAL, GLPrimitive.QUADS);

        MeshType meshType = null;
        try {
            meshType = MeshTypeRegistry.getInstance().getObject(layerConfiguration.MODE);
        }
        catch (Exception e) {
            LoggerProvider.get().error("Exception getting mesh type: {}", e);
            return null;
        }
        
        if (meshBuilderCond.getAsBoolean())
            return callback.apply(mesh, meshType, state.baseCellX, state.baseCellZ, currentLayer, state, colorModifier);
        else
            return new AssetHandle<>(null);
    }

    // ------------------------------------------
    // misc utils

    private boolean meshesFinishedBuilding(LayerState currentLayer) {
        return currentLayer.pendingOutline == null && currentLayer.pendingMain == null;
    }
    
    private int packConfig(int layer) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);
        
        int config = 0;

        if (layerConfiguration.FOG_ENABLED)                                config |= 1 << 0;
        if (layerConfiguration.APPEARANCE.SHADING_ENABLED)                 config |= 1 << 1;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA)               config |= 1 << 2;
        if (layerConfiguration.APPEARANCE.CUSTOM_BRIGHTNESS)               config |= 1 << 3;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR)               config |= 1 << 4;
        if (layerConfiguration.FADE.FADE_ENABLED)                          config |= 1 << 5;
        if (layerConfiguration.FADE.COLOR_FADE)                            config |= 1 << 6;
        if (layerConfiguration.FADE.INVERTED_FADE)                         config |= 1 << 7;
        if (layerConfiguration.FADE.FADE_TYPE == FadeType.STATIC)          config |= 1 << 8;

        return config;
    }

    private int packOutlineConfig(int layer) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);
        
        int config = 0;
        if (layerConfiguration.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS)       config |= 1 << 0;
        if (layerConfiguration.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR)  config |= 1 << 1;

        return config;
    }

    // ------------------------------------------
    // resource management

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

    // ------------------------------------------
    // typedefs

    public static class LayerState implements AutoCloseable {
        int index;
        float offsetX;
        float offsetZ;

        int baseCellX;
        int baseCellZ;

        AssetHandle<AbstractStaticMesh.Builder<?, ?>> pendingMain;
        AssetHandle<AbstractStaticMesh.Builder<?, ?>> pendingOutline;

        Texture.TextureData texture;
        AbstractStaticMesh<?> mainBuffer;
        AbstractStaticMesh<?> outlineBuffer;

        boolean needsRebuild;

        boolean cellInitialized = false;
        CloudStatus prevStatus;

        int layerIndexCount;
        int outlineLayerIndexCount;

        AbstractUBODataBuffer<?, ?>  transforms;
        AbstractUBODataBuffer<?, ?>  cloudsInfo;
        AbstractUBODataBuffer<?, ?>  lighting;
        AbstractUBODataBuffer<?, ?>  camera;
        AbstractUBODataBuffer<?, ?>  outline;

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
            outline.close();
        }

        @Override
        public void close() {
            if (mainBuffer != null) {
                mainBuffer.close();
            }
            if (outlineBuffer != null) {
                outlineBuffer.close();
            }
            closeBuffers();
        }
    }
}
