package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.mesh.CompiledMesh;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.utils.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer {
    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4()
        .putInt()
        .putInt()
        .putInt()
        .putInt()
        .get();

    private final RenderSystem.AutoStorageIndexBuffer indices =
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    @Nullable
    public Optional<Texture.TextureData> currentTexture = Optional.empty();
    private final List<LayerState> layers = new ArrayList<>();

    protected static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/clouds.png");

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
        try (InputStream inputStream = resourceManager.open(textureLocation);
            NativeImage nativeImage = NativeImage.read(inputStream)) {

            int w = nativeImage.getWidth();
            int h = nativeImage.getHeight();

            long[] cells = new long[w * h];
            byte[] neighbors = new byte[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = x + y * w;
                    int pixel = nativeImage.getPixel(x, y);

                    if (ARGB.alpha(pixel) < 10) {
                        cells[idx] = 0L;
                        neighbors[idx] = 0;
                        continue;
                    }

                    int count = 0;

                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dz == 0) {
                                continue;
                            }

                            if (isSolid(nativeImage, x + dx, y + dz, w, h)) {
                                count++;
                            }
                        }
                    }

                    boolean n  = !isSolid(nativeImage, x,     y - 1, w, h);
                    boolean e  = !isSolid(nativeImage, x + 1, y,     w, h);
                    boolean s  = !isSolid(nativeImage, x,     y + 1, w, h);
                    boolean w0 = !isSolid(nativeImage, x - 1, y,     w, h);

                    cells[idx] = packCellData(pixel, n, e, s, w0);
                    neighbors[idx] = (byte)count;
                }
            }

            currentTexture = Optional.of(
                new Texture.TextureData(cells, neighbors, w, h)
            );

            return currentTexture;

        } catch (IOException e) {
            System.out.println("Failed to load cloud texture: " + e);
            return Optional.empty();
        }
    }

    private static boolean isSolid(NativeImage img, int x, int y, int w, int h) {
        return ARGB.alpha(img.getPixel(
            Math.floorMod(x, w),
            Math.floorMod(y, h)
        )) >= 10;
    }


    private static long packCellData(int color, boolean north, boolean east, boolean south, boolean west) {
        return (long) color << 4 |
               (north ? 1 : 0) << 3 |
               (east ? 1 : 0) << 2 |
               (south ? 1 : 0) << 1 |
               (west ? 1 : 0);
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
            prepare(client.getResourceManager(), Profiler.get(), TEXTURE_LOCATION);
        }

        apply(currentTexture, client.getResourceManager(), Profiler.get());
    }
    
    public void render(int skyColor, CloudStatus status, float cloudHeight, Vec3 cam, float tickDelta) {
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

            RelativeCameraPos layerPos =
                (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                (layerY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            int cellX = Mth.floor(dxLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dzLayer / MeshBuilder.CELL_SIZE_IN_BLOCKS);

            if (currentLayer.prevCellX == Integer.MIN_VALUE) {
                currentLayer.baseCellX = cellX;
                currentLayer.baseCellZ = cellZ;
                currentLayer.needsRebuild = true;
            }

            int range = 32;
            int slack = range / 2;

            int dxCell = cellX - currentLayer.baseCellX;
            int dzCell = cellZ - currentLayer.baseCellZ;

            if (Math.abs(dxCell) > slack || Math.abs(dzCell) > slack) {
                currentLayer.baseCellX = cellX;
                currentLayer.baseCellZ = cellZ;
                currentLayer.needsRebuild = true;
            }
                        
            float offX = (float)(dxLayer - currentLayer.baseCellX * MeshBuilder.CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dzLayer - currentLayer.baseCellZ * MeshBuilder.CELL_SIZE_IN_BLOCKS);

            long now = System.currentTimeMillis();

            float relY = (float)(relYTop - cloudChunkHeight / 2.0f);
            float transition = Math.max(0.0001f, layerConfiguration.FADE.TRANSITION_RANGE);
            float dir = Mth.clamp(relY / transition, -1.0f, 1.0f);

            boolean cameraChanged =
                layerPos != currentLayer.prevPos
                || status != currentLayer.prevStatus;

            boolean fadeChanged = layerConfiguration.FADE.FADE_ENABLED &&
                    Math.abs(relY) <= layerConfiguration.FADE.TRANSITION_RANGE &&
                    (Float.isNaN(currentLayer.prevFadeMix) || 
                     Math.abs(dir - currentLayer.prevFadeMix) > 0.02f);

            boolean needs = currentLayer.needsRebuild
                    || cameraChanged
                    || (fadeChanged && currentLayer.fadeThrottler.shouldRebuild(now));

            if (needs) {
                currentLayer.needsRebuild  = false;
                currentLayer.prevCellX     = cellX;
                currentLayer.prevCellZ     = cellZ;
                currentLayer.prevPos       = layerPos;
                currentLayer.prevStatus    = status;
                currentLayer.currentStatus = status;
                currentLayer.prevFadeMix   = dir;

                try (MeshData mesh = buildMeshForLayer(
                    tex, Tesselator.getInstance(), 
                    currentLayer.baseCellX,
                    currentLayer.baseCellZ,
                    status, 
                    layerPos, relY, layer, 
                    skyColor, 
                    0.0F, 0.0F)) {

                    if (mesh == null) {
                        currentLayer.layerIndexCount = 0;
                    } else {
                        if (currentLayer.buffer != null && currentLayer.buffer.size() >= mesh.vertexBuffer().remaining()) {
                            RenderSystem.getDevice().createCommandEncoder()
                                    .writeToBuffer(currentLayer.buffer.slice(), mesh.vertexBuffer());
                        } else {
                            if (currentLayer.buffer != null) currentLayer.buffer.close();
                            currentLayer.buffer = RenderSystem.getDevice().createBuffer(
                                    () -> "Cloud layer " + layer,
                                    72, // 72,32,64, any 8-bit combination idfk
                                    mesh.vertexBuffer()
                            );
                        }
                        currentLayer.layerIndexCount = mesh.drawState().indexCount();
                    }
                }
            }

            if (currentLayer.layerIndexCount <= 0) {
                continue;
            }

            if (!type.doDepthWrite()) {
                drawLayer(ModRenderPipelines.POSITION_COLOR_NO_DEPTH, 
                    offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, 
                    layer, currentLayer, skyColor);
                continue;
            }

            drawLayer(
                ModRenderPipelines.POSITION_COLOR_DEPTH, 
                offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, 
                layer, currentLayer, skyColor);
            drawLayer(
                ModRenderPipelines.CUSTOM_POSITION_COLOR, 
                offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, 
                layer, currentLayer, skyColor);
        }
    }

    @Nullable
    private MeshData buildMeshForLayer(Texture.TextureData tex,
                                       Tesselator tess, int cx, int cz,
                                       CloudStatus status, RelativeCameraPos pos,
                                       float relYCenter, int currentLayer,
                                       int skyColor, float offX, float offZ) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        LayerState state = layers.get(currentLayer);
        RenderPipeline pipe = ModRenderPipelines.CUSTOM_POSITION_COLOR;

        BufferBuilder bb = tess.begin(pipe.getVertexFormatMode(), pipe.getVertexFormat());
        MeshTypeBuilder builder = null;
        try {
            builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);
        }
        catch (Throwable exception) {
            exception.printStackTrace();
            return null;
        }

        builder.Build(bb, tex, pos, state, cx, cz, relYCenter, currentLayer, skyColor, offX, offZ);
        return bb.buildOrThrow();
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

    public void close() {
        for (LayerState layer : layers) {
            if (layer.buffer != null) layer.buffer.close();
        }
    }
    
    public static class LayerState {
        public int index;
        public float offsetX, offsetZ;

        public CompiledMesh cachedMesh;

        public int baseCellX;
        public int baseCellZ;

        public Texture.TextureData texture;
        public GpuBuffer buffer;

        public int indexCount;
        public boolean needsRebuild;

        public int 
            prevCellX = Integer.MIN_VALUE, 
            prevCellZ = Integer.MIN_VALUE;
        public RelativeCameraPos prevPos;

        public CloudStatus prevStatus;
        public CloudStatus currentStatus;

        public float prevFadeMix;
        public boolean bufferEmpty;
        public int layerIndexCount;

        public final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud Layer UBO", 130, UBO_SIZE);
        
        public final RebuildThrottler fadeThrottler = new RebuildThrottler(100);

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

    private void drawLayer(
        RenderPipeline pipeline,
        float offX, float offY, float offZ,
        int layer,
        LayerState currentLayer,
        int skyColor) {

        int shaderCloudColor = ColorUtils.getCloudShaderColor(layer, skyColor);
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.INSTANCE.getLayer(layer);

        try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(currentLayer.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putVec4(ARGB.redFloat(shaderCloudColor), 
                    ARGB.greenFloat(shaderCloudColor), 
                    ARGB.blueFloat(shaderCloudColor), 
                    ARGB.alphaFloat(shaderCloudColor))
                .putInt(packConfig(layer))
                .putInt((int) layerConfiguration.FOG.FOG_START_DISTANCE)
                .putInt((int) layerConfiguration.FOG.FOG_END_DISTANCE)
                .putInt(0); // pad
        }

        GpuBuffer indexBuf = indices.getBuffer(currentLayer.layerIndexCount);
        RenderTarget rt = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget ct = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
				.writeTransform(
					RenderSystem.getModelViewMatrix(),
					new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
					new Vector3f(-offX, offY, -offZ),
					new Matrix4f(),
                    0.0f
				);
        GpuTextureView colorTex = rt.getColorTextureView();
        GpuTextureView depthTex = rt.getDepthTextureView();

        if (ct != null) {
            colorTex = ct.getColorTextureView();
            depthTex = ct.getDepthTextureView();
        } else {
            colorTex = rt.getColorTextureView();
            depthTex = rt.getDepthTextureView();
        }
        RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Clouds", colorTex, OptionalInt.empty(), depthTex, OptionalDouble.empty());

        try {
            RenderSystem.bindDefaultUniforms(pass);

            pass.setPipeline(pipeline);
            pass.setUniform("Model", currentLayer.ubo.currentBuffer());
            pass.setUniform("DynamicTransforms", gpuBufferSlice);

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
