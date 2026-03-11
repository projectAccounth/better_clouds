package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.ShadingMode;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CustomCloudRenderer implements AutoCloseable {

    private Optional<Texture.TextureData> currentTexture = Optional.empty();
    private final List<LayerState> layers = new ArrayList<>();
    
    protected static final IdentifierWrapper TEXTURE_LOCATION = 
        IdentifierWrapper.of("minecraft", "textures/environment/clouds.png");

    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

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
                        offX, offY, offZ,
                        layer, shaderColor, relY, dayTime, cam);
                    continue;
                }

                drawLayer(
                    ModRenderPipelines.POSITION_COLOR_DEPTH,
                        offX, offY, offZ,
                        layer, shaderColor, relY, dayTime, cam);
                drawLayer(
                    ModRenderPipelines.CUSTOM_POSITION_COLOR,
                        offX, offY, offZ,
                        layer, shaderColor, relY, dayTime, cam);
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

        try (MeshData mesh = buildMeshForLayer(
            currentLayer.texture,
            currentLayer.baseCellX, currentLayer.baseCellZ,
            currentLayer.prevStatus,
            relY, layer, skyColor)) {

            if (mesh == null) {
                currentLayer.layerIndexCount = 0;
            } else {
                if (currentLayer.buffer != null && 
                    !currentLayer.buffer.isClosed() &&
                    currentLayer.buffer.size() >= mesh.vertexBuffer().remaining()) {
                    RenderSystem.getDevice().createCommandEncoder()
                            .writeToBuffer(currentLayer.buffer.slice(), mesh.vertexBuffer());
                } else {
                    if (currentLayer.buffer != null && !currentLayer.buffer.isClosed()) currentLayer.buffer.close();
                    currentLayer.buffer = RenderSystem.getDevice().createBuffer(
                            () -> "Cloud layer " + layer,
                            72,
                            mesh.vertexBuffer()
                    );
                }
                currentLayer.layerIndexCount = mesh.drawState().indexCount();
            }
        }
    }

    @Nullable
    private MeshData buildMeshForLayer(Texture.TextureData tex,
                                       int baseCx, int baseCz,
                                       CloudStatus status,
                                       float relYCenter, int currentLayer,
                                       int skyColor) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);

        BufferBuilder mesh = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS, 
            DefaultVertexFormat.POSITION_COLOR_NORMAL
        );

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

        return mesh.buildOrThrow();
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

        if (layerConfiguration.FOG_ENABLED) config |= 1 << 0;
        if (layerConfiguration.APPEARANCE.SHADING_ENABLED) config |= 1 << 1;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA) config |= 1 << 2;
        if (layerConfiguration.APPEARANCE.CUSTOM_BRIGHTNESS) config |= 1 << 3;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR) config |= 1 << 4;
        if (layerConfiguration.FADE.FADE_ENABLED)            config |= 1 << 5;
        if (layerConfiguration.FADE.COLOR_FADE)              config |= 1 << 6;

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
            if (layer.buffer != null && !layer.buffer.isClosed()) {
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

        Texture.TextureData texture;
        GpuBuffer buffer;

        boolean needsRebuild;

        boolean cellInitialized = false;
        CloudStatus prevStatus;

        boolean bufferEmpty;
        int layerIndexCount;

        MappableRingBuffer transformsBuffer = new MappableRingBuffer(() -> "Cloud Layer Transformation UBO", 130, TRANSFORMS_SIZE);
        MappableRingBuffer cloudsInfoBuffer = new MappableRingBuffer(() -> "Cloud Layer Information UBO", 130, CLOUDS_INFO_SIZE);
        MappableRingBuffer lightingBuffer   = new MappableRingBuffer(() -> "Cloud Layer Lighting UBO", 130, LIGHTING_SIZE);
        MappableRingBuffer cameraBuffer     = new MappableRingBuffer(() -> "Cloud Layer Camera UBO", 130, CAMERA_SIZE);

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
            .putVec4()
            .get();

    private static final int TRANSFORMS_SIZE =
        new Std140SizeCalculator()
            .putVec4()
            .get();

    private static final int CLOUDS_INFO_SIZE =
        new Std140SizeCalculator()
            .putIVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private static final int LIGHTING_SIZE = 
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16 * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        16;

    private void drawLayer(
        RenderPipeline pipeline,
        float ox, float oy, float oz,
        int layerIdx,
        int skyColor,
        float relY,
        long timeTicks,
        Vec3 camPos) {

        LayerState currentLayer = this.layers.get(layerIdx);

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.getInstance().getLayer(layerIdx);

        CloudsConfiguration.LightingParameters lightingParameters = 
            CloudsConfiguration.getInstance().LIGHTING;
        List<DiffuseLight> lights = lightingParameters.lights;
        int maxLightCount = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
        float lightAmbientFactor = lightingParameters.AMBIENT_LIGHTING_STRENGTH;
        float lightShadingStrength = lightingParameters.MAX_LIGHTING_SHADING;

        float heightInBlocks = MeshBuilder.HEIGHT_IN_BLOCKS *
                (layerConfiguration.IS_ENABLED
                    ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                    : 1.0f);

        try (GpuBuffer.MappedView view =
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(currentLayer.transformsBuffer.currentBuffer(), false, true)) {

            Std140Builder.intoBuffer(view.data())
                .putVec4(-ox, oy, -oz, 1.0f);
        }

        try (GpuBuffer.MappedView view =
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(currentLayer.cloudsInfoBuffer.currentBuffer(), false, true)) {

            Std140Builder.intoBuffer(view.data())

                // ivec4 info0
                .putIVec4(
                    packConfig(layerIdx),
                    (int) layerConfiguration.FOG.FOG_START_DISTANCE,
                    (int) layerConfiguration.FOG.FOG_END_DISTANCE,
                    layerConfiguration.APPEARANCE.BASE_ALPHA
                )

                // vec4 info1
                .putVec4(
                    layerConfiguration.FADE.FADE_ALPHA,
                    layerConfiguration.FADE.TRANSITION_RANGE,
                    heightInBlocks,
                    relY
                )

                // vec4 skyColor
                .putVec4(
                    ARGB.redFloat(skyColor),
                    ARGB.greenFloat(skyColor),
                    ARGB.blueFloat(skyColor),
                    1.0f
                )

                // vec4 fadeToColor
                .putVec4(
                    ARGB.redFloat(layerConfiguration.FADE.FADE_TO_COLOR),
                    ARGB.greenFloat(layerConfiguration.FADE.FADE_TO_COLOR),
                    ARGB.blueFloat(layerConfiguration.FADE.FADE_TO_COLOR),
                    1.0f
                );
        }

        try (GpuBuffer.MappedView view =
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(currentLayer.lightingBuffer.currentBuffer(), false, true)) {

            Std140Builder builder = Std140Builder.intoBuffer(view.data());

            int lightCount = Math.min(lights.size(), maxLightCount);
            for (int i = 0; i < maxLightCount; i++) {
                if (i < lightCount) {
                    DiffuseLight l = lights.get(i);

                    float x = l.getXDirection();
                    float y = l.getYDirection();
                    float z = l.getZDirection();

                    if (CloudsConfiguration.getInstance().LIGHTING.LIGHTING_TYPE == LightingType.DYNAMIC) {
                        float[] tmp = { x, y, z };
                        l.evaluate(timeTicks, tmp);
                        x = tmp[0];
                        y = tmp[1];
                        z = tmp[2];
                    }

                    builder.putVec4(x, y, z, l.intensity());
                } else {
                    builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                }
            }

            for (int i = 0; i < maxLightCount; i++) {
                if (i < lightCount) {
                    Vector3f c = lights.get(i).color();
                    builder.putVec4(c.x(), c.y(), c.z(), 1.0f);
                } else {
                    builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                }
            }

            builder.putVec4(
                lightCount,
                lightShadingStrength,
                lightAmbientFactor,
                lightingParameters.SHADING_MODE == ShadingMode.GOURAUD ? 0 : 1
            );
        }

        try (GpuBuffer.MappedView view =
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(currentLayer.cameraBuffer.currentBuffer(), false, true)) {

            Std140Builder.intoBuffer(view.data())
                .putVec4(
                    (float) camPos.x,
                    (float) camPos.y,
                    (float) camPos.z,
                    1.0f
                );
        }

        GpuBuffer indexBuf = indices.getBuffer(currentLayer.layerIndexCount);
        GpuBufferSlice slice = RenderSystem.getDynamicUniforms()
				.writeTransform(
					RenderSystem.getModelViewMatrix(),
					new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
					new Vector3f(0, 0, 0),
					new Matrix4f()
				);

        
        var encoder = RenderSystem.getDevice().createCommandEncoder();

        RenderTarget rt = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget ct = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuTextureView colorTex;
        GpuTextureView depthTex;
        
        if (ct != null) {
            colorTex = ct.getColorTextureView();
            depthTex = ct.getDepthTextureView();
        } else {
            colorTex = rt.getColorTextureView();
            depthTex = rt.getDepthTextureView();
        }

        RenderPass pass = encoder.createRenderPass(
            () -> "Clouds",
            colorTex,
            OptionalInt.empty(),
            depthTex,
            OptionalDouble.empty() 
        );

        try {
            RenderSystem.bindDefaultUniforms(pass);

            pass.setPipeline(pipeline);
            pass.setUniform("Transforms", currentLayer.transformsBuffer.currentBuffer());
            pass.setUniform("CloudInfo", currentLayer.cloudsInfoBuffer.currentBuffer());
            pass.setUniform("Lighting", currentLayer.lightingBuffer.currentBuffer());
            pass.setUniform("Camera", currentLayer.cameraBuffer.currentBuffer());
            pass.setUniform("DynamicTransforms", slice);

            pass.setVertexBuffer(0, currentLayer.buffer);
            pass.setIndexBuffer(indexBuf, indices.type());
            pass.drawIndexed(0, 0, currentLayer.layerIndexCount, 1);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        if (pass != null) {
            pass.close();
        }
    }
}
