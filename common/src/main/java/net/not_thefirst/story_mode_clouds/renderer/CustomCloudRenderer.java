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
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.Texture;

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
// TODO: Fix fog
@Environment(EnvType.CLIENT)
public class CustomCloudRenderer {
    public static enum CloudMode {
        NORMAL,
        POPULATED
    }

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
    private int prevSkyColor = 0;
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

    public Optional<Texture.TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        try (InputStream inputStream = resourceManager.open(TEXTURE_LOCATION);
             NativeImage nativeImage = NativeImage.read(inputStream)) {

            int w = nativeImage.getWidth();
            int h = nativeImage.getHeight();
            long[] cells = new long[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = nativeImage.getPixel(x, y);
                    if (ARGB.alpha(pixel) < 10) {
                        cells[x + y * w] = 0L;
                    } else {
                        boolean north = ARGB.alpha(nativeImage.getPixel(x, Math.floorMod(y - 1, h))) < 10;
                        boolean east  = ARGB.alpha(nativeImage.getPixel(Math.floorMod(x + 1, w), y)) < 10;
                        boolean south = ARGB.alpha(nativeImage.getPixel(x, Math.floorMod(y + 1, h))) < 10;
                        boolean west  = ARGB.alpha(nativeImage.getPixel(Math.floorMod(x - 1, w), y)) < 10;
                        cells[x + y * w] = packCellData(pixel, north, east, south, west);
                    }
                }
            }

            currentTexture = Optional.of(new Texture.TextureData(cells, w, h));
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
            prepare(client.getResourceManager(), Profiler.get());
        }

        apply(currentTexture, client.getResourceManager(), Profiler.get());
    }
    
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

            if (currentLayer.layerIndexCount > 0) {
                if (fancy) {
                    drawLayer(ModRenderPipelines.POSITION_COLOR_DEPTH, offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, layer, indices, currentLayer);
                }
                drawLayer(ModRenderPipelines.CUSTOM_POSITION_COLOR, offX, (float) (layerConfiguration.LAYER_HEIGHT - cam.y), offZ, layer, indices, currentLayer);

            }
        }
    }

    private int packConfig(int layer) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);
        
        int config = 0;
        if (layerConfiguration.FOG_ENABLED) config |= 1 << 0;
        return config;
    }

    private void drawLayer(
        RenderPipeline pipeline,
        float offX, float offY, float offZ,
        int layer,
        RenderSystem.AutoStorageIndexBuffer indices, LayerState currentLayer) {

        try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(currentLayer.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putVec4(-offX, offY, -offZ, 0.0f)
                .putInt(packConfig(layer))
                .putInt(0)
                .putInt(0)
                .putInt(0);
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
        MeshTypeBuilder builder = 
            MeshBuilderRegistry.getBuilder(layerConfiguration.MODE.name());

        builder.Build(bb, tex, pos, state, cx, cz, relYCenter, currentLayer, skyColor, offX, offZ);
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
    
    public static class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public Texture.TextureData texture;
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
        public final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud Layer UBO", 130, UBO_SIZE);

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
