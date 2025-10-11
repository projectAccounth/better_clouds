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
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    private static final float CELL_SIZE = 12.0F;
    private static final float HEIGHT = 4.0F;
    private static final int UBO_SIZE = new Std140SizeCalculator()
            .putVec4()  // Sky color / tint
            .putVec4()  // Offset
            .putVec4()  // Dimensions
            .putFloat() // BASE_ALPHA
            .putFloat() // FADE_ALPHA
            .putFloat() // BRIGHTNESS
            .putFloat() // TRANSITION_RANGE
            .putFloat() // CLOUD_LAYER_SPACING
            .putInt()   // Layer index
            .putInt()   // Config flags
            .putFloat()  // Padding
            .get();

    private final LayerState[] layers;
    private final int maxLayers;
    // Removed global utb; each layer has its own utb now
    @Nullable
    private TextureData baseTexture;

    public CustomCloudRenderer() {
        super();
        this.maxLayers = CloudsConfiguration.MAX_LAYER_COUNT;
        this.layers = new LayerState[maxLayers];

        Random rand = CloudsConfiguration.get().CLOUD_RANDOM_LAYERS ? new Random(0xC0FFEE) : null;
        for (int i = 0; i < maxLayers; i++) {
            layers[i] = new LayerState(
                    rand != null ? rand.nextFloat() * CELL_SIZE * 64f : 0f,
                    rand != null ? rand.nextFloat() * CELL_SIZE * 64f : 0f,
                    i
            );
        }
    }

    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        this.baseTexture = optional.orElse(null);
        for (LayerState layer : layers) {
            layer.texture = baseTexture;
            layer.needsRebuild = true;
        }
    }

    public void render(int skyColor, CloudStatus status, float cloudHeight, Vec3 cam, float tickDelta) {
        int activeLayers = Mth.clamp(CloudsConfiguration.get().CLOUD_LAYERS, 0, maxLayers);
        if (activeLayers <= 0 || baseTexture == null) return;

        boolean fancy = status == CloudStatus.FANCY;

        int radius = Mth.ceil(Math.min(Minecraft.getInstance().options.cloudRange().get(), 128) * 16 / CELL_SIZE);

        // ensure per-layer UTB capacity for active layers
        for (int i = 0; i < activeLayers; i++) {
            layers[i].ensureUTB(radius);
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < activeLayers; i++) order.add(i);
        order.sort((a, b) -> {
            float ya = (float)(cloudHeight - cam.y) + a * CloudsConfiguration.get().CLOUD_LAYERS_SPACING;
            float yb = (float)(cloudHeight - cam.y) + b * CloudsConfiguration.get().CLOUD_LAYERS_SPACING;
            return Float.compare(Math.abs(yb), Math.abs(ya));
        });

        for (int idx : order) {
            renderLayer(idx, skyColor, Minecraft.getInstance().level.dimensionType().cloudHeight().orElse(192), cam, fancy, status, radius, tickDelta);
        }
    }

    private void renderLayer(int idx, int skyColor, float cloudHeight, Vec3 cam,
                            boolean fancy, CloudStatus status, int radius, float dt) {
        LayerState layer = layers[idx];
        TextureData tex = layer.texture;
        if (tex == null) return;

        // Anchor the cloud grid to a fixed world-space origin
        double worldX = cam.x + dt * 0.03f;
        double worldZ = cam.z + 3.96f;

        double wrapX = tex.width() * CELL_SIZE;
        double wrapZ = tex.height() * CELL_SIZE;

        // normalize offsets into wrap range
        double normOffX = ((layer.offsetX % wrapX) + wrapX) % wrapX;
        double normOffZ = ((layer.offsetZ % wrapZ) + wrapZ) % wrapZ;

        // apply offset BEFORE wrapping
        worldX += normOffX;
        worldZ += normOffZ;

        double dx = worldX - Mth.floor(worldX / wrapX) * wrapX;
        double dz = worldZ - Mth.floor(worldZ / wrapZ) * wrapZ;

        int cellX = Mth.floor(dx / CELL_SIZE);
        int cellZ = Mth.floor(dz / CELL_SIZE);
        float offX = (float) (dx - cellX * CELL_SIZE);
        float offZ = (float) (dz - cellZ * CELL_SIZE);


        float layerY = cloudHeight + idx * CloudsConfiguration.get().CLOUD_LAYERS_SPACING;
        float relY = layerY - (float) cam.y;
        RelativeCameraPos pos = computeRelativePos(relY, HEIGHT);

        boolean rebuild = layer.needsRebuild || cellX != layer.prevCellX || cellZ != layer.prevCellZ
                || pos != layer.prevRelativePos || status != layer.prevStatus;

        if (rebuild) {
            rebuildLayer(layer, tex, cellX, cellZ, pos, status, idx, radius);
        }

        if (layer.quadCount > 0) {
            writeUBO(layer, offX, offZ, skyColor, relY, relY);
            drawLayer(layer, layer.quadCount, fancy);
        }
    }

    private void rebuildLayer(LayerState layer, TextureData tex, int cellX, int cellZ,
                              RelativeCameraPos pos, CloudStatus status, int layerIdx, int radius) {
        // ensure layer utb is available/resized
        layer.ensureUTB(radius);

        // rotate/map the per-layer UTB
        layer.utb.rotate();
        try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(layer.utb.currentBuffer(), false, true)) {
            buildMeshForLayer(tex, mapped.data(), cellX, cellZ, status, pos, layerIdx, radius);
            layer.quadCount = mapped.data().position() / 3;
        }

        layer.prevCellX = cellX;
        layer.prevCellZ = cellZ;
        layer.prevRelativePos = pos;
        layer.prevStatus = status;
        layer.needsRebuild = false;
    }

    private void buildMeshForLayer(TextureData tex, ByteBuffer buf, int baseCellX, int baseCellZ,
                                CloudStatus status, RelativeCameraPos pos, int layerIdx, int radius) {
        if (tex == null) return;
        long[] cells = tex.cells();
        int width = tex.width();
        int height = tex.height();

        for (int n = 0; n <= 2 * radius; n++) {
            for (int o = -n; o <= n; o++) {
                int p = n - Math.abs(o);
                if (p >= 0 && p <= radius && o * o + p * p <= radius * radius) {
                    if (p != 0) tryBuildCell(pos, buf, baseCellX, baseCellZ, o, -p, status, width, height, cells);
                    tryBuildCell(pos, buf, baseCellX, baseCellZ, o, p, status, width, height, cells);
                }
            }
        }
    }

    private void tryBuildCell(RelativeCameraPos pos, ByteBuffer buf, int baseCellX, int baseCellZ,
                            int offsetX, int offsetZ, CloudStatus fancy, int texWidth, int texHeight, long[] cells) {
        int cellX = Math.floorMod(baseCellX + offsetX, texWidth);
        int cellZ = Math.floorMod(baseCellZ + offsetZ, texHeight);

        long cellData = cells[cellX + cellZ * texWidth];
        if (cellData == 0L) return;

        if (fancy == CloudStatus.FANCY) {
            buildExtrudedCell(pos, buf, offsetX, offsetZ, cellData);
        } else {
            buildFlatCell(buf, offsetX, offsetZ);
        }
    }


    private void buildFlatCell(ByteBuffer buf, int x, int z) {
        encodeFace(buf, x, z, Direction.DOWN, 32);
    }

    private static boolean isNorthEmpty(long l) {
		return (l >> 3 & 1L) != 0L;
	}

	private static boolean isEastEmpty(long l) {
		return (l >> 2 & 1L) != 0L;
	}

	private static boolean isSouthEmpty(long l) {
		return (l >> 1 & 1L) != 0L;
	}

	private static boolean isWestEmpty(long l) {
		return (l >> 0 & 1L) != 0L;
	}

    private void buildExtrudedCell(RelativeCameraPos pos, ByteBuffer buf, int x, int z, long cellData) {
        if (pos != RelativeCameraPos.BELOW_CLOUDS) encodeFace(buf, x, z, Direction.UP, 0);
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) encodeFace(buf, x, z, Direction.DOWN, 0);

        if (isNorthEmpty(cellData) && z > 0) encodeFace(buf, x, z, Direction.NORTH, 0);
        if (isSouthEmpty(cellData) && z < 0) encodeFace(buf, x, z, Direction.SOUTH, 0);
        if (isWestEmpty(cellData) && x > 0) encodeFace(buf, x, z, Direction.WEST, 0);
        if (isEastEmpty(cellData) && x < 0) encodeFace(buf, x, z, Direction.EAST, 0);

        boolean inner = Math.abs(x) <= 1 && Math.abs(z) <= 1;
        if (inner) {
            for (Direction dir : Direction.values()) {
                encodeFace(buf, x, z, dir, 16);
            }
        }
    }

    private void encodeFace(ByteBuffer buf, int x, int z, Direction dir, int flag) {
        int encoded = dir.get3DDataValue() | flag;
        encoded |= (x & 1) << 7;
        encoded |= (z & 1) << 6;
        buf.put((byte)(x >> 1)).put((byte)(z >> 1)).put((byte)encoded);
    }

    private int getConfigFlag(CloudsConfiguration config) {
        int flags = 0;
        if (config.IS_ENABLED)        flags |= 1 << 0;
        if (config.APPEARS_SHADED)    flags |= 1 << 1;
        if (config.USES_CUSTOM_ALPHA) flags |= 1 << 2;
        if (config.CUSTOM_BRIGHTNESS) flags |= 1 << 3;
        if (config.USES_CUSTOM_COLOR) flags |= 1 << 4;
        if (config.FOG_ENABLED)       flags |= 1 << 5;
        if (config.FADE_ENABLED)      flags |= 1 << 6;
        if (config.RANDOMIZED_Y)      flags |= 1 << 7;
        return flags;
    }

    /**
     * 
     * @param layer The current layer state.
     * @param offX The current X offset from the cloud's origin (counts as the top-left origin)
     * @param offZ The current Z offset from the cloud's origin (counts as the top-left origin)
     * @param skyColor The current Vanilla's cloud color.
     * @param y The relative distance from camera to the layer's Y position.
     * @param relYToCenter The relative distance from camera to the layer's Y center position.
     */
    private void writeUBO(LayerState layer, float offX, float offZ, int skyColor, float y, float relYToCenter) {
        CloudsConfiguration config = CloudsConfiguration.get();

        int appliedColor;
        if (!config.IS_ENABLED) {
            // full vanilla fallback
            appliedColor = skyColor;
        } else if (config.USES_CUSTOM_COLOR) {
            appliedColor = config.CLOUD_COLORS[layer.index];
        } else if (config.CUSTOM_BRIGHTNESS) {
            // brightness handled in shader, force white
            appliedColor = ARGB.colorFromFloat(1, 1, 1, 1);
        } else {
            // default vanilla color
            appliedColor = skyColor;
        }

        float cloudChunkHeight = HEIGHT * (config.IS_ENABLED ? config.CLOUD_Y_SCALE : 1.0f);

        try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(layer.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mapped.data())
                .putVec4(ARGB.redFloat(appliedColor),
                        ARGB.greenFloat(appliedColor),
                        ARGB.blueFloat(appliedColor),
                        1.0f)
                .putVec4(-offX, y, -offZ, 0.0f)
                .putVec4(CELL_SIZE, cloudChunkHeight, CELL_SIZE, 0.0f)
                .putFloat(config.BASE_ALPHA)       // BASE_ALPHA
                .putFloat(config.FADE_ALPHA)       // FADE_ALPHA
                .putFloat(config.BRIGHTNESS)       // BRIGHTNESS
                .putFloat(config.TRANSITION_RANGE) // TRANSITION_RANGE
                .putFloat(config.CLOUD_LAYERS_SPACING) // CLOUD_LAYERS_SPACING
                .putInt(layer.index)               // Layer index
                .putInt(getConfigFlag(config))          // Config flag
                .putFloat(relYToCenter);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawLayer(LayerState layer, int quadCount, boolean fancy) {
        if (quadCount == 0 || layer.utb == null) return;

        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpuBuffer = indices.getBuffer(6 * quadCount);

        RenderTarget rt = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget ct = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
        GpuTextureView colorTex = rt.getColorTextureView();
        GpuTextureView depthTex = rt.getDepthTextureView();

        if (ct != null) {
            colorTex = ct.getColorTextureView();
            depthTex = ct.getDepthTextureView();
        } else {
            colorTex = rt.getColorTextureView();
            depthTex = rt.getDepthTextureView();
        }

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Clouds", colorTex, OptionalInt.empty(), depthTex, OptionalDouble.empty())) {
            RenderPipeline pipeline = fancy ? ModRenderPipelines.CLOUDS_CUSTOM_PIPELINE : RenderPipelines.FLAT_CLOUDS;
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", gpuBufferSlice);
            pass.setUniform("CloudInfo", layer.ubo.currentBuffer());
            pass.setUniform("CloudFaces", layer.utb.currentBuffer()); // per-layer
            pass.setIndexBuffer(gpuBuffer, indices.type());
            pass.setVertexBuffer(0, RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(quadCount));
            pass.setPipeline(pipeline);
            pass.drawIndexed(0, 0, 6 * quadCount, 1);
        }
    }

    private RelativeCameraPos computeRelativePos(float baseY, float height) {
        float top = baseY + height;
        if (top < 0) return RelativeCameraPos.ABOVE_CLOUDS;
        if (baseY > 0) return RelativeCameraPos.BELOW_CLOUDS;
        return RelativeCameraPos.INSIDE_CLOUDS;
    }

    private static int computeUTBSize(int radius) {
        int k = (radius + 1) * (radius + 1) * 2;
        return (k * 4 + 54) * 3;
    }

    @Override
    public void close() {
        for (LayerState layer : layers) {
            layer.ubo.close();
            if (layer.utb != null) {
                layer.utb.close();
            }
        }
    }

    @Override
    public void endFrame() {
        for (LayerState layer : layers) {
            layer.ubo.rotate();
        }
    }

    private static class LayerState {
        int index;
        float offsetX, offsetZ;
        int prevCellX = Integer.MIN_VALUE, prevCellZ = Integer.MIN_VALUE;
        RelativeCameraPos prevRelativePos = RelativeCameraPos.INSIDE_CLOUDS;
        CloudStatus prevStatus = null;
        boolean needsRebuild = true;
        int quadCount = 0;
        @Nullable TextureData texture;

        @Nullable
        MappableRingBuffer utb;

        @Nullable
        MappableRingBuffer ubo;

        public LayerState(float x, float z, int index) { 
            this.offsetX = x; this.offsetZ = z;
            this.index = index;
            this.ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
        }

        public void ensureUTB(int radius) {
            int size = computeUTBSize(radius);
            if (this.utb == null || this.utb.currentBuffer().size() != size) {
                if (this.utb != null) this.utb.close();
                this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, size);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public enum RelativeCameraPos { ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS }
}
