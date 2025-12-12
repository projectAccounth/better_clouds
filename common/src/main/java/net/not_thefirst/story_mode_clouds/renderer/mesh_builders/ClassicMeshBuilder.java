package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.Texture;

import net.minecraft.client.CloudStatus;

public class ClassicMeshBuilder implements MeshTypeBuilder {
    public BufferBuilder Build(
        BufferBuilder bb, Texture.TextureData tex, 
        RelativeCameraPos pos, LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor,
        float chunkOffX, float chunkOffZ) {
        
        int range = 32;
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        int dxStart = -range, dxEnd = range, dxStep = 1;
        int dzStart = -range, dzEnd = range, dzStep = 1;

        for (int dz = dzStart; dz != dzEnd; dz += dzStep) {
            for (int dx = dxStart; dx != dxEnd; dx += dxStep) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                if (cell != 0L) {
                    if (state.currentStatus == CloudStatus.FANCY) {
                        buildExtrudedCell(pos, bb, dx, dz, cell, relY, currentLayer, skyColor);
                    } else {
                        buildFlatCell(bb, dx, dz, currentLayer, relY, skyColor);
                    }
                }
            }
        }

        return bb;
    }

    private static void buildFlatCell(BufferBuilder bb, int cx, int cz, int currentLayer, float y, int skyColor) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        int adjustedColor = ColorUtils.recolor(MeshBuilder.topColor, currentLayer, skyColor);

        float colorR = ARGB.redFloat(adjustedColor);
        float colorG = ARGB.greenFloat(adjustedColor);
        float colorB = ARGB.blueFloat(adjustedColor);
        float colorA = ARGB.alphaFloat(adjustedColor);

        bb.vertex(x0, 0, z0).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x1, 0, z0).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x1, 0, z1).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x0, 0, z1).color(colorR, colorG, colorB, colorA).endVertex();

        bb.vertex(x0, 0, z1).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x1, 0, z1).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x1, 0, z0).color(colorR, colorG, colorB, colorA).endVertex();
        bb.vertex(x0, 0, z0).color(colorR, colorG, colorB, colorA).endVertex();
    }

    private static void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int cx, int cz, long cell, float relY, int currentLayer, int skyColor) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED ? layerConfiguration.CLOUD_Y_SCALE : 1.0f);        
        
        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            int color = ColorUtils.recolor(MeshBuilder.topColor, scaledY1, pos, relY, currentLayer, skyColor);
            int colorR = ARGB.red(color);
            int colorG = ARGB.green(color);
            int colorB = ARGB.blue(color);
            int colorA = ARGB.alpha(color);

            bb.vertex(x0, scaledY1, z1).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x1, scaledY1, z1).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x1, scaledY1, z0).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x0, scaledY1, z0).color(colorR, colorG, colorB, colorA).endVertex();
        }
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            int color = ColorUtils.recolor(MeshBuilder.innerColor, y0, pos, relY, currentLayer, skyColor);
            int colorR = ARGB.red(color);
            int colorG = ARGB.green(color);
            int colorB = ARGB.blue(color);
            int colorA = ARGB.alpha(color); 
            
            bb.vertex(x0, y0, z0).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x1, y0, z0).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x1, y0, z1).color(colorR, colorG, colorB, colorA).endVertex();
            bb.vertex(x0, y0, z1).color(colorR, colorG, colorB, colorA).endVertex();
        }
        // Sides
        int colorA = ColorUtils.recolor(MeshBuilder.sideColor, scaledY1, pos, relY, currentLayer, skyColor);
        int colorB = ColorUtils.recolor(MeshBuilder.sideColor, y0, pos, relY, currentLayer, skyColor);

        float colorAR = ARGB.redFloat(colorA);
        float colorAG = ARGB.greenFloat(colorA);
        float colorAB = ARGB.blueFloat(colorA);
        float colorAA = ARGB.alphaFloat(colorA);

        float colorBR = ARGB.redFloat(colorB);
        float colorBG = ARGB.greenFloat(colorB);
        float colorBB = ARGB.blueFloat(colorB);
        float colorBA = ARGB.alphaFloat(colorB);

        // S
        if (Texture.isSouthEmpty(cell)) {
            bb.vertex(x0, y0, z1).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x1, y0, z1).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x1, scaledY1, z1).color(colorAR, colorAG, colorAB, colorAA).endVertex();
            bb.vertex(x0, scaledY1, z1).color(colorAR, colorAG, colorAB, colorAA).endVertex();
        }

        // W
        if (Texture.isWestEmpty(cell)) {
            bb.vertex(x0, y0, z0).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x0, y0, z1).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x0, scaledY1, z1).color(colorAR, colorAG, colorAB, colorAA).endVertex();
            bb.vertex(x0, scaledY1, z0).color(colorAR, colorAG, colorAB, colorAA).endVertex();
        }

        // N
        if (Texture.isNorthEmpty(cell)) {
            bb.vertex(x1, y0, z0).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x0, y0, z0).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x0, scaledY1, z0).color(colorAR, colorAG, colorAB, colorAA).endVertex();
            bb.vertex(x1, scaledY1, z0).color(colorAR, colorAG, colorAB, colorAA).endVertex();
        }

        // E
        if (Texture.isEastEmpty(cell)) {
            bb.vertex(x1, y0, z1).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x1, y0, z0).color(colorBR, colorBG, colorBB, colorBA).endVertex();
            bb.vertex(x1, scaledY1, z0).color(colorAR, colorAG, colorAB, colorAA).endVertex();
            bb.vertex(x1, scaledY1, z1).color(colorAR, colorAG, colorAB, colorAA).endVertex();
        }
    }
}
