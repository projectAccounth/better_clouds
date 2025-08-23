package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;

    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private RelativeCameraPos prevRelativeCameraPos = RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevType;
    @Nullable
    private TextureData texture;
    private final VertexBuffer vertexBuffer = new VertexBuffer(com.mojang.blaze3d.buffers.BufferUsage.STATIC_WRITE);
    private boolean vertexBufferEmpty;

    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        this.texture = optional.orElse(null);
        this.needsRebuild = true;
    }

    private static int getColor(long cell) { return (int) (cell >> 4 & 0xFFFFFFFFL); }
    private static boolean isNorthEmpty(long c) { return (c >> 3 & 1L) != 0L; }
    private static boolean isEastEmpty(long c)  { return (c >> 2 & 1L) != 0L; }
    private static boolean isSouthEmpty(long c) { return (c >> 1 & 1L) != 0L; }
    private static boolean isWestEmpty(long c)  { return (c & 1L) != 0L; }

    private float insideCloudFadeFactor = 0.0F; // unused
    // === Rendering ===
    public void render(int i, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta) {
        if (this.texture == null) return;

        float relY = (float) (cloudHeight - cam.y);
        float relYTop = relY + HEIGHT_IN_BLOCKS * (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0F);

        RelativeCameraPos pos;
        if (relYTop < 0.0F) pos = RelativeCameraPos.ABOVE_CLOUDS;
        else if (relY > 0.0F) pos = RelativeCameraPos.BELOW_CLOUDS;
        else pos = RelativeCameraPos.INSIDE_CLOUDS;

        if (pos == RelativeCameraPos.INSIDE_CLOUDS) {
            float normalizedY = 1 - Mth.clamp(Math.abs(relYTop / HEIGHT_IN_BLOCKS * CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE), 0.0F, 1.0F);
            insideCloudFadeFactor = normalizedY;
        } else {
            insideCloudFadeFactor = 0.0F;
        }

        double dx = cam.x + tickDelta * 0.03F;
        double dz = cam.z + 3.96F;

        double wrapX = this.texture.width() * CELL_SIZE_IN_BLOCKS;
        double wrapZ = this.texture.height() * CELL_SIZE_IN_BLOCKS;
        dx -= Mth.floor(dx / wrapX) * wrapX;
        dz -= Mth.floor(dz / wrapZ) * wrapZ;

        int cellX = Mth.floor(dx / CELL_SIZE_IN_BLOCKS);
        int cellZ = Mth.floor(dz / CELL_SIZE_IN_BLOCKS);
        float offX = (float) (dx - cellX * CELL_SIZE_IN_BLOCKS);
        float offZ = (float) (dz - cellZ * CELL_SIZE_IN_BLOCKS);

        RenderType renderType = status == CloudStatus.FANCY ? RenderType.clouds() : RenderType.flatClouds();
        this.vertexBuffer.bind();

        if (this.needsRebuild || cellX != prevCellX || cellZ != prevCellZ || pos != prevRelativeCameraPos || status != prevType) {
            this.needsRebuild = false;
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevRelativeCameraPos = pos;
            this.prevType = status;

            MeshData meshData = this.buildMesh(Tesselator.getInstance(), cellX, cellZ, status, pos, renderType);
            if (meshData != null) {
                this.vertexBuffer.upload(meshData);
                this.vertexBufferEmpty = false;
            } else {
                this.vertexBufferEmpty = true;
            }
        }

        if (!this.vertexBufferEmpty) {
            if (CloudsConfiguration.INSTANCE.FULLBRIGHT && CloudsConfiguration.INSTANCE.IS_ENABLED)
                RenderSystem.setShaderColor(1.0F, 1.0F,1.0F, 1.0F);
            else
                RenderSystem.setShaderColor(ARGB.redFloat(i), ARGB.greenFloat(i), ARGB.blueFloat(i), 1.0F);
            if (status == CloudStatus.FANCY) {
                this.drawWithRenderType(RenderType.cloudsDepthOnly(), proj, modelView, offX, relY, offZ);
            }
            this.drawWithRenderType(renderType, proj, modelView, offX, relY, offZ);
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private void drawWithRenderType(RenderType rt, Matrix4f proj, Matrix4f mv, float ox, float oy, float oz) {
        rt.setupRenderState();
        var shader = RenderSystem.getShader();
        if (shader != null && shader.MODEL_OFFSET != null) {
            shader.MODEL_OFFSET.set(-ox, oy, -oz);
        }
        this.vertexBuffer.drawWithShader(proj, mv, shader);
        rt.clearRenderState();
    }

    @Nullable
    private MeshData buildMesh(Tesselator tesselator, int cx, int cz, CloudStatus status, RelativeCameraPos pos, RenderType rt) {
        int top = ARGB.colorFromFloat(0.8F, 1, 1, 1);
        int bottom = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int side = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int inner = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);

        BufferBuilder bb = tesselator.begin(rt.mode(), rt.format());
        this.buildMesh(pos, bb, cx, cz, side, top, bottom, inner, status == CloudStatus.FANCY);
        return bb.build();
    }

    private void buildMesh(RelativeCameraPos pos, BufferBuilder bb, int cx, int cz, int bottom, int top, int side, int inner, boolean fancy) {
        if (this.texture == null) return;

        int range = 32;
        long[] cells = this.texture.cells();
        int w = this.texture.width();
        int h = this.texture.height();

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                if (cell != 0L) {
                    int color = getColor(cell);
                    if (fancy) {
                        this.buildExtrudedCell(pos, bb,
                            ARGB.multiply(bottom, color),
                            ARGB.multiply(top, color),
                            ARGB.multiply(side, color),
                            ARGB.multiply(inner, color),
                            dx, dz, cell);
                    } else {
                        this.buildFlatCell(bb, ARGB.multiply(top, color), dx, dz);
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder bb, int color, int cx, int cz) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;
        bb.addVertex(x0, 0, z0).setColor(color);
        bb.addVertex(x0, 0, z1).setColor(color);
        bb.addVertex(x1, 0, z1).setColor(color);
        bb.addVertex(x1, 0, z0).setColor(color);
    }

    private void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                               int bottomColor, int topColor, int sideColor, int innerColor,
                               int cx, int cz, long cell) {

        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = HEIGHT_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;

        float scaledY1 = y1 * (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f); 

        // Top face (skip if below clouds)
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(topColor, scaledY1, pos));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(topColor, scaledY1, pos));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(topColor, scaledY1, pos));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(topColor, scaledY1, pos));
        }

        // Bottom face (skip if above clouds)
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            bb.addVertex(x1, y0, z0).setColor(recolor(bottomColor, y0, pos));
            bb.addVertex(x1, y0, z1).setColor(recolor(bottomColor, y0, pos));
            bb.addVertex(x0, y0, z1).setColor(recolor(bottomColor, y0, pos));
            bb.addVertex(x0, y0, z0).setColor(recolor(bottomColor, y0, pos));
        }

        // Side faces, only if neighbor empty
        if (isNorthEmpty(cell)) {
            bb.addVertex(x0, y0, z0).setColor(recolor(sideColor, y0, pos));
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x1, y0, z0).setColor(recolor(sideColor, y0, pos));
        }
        if (isSouthEmpty(cell)) {
            bb.addVertex(x1, y0, z1).setColor(recolor(sideColor, y0, pos));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x0, y0, z1).setColor(recolor(sideColor, y0, pos));
        }
        if (isWestEmpty(cell)) {
            bb.addVertex(x0, y0, z1).setColor(recolor(sideColor, y0, pos));
            bb.addVertex(x0, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x0, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x0, y0, z0).setColor(recolor(sideColor, y0, pos));
        }
        if (isEastEmpty(cell)) {
            bb.addVertex(x1, y0, z0).setColor(recolor(sideColor, y0, pos));
            bb.addVertex(x1, scaledY1, z0).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x1, scaledY1, z1).setColor(recolor(sideColor, scaledY1, pos));
            bb.addVertex(x1, y0, z1).setColor(recolor(sideColor, y0, pos));
        }
    }

    private int recolor(int color, float vertexY, RelativeCameraPos pos) {
        return recolor(color, vertexY, pos, 1.0f);
    }

    private int recolor(int color, float vertexY, RelativeCameraPos pos, float fadeMultiplier) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean APPEARS_SHADED    = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean USES_CUSTOM_ALPHA = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;

        float BASE_ALPHA = CloudsConfiguration.INSTANCE.BASE_ALPHA;
        float BRIGHTNESS = CloudsConfiguration.INSTANCE.BRIGHTNESS;
        
        float FADE_RANGE = CloudsConfiguration.INSTANCE.FADE_RANGE;

        float r = APPEARS_SHADED ? ARGB.redFloat(color) : BRIGHTNESS;
        float g = APPEARS_SHADED ? ARGB.greenFloat(color) : BRIGHTNESS;
        float b = APPEARS_SHADED ? ARGB.blueFloat(color) : BRIGHTNESS;
        float a = USES_CUSTOM_ALPHA ? BASE_ALPHA : ARGB.alphaFloat(color);

        float fade = (pos == RelativeCameraPos.ABOVE_CLOUDS) ? 1.0F : Mth.clamp(1.0F - (Math.abs(vertexY) / FADE_RANGE), 0.0F, 1.0F);

        fade *= fadeMultiplier;
        return ARGB.colorFromFloat(Math.clamp(a * (fade + insideCloudFadeFactor), 0, a) , r, g, b);
    }


    public void markForRebuild() { this.needsRebuild = true; }
    @Override public void close() { this.vertexBuffer.close(); }

    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }

}
