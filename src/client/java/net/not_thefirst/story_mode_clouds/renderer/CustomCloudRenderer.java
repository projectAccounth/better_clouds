package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class CustomCloudRenderer extends CloudRenderer {
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;
    private LayerState[] layers;

    private int maxLayerCount = CloudsConfiguration.MAX_LAYER_COUNT;

    public CustomCloudRenderer() {
        super();

        this.layers = new LayerState[maxLayerCount];

        for (int i = 0; i < 10; i++) {
            layers[i] = new LayerState(i);

            layers[i].buffer = new VertexBuffer(com.mojang.blaze3d.buffers.BufferUsage.STATIC_WRITE);
            layers[i].bufferEmpty = true;
            layers[i].needsRebuild = true;
            layers[i].prevCellX = Integer.MIN_VALUE;
            layers[i].prevCellZ = Integer.MIN_VALUE;
            layers[i].prevPos = RelativeCameraPos.INSIDE_CLOUDS;
            layers[i].prevStatus = null;
            layers[i].lastFadeRebuildMs = System.currentTimeMillis();
        }

        if (CloudsConfiguration.INSTANCE.CLOUD_RANDOM_LAYERS) {
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                layers[i].offsetX = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
                layers[i].offsetZ = random.nextFloat() * CELL_SIZE_IN_BLOCKS * 64.0f;
            }
        }
    }

    @Override
    protected Optional<TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return super.prepare(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(Optional<TextureData> optional, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureData baseTexture = optional.orElse(super.prepare(resourceManager, profilerFiller).orElse(null));
        for (int i = 0; i < 10; i++) {
            layers[i].texture = resolveTextureForLayer(i, baseTexture);
            layers[i].needsRebuild = true;
        }
    }

    @Nullable
    private TextureData resolveTextureForLayer(int layer, @Nullable TextureData fallback) {
        // Hook for per-layer texture overrides from config
        return fallback;
    }

    private static int getColor(long cell) { return (int)(cell >> 4 & 0xFFFFFFFFL); }
    private static boolean isNorthEmpty(long c) { return (c >> 3 & 1L) != 0L; }
    private static boolean isEastEmpty(long c)  { return (c >> 2 & 1L) != 0L; }
    private static boolean isSouthEmpty(long c) { return (c >> 1 & 1L) != 0L; }
    private static boolean isWestEmpty(long c)  { return (c & 1L) != 0L; }

    // === Rendering ===
    @Override
    public void render(int i, CloudStatus status, float cloudHeight, Matrix4f proj, Matrix4f modelView, Vec3 cam, float tickDelta) {
        int layers = CloudsConfiguration.INSTANCE.CLOUD_LAYERS;
        if (layers <= 0) return;

        double dx = cam.x + tickDelta * 0.03F;
        double dz = cam.z + 3.96F;

        int layerCount = CloudsConfiguration.INSTANCE.CLOUD_LAYERS; // or however many you have
        List<Integer> layerIndices = new ArrayList<>();
        for (int ind = 0; ind < layerCount; ind++) {
            layerIndices.add(ind);
        }

        // compute relative distance for sorting
        layerIndices.sort((a, b) -> {
            float ay = (float)(cloudHeight - cam.y) + a * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float by = (float)(cloudHeight - cam.y) + b * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            // we want farthest drawn first, nearest last
            return Float.compare(Math.abs(by), Math.abs(ay));
        });

        for (int layer : layerIndices) {
            LayerState currentLayer = this.layers[layer];
            TextureData tex = currentLayer.texture;
            if (tex == null) continue;

            double wrapX = tex.width() * CELL_SIZE_IN_BLOCKS;
            double wrapZ = tex.height() * CELL_SIZE_IN_BLOCKS;
            double dxLayer = dx + currentLayer.offsetX;
            double dzLayer = dz + currentLayer.offsetZ;
            dxLayer -= Mth.floor(dxLayer / wrapX) * wrapX;
            dzLayer -= Mth.floor(dzLayer / wrapZ) * wrapZ;

            float cloudChunkHeight = HEIGHT_IN_BLOCKS * 
                (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0F);
            float layerY = (float)(cloudHeight - cam.y) + layer * CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING;
            float relYTop = layerY + cloudChunkHeight;

            RelativeCameraPos layerPos =
                (relYTop < 0.0F) ? RelativeCameraPos.ABOVE_CLOUDS :
                (layerY > 0.0F) ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

            int cellX = Mth.floor(dxLayer / CELL_SIZE_IN_BLOCKS);
            int cellZ = Mth.floor(dzLayer / CELL_SIZE_IN_BLOCKS);
            float offX = (float)(dxLayer - cellX * CELL_SIZE_IN_BLOCKS);
            float offZ = (float)(dzLayer - cellZ * CELL_SIZE_IN_BLOCKS);

            long now = System.currentTimeMillis();

            float relY = (float)(relYTop - cloudChunkHeight / 2.0f);

            if (CloudsConfiguration.INSTANCE.IS_ENABLED &&
                Math.abs(relY) <= CloudsConfiguration.INSTANCE.TRANSITION_RANGE && 
                now - currentLayer.lastFadeRebuildMs > 40) {
                // How should I optimize this?

                currentLayer.needsRebuild = true;
                currentLayer.lastFadeRebuildMs = now;
            }

            boolean isCloudFancy = status == CloudStatus.FANCY;
            RenderType rt = isCloudFancy ? RenderType.clouds() : RenderType.flatClouds();

            if (currentLayer.needsRebuild ||
                cellX != currentLayer.prevCellX || cellZ != currentLayer.prevCellZ ||
                layerPos != currentLayer.prevPos || status != currentLayer.prevStatus) {

                currentLayer.needsRebuild = false;
                currentLayer.prevCellX = cellX;
                currentLayer.prevCellZ = cellZ;
                currentLayer.prevPos = layerPos;
                currentLayer.prevStatus = status;
                
                MeshData mesh = buildMeshForLayer(tex, Tesselator.getInstance(), cellX, cellZ, status, layerPos, rt, relY, layer);
                if (mesh != null) {
                    currentLayer.buffer.bind();
                    currentLayer.buffer.upload(mesh);
                    VertexBuffer.unbind();
                    currentLayer.bufferEmpty = false;
                } else {
                    currentLayer.bufferEmpty = true;
                }
            }

            if (!currentLayer.bufferEmpty) {
                float CUSTOM_BRIGHTNESS = CloudsConfiguration.INSTANCE.BRIGHTNESS;
                
                if (CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS && CloudsConfiguration.INSTANCE.IS_ENABLED)
                    RenderSystem.setShaderColor(CUSTOM_BRIGHTNESS, CUSTOM_BRIGHTNESS, CUSTOM_BRIGHTNESS, 1.0F);
                else 
                    RenderSystem.setShaderColor(ARGB.redFloat(i), ARGB.greenFloat(i), ARGB.blueFloat(i), 1.0F);

                if (!CloudsConfiguration.INSTANCE.FOG_ENABLED)
                    RenderSystem.setShaderFog(new FogParameters(Float.MAX_VALUE, 0.0F, FogShape.SPHERE, 0.0F, 0.0F, 0.0F, 0.0F));

                currentLayer.buffer.bind();

                if (isCloudFancy)
                    drawWithRenderType(RenderType.cloudsDepthOnly(), proj, modelView, offX, layerY, offZ, currentLayer.buffer);

                drawWithRenderType(rt, proj, modelView, offX, layerY, offZ, currentLayer.buffer);
                
                VertexBuffer.unbind();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }
    }

    private void drawWithRenderType(RenderType rt, Matrix4f proj, Matrix4f mv, float ox, float oy, float oz, VertexBuffer buf) {
        rt.setupRenderState();
        var shader = RenderSystem.getShader();
        
        if (shader != null && shader.MODEL_OFFSET != null) {
            shader.MODEL_OFFSET.set(-ox, oy, -oz);
        }
        buf.drawWithShader(proj, mv, shader);
        rt.clearRenderState();
    }

    @Nullable
    private MeshData buildMeshForLayer(TextureData tex, Tesselator tess, int cx, int cz,
                                       CloudStatus status, RelativeCameraPos pos, RenderType rt, float relY, int currentLayer) {
        int top = ARGB.colorFromFloat(0.8F, 1, 1, 1);
        int bottom = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int side = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int inner = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);

        BufferBuilder bb = tess.begin(rt.mode(), rt.format());
        buildMesh(tex, pos, bb, cx, cz, side, top, bottom, inner, status == CloudStatus.FANCY, relY, currentLayer);
        return bb.build();
    }

    private void buildMesh(TextureData tex, RelativeCameraPos pos, BufferBuilder bb,
                           int cx, int cz, int bottom, int top, int side, int inner, boolean fancy, float relY, int currentLayer) {
        int range = 32;
        long[] cells = tex.cells();
        int w = tex.width();
        int h = tex.height();

        boolean isCustom = CloudsConfiguration.INSTANCE.DEBUG_v0;

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                if (cell != 0L) {
                    int color = getColor(cell);
                    if (fancy) {
                        if (!isCustom) buildExtrudedCell(pos, bb,
                            ARGB.multiply(bottom, color),
                            ARGB.multiply(top, color),
                            ARGB.multiply(side, color),
                            ARGB.multiply(inner, color),
                            dx, dz, cell, relY, currentLayer);
                        else buildExtrudedCellFancyScattered(pos, bb,
                            ARGB.multiply(top, color),
                            dx, dz, cell, cells, w, h, relY, currentLayer);
                    } else {
                        buildFlatCell(bb, ARGB.multiply(top, color), dx, dz, currentLayer);
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder bb, int color, int cx, int cz, int currentLayer) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;
        bb.addVertex(x0, 0, z0).setColor(recolor(color, 0, currentLayer));
        bb.addVertex(x0, 0, z1).setColor(recolor(color, 0, currentLayer));
        bb.addVertex(x1, 0, z1).setColor(recolor(color, 0, currentLayer));
        bb.addVertex(x1, 0, z0).setColor(recolor(color, 0, currentLayer));
    }

    private void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int bottomColor, int topColor, int sideColor, int innerColor,
                                   int cx, int cz, long cell, float relY, int currentLayer) {
        float x0 = cx * CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = HEIGHT_IN_BLOCKS;
        float z0 = cz * CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + CELL_SIZE_IN_BLOCKS;

        float scaledY1 = y1 * (CloudsConfiguration.INSTANCE.IS_ENABLED ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE : 1.0f);

        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            int colorTop = recolor(topColor, scaledY1, pos, relY, currentLayer);
            bb.addVertex(x0, scaledY1, z0).setColor(colorTop);
            bb.addVertex(x0, scaledY1, z1).setColor(colorTop);
            bb.addVertex(x1, scaledY1, z1).setColor(colorTop);
            bb.addVertex(x1, scaledY1, z0).setColor(colorTop);
        }
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            int colorBottom = recolor(bottomColor, y0, pos, relY, currentLayer);
            bb.addVertex(x1, y0, z0).setColor(colorBottom);
            bb.addVertex(x1, y0, z1).setColor(colorBottom);
            bb.addVertex(x0, y0, z1).setColor(colorBottom);
            bb.addVertex(x0, y0, z0).setColor(colorBottom);
        }

        int colorSideBottom = recolor(sideColor, y0, pos, relY, currentLayer);
        int colorSideTop    = recolor(sideColor, scaledY1, pos, relY, currentLayer);

        // Sides
        if (isNorthEmpty(cell)) {
            bb.addVertex(x0, y0, z0).setColor(colorSideBottom);
            bb.addVertex(x0, scaledY1, z0).setColor(colorSideTop);
            bb.addVertex(x1, scaledY1, z0).setColor(colorSideTop);
            bb.addVertex(x1, y0, z0).setColor(colorSideBottom);
        }
        if (isSouthEmpty(cell)) {
            bb.addVertex(x1, y0, z1).setColor(colorSideBottom);
            bb.addVertex(x1, scaledY1, z1).setColor(colorSideTop);
            bb.addVertex(x0, scaledY1, z1).setColor(colorSideTop);
            bb.addVertex(x0, y0, z1).setColor(colorSideBottom);
        }
        if (isWestEmpty(cell)) {
            bb.addVertex(x0, y0, z1).setColor(colorSideBottom);
            bb.addVertex(x0, scaledY1, z1).setColor(colorSideTop);
            bb.addVertex(x0, scaledY1, z0).setColor(colorSideTop);
            bb.addVertex(x0, y0, z0).setColor(colorSideBottom);
        }
        if (isEastEmpty(cell)) {
            bb.addVertex(x1, y0, z0).setColor(colorSideBottom);
            bb.addVertex(x1, scaledY1, z0).setColor(colorSideTop);
            bb.addVertex(x1, scaledY1, z1).setColor(colorSideTop);
            bb.addVertex(x1, y0, z1).setColor(colorSideBottom);
        }
    }

    // Full implementation of extruded cloud cells with scattered macro/micro cubes
    private void buildExtrudedCellFancyScattered(RelativeCameraPos pos, BufferBuilder bb,
                                                int color, int cx, int cz, long cell,
                                                long[] cells, int w, int h,
                                                float relY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) return;

        // Parent cell vertical bounds
        final float parentY0 = 0.0f;
        final float parentY1 = HEIGHT_IN_BLOCKS;
        final float scaledParentY1 = parentY1 * (CloudsConfiguration.INSTANCE.IS_ENABLED
                                                ? CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE
                                                : 1.0f);

        // Some tunables (feel free to tweak)
        final int CLUSTER_MIN = 2;        // per-cell clusters
        final int CLUSTER_MAX = 4;
        final float SZ_MIN_FRAC = 0.45f;  // sub-cube XZ size = CELL * (SZ_MIN_FRAC .. SZ_MIN_FRAC+SZ_RANGE)
        final float SZ_RANGE_FRAC = 0.35f;
        final float Y_MIN_FRAC = 0.35f;   // sub-cube Y size fraction of vertical range
        final float Y_RANGE_FRAC = 0.5f;
        final float CENTER_PULL = 0.25f;  // neighbor pull into cell center
        final float EDGE_SHRINK = 0.85f;  // shrink height when cell has many empty neighbors

        // neighbor influence (same logic you've used)
        float neighborX = 0f, neighborZ = 0f;
        int neighborCount = 0;
        if (!isNorthEmpty(cell)) { neighborZ -= 0.2f; neighborCount++; }
        if (!isSouthEmpty(cell)) { neighborZ += 0.2f; neighborCount++; }
        if (!isWestEmpty(cell))  { neighborX -= 0.2f; neighborCount++; }
        if (!isEastEmpty(cell))  { neighborX += 0.2f; neighborCount++; }
        if (neighborCount > 0) {
            neighborX /= neighborCount;
            neighborZ /= neighborCount;
        }

        // Determine number of clusters deterministically per cell
        float selector = (deterministicOffset(cx, cz, 7777, 1.0f) + 1f) * 0.5f; // [0..1]
        int clusterCount = CLUSTER_MIN + (int)(selector * (CLUSTER_MAX - CLUSTER_MIN + 1));
        clusterCount = Math.max(CLUSTER_MIN, Math.min(CLUSTER_MAX, clusterCount));

        float cellSize = CELL_SIZE_IN_BLOCKS;
        float verticalRange = scaledParentY1 - parentY0;
        float cellCenterX = cx * cellSize + cellSize * 0.5f;
        float cellCenterZ = cz * cellSize + cellSize * 0.5f;

        for (int ci = 0; ci < clusterCount; ci++) {
            // deterministic-ish random in [0..1] per cluster
            float rnd = (deterministicOffset(cx, cz, 8000 + ci, 1.0f) + 1f) * 0.5f;

            // sub-cube dimensions
            float sizeXZ = cellSize * (SZ_MIN_FRAC + rnd * SZ_RANGE_FRAC);            // ~0.45..0.8 * cell
            float sizeY  = verticalRange * (Y_MIN_FRAC + rnd * Y_RANGE_FRAC);        // fraction of parent vertical span

            // horizontal offset range inside parent cell so cube stays inside bounds
            float maxOffsetXZ = Math.max(0f, (cellSize - sizeXZ) * 0.5f);
            float offsetX = deterministicOffset(cx, cz, 9000 + ci, maxOffsetXZ) + neighborX * cellSize * CENTER_PULL;
            float offsetZ = deterministicOffset(cz, cx, 9100 + ci, maxOffsetXZ) + neighborZ * cellSize * CENTER_PULL;

            float centerX = cellCenterX + offsetX;
            float centerZ = cellCenterZ + offsetZ;

            // vertical placement — **only upwards** from parentY0, clamp inside parent vertical band
            float minCenterY = parentY0 + sizeY * 0.5f;
            float maxCenterY = scaledParentY1 - sizeY * 0.5f;
            float vOffsetRange = Math.max(0f, (maxCenterY - minCenterY) * 0.5f);
            float vDet = deterministicOffset(cx + cz, 9200 + ci, 1, vOffsetRange);
            float centerY = minCenterY + (rnd * 0.9f) * (maxCenterY - minCenterY) + vDet;
            centerY = Math.max(minCenterY, Math.min(maxCenterY, centerY));

            // extents
            float x0 = centerX - sizeXZ * 0.5f;
            float x1 = centerX + sizeXZ * 0.5f;
            float z0 = centerZ - sizeXZ * 0.5f;
            float z1 = centerZ + sizeXZ * 0.5f;
            float y0 = centerY - sizeY * 0.5f;
            float y1 = centerY + sizeY * 0.5f;

            // clamp vertically to strict parent band (safety)
            if (y0 < parentY0) y0 = parentY0;
            if (y1 > scaledParentY1) y1 = scaledParentY1;

            // optionally shrink heights for exposed cells (soft taper at edges)
            int emptySides = 0;
            if (isNorthEmpty(cell)) emptySides++;
            if (isSouthEmpty(cell)) emptySides++;
            if (isWestEmpty(cell))  emptySides++;
            if (isEastEmpty(cell))  emptySides++;

            if (emptySides >= 2) {
                float h0 = y1 - y0;
                float newH = Math.max(0.001f, h0 * EDGE_SHRINK);
                y1 = y0 + newH;
            }

            // color per-subcube (vary alpha a bit with rnd)
            float alphaScale = 0.32f + rnd * 0.28f; // ~0.32..0.6
            int subColor = scaleAlpha(color, alphaScale);

            // Top face
            if (pos != RelativeCameraPos.BELOW_CLOUDS) {
                int cTop = recolor(subColor, y1, pos, relY, currentLayer);
                bb.addVertex(x0, y1, z0).setColor(cTop);
                bb.addVertex(x0, y1, z1).setColor(cTop);
                bb.addVertex(x1, y1, z1).setColor(cTop);
                bb.addVertex(x1, y1, z0).setColor(cTop);
            }

            // Bottom face
            if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
                int cBot = recolor(subColor, y0, pos, relY, currentLayer);
                bb.addVertex(x1, y0, z0).setColor(cBot);
                bb.addVertex(x1, y0, z1).setColor(cBot);
                bb.addVertex(x0, y0, z1).setColor(cBot);
                bb.addVertex(x0, y0, z0).setColor(cBot);
            }

            // Side colors
            int cSideBottom = recolor(subColor, y0, pos, relY, currentLayer);
            int cSideTop    = recolor(subColor, y1, pos, relY, currentLayer);

            // Render full sides for these chunky sub-cubes (keeps them visible when overlapping neighbors)
            // North
            bb.addVertex(x0, y0, z0).setColor(cSideBottom);
            bb.addVertex(x0, y1, z0).setColor(cSideTop);
            bb.addVertex(x1, y1, z0).setColor(cSideTop);
            bb.addVertex(x1, y0, z0).setColor(cSideBottom);

            // South
            bb.addVertex(x1, y0, z1).setColor(cSideBottom);
            bb.addVertex(x1, y1, z1).setColor(cSideTop);
            bb.addVertex(x0, y1, z1).setColor(cSideTop);
            bb.addVertex(x0, y0, z1).setColor(cSideBottom);

            // West
            bb.addVertex(x0, y0, z1).setColor(cSideBottom);
            bb.addVertex(x0, y1, z1).setColor(cSideTop);
            bb.addVertex(x0, y1, z0).setColor(cSideTop);
            bb.addVertex(x0, y0, z0).setColor(cSideBottom);

            // East
            bb.addVertex(x1, y0, z0).setColor(cSideBottom);
            bb.addVertex(x1, y1, z0).setColor(cSideTop);
            bb.addVertex(x1, y1, z1).setColor(cSideTop);
            bb.addVertex(x1, y0, z1).setColor(cSideBottom);
        }
    }

    // === Helpers ===

    // Deterministic offset: stable per (x,z,seed), avoids RNG desync
    private float deterministicOffset(int x, int z, int seed, float scale) {
        long h = (long)x * 73428767L ^ (long)z * 91293199L ^ (long)seed * 1234567L;
        h = (h ^ (h >> 13)) * 1274126177L;

        float v = ((h & 0x7FFFFFFFL) / (float) Integer.MAX_VALUE); // [0..1]
        return (v * 2f - 1f) * scale; // [-scale..scale]
    }

    // Scale only alpha
    private int scaleAlpha(int color, float alphaScale) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        a = Math.min(255, Math.max(0, (int) (a * alphaScale)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    private int recolor(int color, float vertexY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // Apply custom tinting
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        }

        return ARGB.colorFromFloat(baseAlpha, r, g, b);
    }

    private int recolor(int color, float vertexY, RelativeCameraPos pos, float relY, int currentLayer) {
        if (!CloudsConfiguration.INSTANCE.IS_ENABLED) {
            return color;
        }

        boolean shaded   = CloudsConfiguration.INSTANCE.APPEARS_SHADED;
        boolean useAlpha = CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA;
        boolean useColor = CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? CloudsConfiguration.INSTANCE.BASE_ALPHA : ARGB.alphaFloat(color);
        float fadeAlpha  = CloudsConfiguration.INSTANCE.FADE_ALPHA;
        int customColor  = CloudsConfiguration.INSTANCE.CLOUD_COLORS[currentLayer];

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // === Tint logic ===
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        } else if (!shaded) {
            r = g = b = 1.0f;
        }

        if (!CloudsConfiguration.INSTANCE.FADE_ENABLED) {
            return ARGB.colorFromFloat(baseAlpha, r, g, b);
        }

        float cloudHeight = HEIGHT_IN_BLOCKS * CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE;
        float normalizedY = Mth.clamp(vertexY / cloudHeight, 0.0f, 1.0f);

        float transitionRange = CloudsConfiguration.INSTANCE.TRANSITION_RANGE;
        float dir = Mth.clamp(relY / transitionRange, -1.0f, 1.0f);

        float fadeBelow = Mth.lerp(normalizedY, 1.0f, fadeAlpha);
        float fadeAbove = Mth.lerp(1.0f - normalizedY, 1.0f, fadeAlpha);

        float mix = (dir + 1.0f) / 2.0f;
        float fade = Mth.lerp(mix, fadeBelow, fadeAbove);

        float finalAlpha = baseAlpha * (1.0f - fade);
        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }

    public void markForRebuild(int layer) {
        if (layer >= 0 && layer < layers.length) {
            layers[layer].needsRebuild = true;
        }
    }

    public void markForRebuild() {
        for (int i = 0; i < layers.length; i++) {
            layers[i].needsRebuild = true;
        }
    }

    @Override
    public void close() {
        for (LayerState layer : layers) {
            if (layer.buffer != null) layer.buffer.close();
        }
    }

    @SuppressWarnings("unused")
    private class LayerState {
        public int index;
        public float offsetX, offsetZ;
        public TextureData texture;
        public VertexBuffer buffer;
        public int indexCount;
        public boolean needsRebuild;
        public int prevCellX, prevCellZ;
        public RelativeCameraPos prevPos;
        public CloudStatus prevStatus;
        public long lastFadeRebuildMs;
        public float prevFadeMix;
        public boolean bufferEmpty;

        public LayerState(int index) {
            this.index = index;
        }
    }

    public enum RelativeCameraPos {
        ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS
    }

    public enum CloudMeshMode {
        CLASSIC,   // current cuboid clouds
        PUFFY      // new detailed style
    }
}
