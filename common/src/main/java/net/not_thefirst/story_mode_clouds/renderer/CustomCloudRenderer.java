package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.CompiledMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.GpuMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.MeshUploader;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.GLProgram;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.Std140BufferBuilder;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.Std140SizeCalculator;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.UniformBufferObject;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.ShaderRenderType;
import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.memory.Cache;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer implements AutoCloseable {

    @Nullable
    public Optional<Texture.TextureData> currentTexture = Optional.empty();
    private final List<LayerState> layers = new ArrayList<>();

    protected static final ResourceLocation TEXTURE_LOCATION = 
        new ResourceLocation("minecraft", "textures/environment/clouds.png");

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
            layerState.prevStatus = null;
            layers.add(layerState);
        }

        for (int i = 0; i < layerCount; i++) {
            LayerState layer = layers.get(i);
            CloudsConfiguration.LayerConfiguration layerConfig = CloudsConfiguration.INSTANCE.getLayer(i);
            layer.offsetX = layerConfig.APPEARANCE.LAYER_OFFSET_X;
            layer.offsetZ = layerConfig.APPEARANCE.LAYER_OFFSET_Z;
        }
    }

    public Optional<Texture.TextureData> prepare(
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller,
        ResourceLocation textureLocation
    ) {
        try (InputStream inputStream = 
            resourceManager.getResource(textureLocation).getInputStream()) {

            this.currentTexture = Texture.buildTexture(inputStream);
            return this.currentTexture;
        }
        catch (IOException exception) {
            exception.printStackTrace();
            System.out.println("[cloud_tweaks]: Failed to build cloud texture, discarding");
            return null;
        }
    }

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
            prepare(client.getResourceManager(), client.getProfiler(), TEXTURE_LOCATION);
        }

        apply(currentTexture, client.getResourceManager(), client.getProfiler());
    }
    
    public void render(int skyColor, CloudStatus status, float cloudHeight, Vec3 cam, float tickDelta, PoseStack poseStack) {
        double baseDx = cam.x;
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

        skyColor = CloudColorProvider.getCloudColor();
        Minecraft client = Minecraft.getInstance();
        float time = (client.level.getGameTime() + tickDelta) / 20.0F;

        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        Matrix4f proj = renderer.getProjectionMatrix(renderer.getMainCamera(), tickDelta, true);

        startRender();
        for (int layer : order) {
            LayerState currentLayer = this.layers.get(layer);

            CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);

            if (!layerConfiguration.LAYER_RENDERED) continue;

            MeshType type = null;
            try {
                type = MeshTypeRegistry
                    .getInstance().getObject(layerConfiguration.MODE);
            }
            catch (Throwable exception) {
                exception.printStackTrace();
                continue;
            }

            if (type == null) continue;

            currentLayer.offsetX = layerConfiguration.APPEARANCE.LAYER_OFFSET_X;
            currentLayer.offsetZ = layerConfiguration.APPEARANCE.LAYER_OFFSET_Z;

            Texture.TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width * MeshBuilder.CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height * MeshBuilder.CELL_SIZE_IN_BLOCKS;

            double timeOffsetX = time * layerConfiguration.APPEARANCE.LAYER_SPEED_X;
            double timeOffsetZ = time * layerConfiguration.APPEARANCE.LAYER_SPEED_Z;

            double dxLayer = baseDx + currentLayer.offsetX + timeOffsetX;
            double dzLayer = baseDz + currentLayer.offsetZ + timeOffsetZ;

            dxLayer -= Mth.floor(dxLayer / wrapX) * wrapX;
            dzLayer -= Mth.floor(dzLayer / wrapZ) * wrapZ;

            float cloudChunkHeight = MeshBuilder.HEIGHT_IN_BLOCKS * 
                (layerConfiguration.IS_ENABLED ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE : 1.0F);
            float layerY = (float)(layerConfiguration.LAYER_HEIGHT - cam.y);
            float relYTop = layerY + cloudChunkHeight;

            int cellX = Mth.floor(dxLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dzLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);

            if (currentLayer.prevCellX == Integer.MIN_VALUE) {
                currentLayer.baseCellX = cellX;
                currentLayer.baseCellZ = cellZ;
                currentLayer.needsRebuild = true;
            }

            int range = CloudsConfiguration.INSTANCE.CLOUD_GRID_SIZE;
            int slack = range * 2 / 3;

            int dxCell = cellX - currentLayer.baseCellX;
            int dzCell = cellZ - currentLayer.baseCellZ;

            if (Math.abs(dxCell) > slack || Math.abs(dzCell) > slack) {
                currentLayer.baseCellX = cellX;
                currentLayer.baseCellZ = cellZ;
                currentLayer.needsRebuild = true;
            }
                        
            float offX = (float)(dxLayer - currentLayer.baseCellX * MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dzLayer - currentLayer.baseCellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            float relY = (float)(relYTop - cloudChunkHeight / 2.0f);

            boolean statusChanged = status != currentLayer.prevStatus;

            boolean needs = currentLayer.needsRebuild
                || statusChanged;

            if (needs) {
                currentLayer.needsRebuild  = false;
                currentLayer.prevCellX     = cellX;
                currentLayer.prevCellZ     = cellZ;
                currentLayer.prevStatus    = status;
                currentLayer.currentStatus = status;

                if (currentLayer.buffer != null) {
                    currentLayer.buffer.close();
                }

                CompiledMesh mesh = buildMeshForLayer(
                    tex, Tesselator.getInstance(), 
                    currentLayer.baseCellX, currentLayer.baseCellZ,
                    status,
                    relY, layer, skyColor);
                
                currentLayer.meshCache.put(0, mesh);
                
                if (mesh != null) {
                    currentLayer.buffer = MeshUploader.upload(mesh);
                    currentLayer.bufferEmpty = false;
                } else {
                    currentLayer.bufferEmpty = true;
                }
            }

            poseStack.pushPose();
            if (currentLayer.bufferEmpty) {
                poseStack.popPose();
                continue;
            }

            int shaderColor = ColorUtils.getCloudShaderColor(layer, skyColor);

            if (!type.doDepthWrite()) {
                drawLayer(ModRenderPipelines.POSITION_COLOR_NO_DEPTH, poseStack.last().pose(), proj,
                    offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ,
                    currentLayer.buffer,
                    layer, currentLayer, shaderColor, tickDelta, relY);
                poseStack.popPose();
                continue;
            }

            drawLayer(
                ModRenderPipelines.POSITION_COLOR_DEPTH_ONLY, poseStack.last().pose(), proj,
                    offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ,
                    currentLayer.buffer,
                    layer, currentLayer, shaderColor, tickDelta, relY);
            drawLayer(
                ModRenderPipelines.CUSTOM_POSITION_COLOR, poseStack.last().pose(), proj,
                    offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ,
                    currentLayer.buffer,
                    layer, currentLayer, shaderColor, tickDelta, relY);

            poseStack.popPose();
        }
        finishRender();
    }

    @Nullable
    private CompiledMesh buildMeshForLayer(Texture.TextureData tex,
                                       Tesselator tess, 
                                       int baseCx, int baseCz,
                                       CloudStatus status,
                                       float relYCenter, int currentLayer,
                                       int skyColor) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);

        BuildingMesh mesh = new BuildingMesh(VertexFormat.POSITION_COLOR_NORMAL, GL11.GL_QUADS);

        MeshTypeBuilder builder = null;
        try {
            builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);
        }
        catch (Throwable exception) {
            exception.printStackTrace();
            return null;
        }

        builder.Build(mesh, tex, state, baseCx, baseCz, relYCenter, currentLayer, skyColor, 0, 0);
        return mesh.compile();
    }

    private void startRender() {
        RenderSystem.disableTexture();
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableFog();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_COLOR, 
            GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, 
            GlStateManager.SourceFactor.ONE, 
            GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR
        );
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
    }

    private void finishRender() {
        RenderSystem.enableTexture();
        RenderSystem.popMatrix();
        RenderSystem.disableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.shadeModel(GL11.GL_FLAT);
    }

    private int packConfig(int layer) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);
        
        int config = 0;

        if (layerConfiguration.FOG_ENABLED) config |= 1 << 0;
        if (layerConfiguration.APPEARANCE.SHADING_ENABLED) config |= 1 << 1;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA) config |= 1 << 2;
        if (layerConfiguration.APPEARANCE.CUSTOM_BRIGHTNESS) config |= 1 << 3;
        if (layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR) config |= 1 << 4;
        if (layerConfiguration.FADE.FADE_ENABLED)            config |= 1 << 5;

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
            layer.meshCache.clear();
        }
    }
    
    public static class LayerState {
        public int index;
        public float offsetX, offsetZ;
        
        public Cache<CompiledMesh> meshCache = new Cache<>(3);

        public int baseCellX;
        public int baseCellZ;

        public Texture.TextureData texture;
        public GpuMesh buffer;

        public boolean needsRebuild;

        public int 
            prevCellX = Integer.MIN_VALUE, 
            prevCellZ = Integer.MIN_VALUE;

        public CloudStatus prevStatus;
        public CloudStatus currentStatus;

        public double phaseX = Double.MIN_VALUE;
        public double phaseZ = Double.MIN_VALUE;
        public boolean bufferEmpty;

        public UniformBufferObject transformsBuffer = new UniformBufferObject(0, TRANSFORMS_SIZE);
        public UniformBufferObject cloudsInfoBuffer = new UniformBufferObject(1, CLOUDS_INFO_SIZE);
        public UniformBufferObject lightingBuffer   = new UniformBufferObject(2, LIGHTING_SIZE);

        public LayerState(int index) {
            this.index = index;
        }

        public LayerState() {
            this(-1);
        }
    }

    private static final int TRANSFORMS_SIZE =
        new Std140SizeCalculator()
            .addMat4()
            .addMat4()
            .addVec4()
            .finish().offset();

    private static final int CLOUDS_INFO_SIZE =
        new Std140SizeCalculator()
            .addIVec4()
            .addVec4()
            .addVec4()
            .finish().offset();

    private static final int LIGHTING_SIZE = 
        Std140SizeCalculator.getVec4Size() * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        Std140SizeCalculator.getVec4Size() * CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT +
        Std140SizeCalculator.getVec4Size();

    private void drawLayer(
        ShaderRenderType rt,
        Matrix4f mv, Matrix4f proj,
        float ox, float oy, float oz, 
        GpuMesh buf,
        int layer,
        LayerState currentLayer, 
        int skyColor, 
        float tickDelta,
        float relY) {
        rt.setup();

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.INSTANCE.getLayer(layer);

        GLProgram shader = rt.program();

        Std140BufferBuilder transforms = new Std140BufferBuilder(TRANSFORMS_SIZE);
        Std140BufferBuilder cloudsInfo = new Std140BufferBuilder(CLOUDS_INFO_SIZE);
        Std140BufferBuilder lighting   = new Std140BufferBuilder(LIGHTING_SIZE);

        CloudsConfiguration.LightingParameters LIGHTING = 
            CloudsConfiguration.INSTANCE.LIGHTING;
        List<DiffuseLight> lights = LIGHTING.lights;
        int maxLightCount = CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
        float lightAmbientFactor = LIGHTING.AMBIENT_LIGHTING_STRENGTH;
        float lightShadingStrength = LIGHTING.MAX_LIGHTING_SHADING;

        transforms.putMat4(proj);
        transforms.putMat4(mv);
        transforms.putVec4(-ox, oy, -oz, 1.0f);

        // Info0
        cloudsInfo.putIVec4(
            packConfig(layer),
            (int) layerConfiguration.FOG.FOG_START_DISTANCE,
            (int) layerConfiguration.FOG.FOG_END_DISTANCE,
            layerConfiguration.APPEARANCE.BASE_ALPHA
        );

        // Info1
        cloudsInfo.putVec4(
            (float) layerConfiguration.FADE.FADE_ALPHA,
            layerConfiguration.FADE.TRANSITION_RANGE,
            MeshBuilder.HEIGHT_IN_BLOCKS *
                (layerConfiguration.IS_ENABLED
                    ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                    : 1.0f),
            relY
        );

        cloudsInfo.putVec4(
            ARGB.redFloat(skyColor),
            ARGB.greenFloat(skyColor),
            ARGB.blueFloat(skyColor),
            1.0f
        );

        int lightCount = Math.min(lights.size(), maxLightCount);

        for (int i = 0; i < maxLightCount; i++) {
            if (i < lightCount) {
                DiffuseLight l = lights.get(i).normalized();
                lighting.putVec4(
                    l.direction.x(),
                    l.direction.y(),
                    l.direction.z(),
                    l.intensity
                );
            } else {
                lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightColors
        for (int i = 0; i < maxLightCount; i++) {
            if (i < lightCount) {
                Vector3f c = lights.get(i).color;
                lighting.putVec4(c.x(), c.y(), c.z(), 1.0f);
            } else {
                lighting.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }

        // LightInformation
        lighting.putVec4(
            (float) lightCount,
            lightShadingStrength,
            lightAmbientFactor,
            0.0f
        );


        currentLayer.transformsBuffer.bind();
        currentLayer.cloudsInfoBuffer.bind();
        currentLayer.lightingBuffer.bind();

        currentLayer.transformsBuffer.update(transforms.build());
        currentLayer.cloudsInfoBuffer.update(cloudsInfo.build());
        currentLayer.lightingBuffer.update(lighting.build());

        buf.draw();

        rt.clear();
    }
}
