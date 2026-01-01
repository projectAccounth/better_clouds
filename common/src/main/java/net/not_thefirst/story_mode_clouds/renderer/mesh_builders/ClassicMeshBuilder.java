package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.utils.WrappedCoordinates;
import net.not_thefirst.story_mode_clouds.renderer.utils.ColorBatch;
import net.not_thefirst.story_mode_clouds.renderer.utils.VertexBuilder;
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

        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, range, w, h);
        ColorBatch colorBatch = new ColorBatch(3);

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int cellIdx = wrapped.getCellIndex(dx, dz, range);
                long cell = cells[cellIdx];
                if (cell != 0L) {
                    if (state.currentStatus == CloudStatus.FANCY) {
                        buildExtrudedCell(pos, bb, dx, dz, cell, relY, currentLayer, skyColor, colorBatch);
                    } else {
                        buildFlatCell(bb, dx, dz, currentLayer, relY, skyColor, colorBatch);
                    }
                }
            }
        }

        return bb;
    }

    private static void buildFlatCell(BufferBuilder bb, int cx, int cz, int currentLayer, float y, int skyColor, ColorBatch colorBatch) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        int adjustedColor = ColorUtils.recolor(MeshBuilder.topColor, currentLayer, skyColor);

        colorBatch.reset();
        colorBatch.add(adjustedColor);

        float colorR = colorBatch.getR(0);
        float colorG = colorBatch.getG(0);
        float colorB = colorBatch.getB(0);
        float colorA = colorBatch.getA(0);

        bb.addVertex(x0, 0, z0).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x1, 0, z0).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x1, 0, z1).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x0, 0, z1).setColor(colorR, colorG, colorB, colorA);

        bb.addVertex(x0, 0, z1).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x1, 0, z1).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x1, 0, z0).setColor(colorR, colorG, colorB, colorA);
        bb.addVertex(x0, 0, z0).setColor(colorR, colorG, colorB, colorA);
    }

    private static void buildExtrudedCell(RelativeCameraPos pos, BufferBuilder bb,
                                   int cx, int cz, long cell, float relY, int currentLayer, int skyColor, ColorBatch colorBatch) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE : 1.0f);        
        
        int topColor = ColorUtils.recolor(MeshBuilder.topColor, scaledY1, pos, relY, currentLayer, skyColor);
        int bottomColor = ColorUtils.recolor(MeshBuilder.innerColor, y0, pos, relY, currentLayer, skyColor);
        int sideColor = ColorUtils.recolor(MeshBuilder.sideColor, scaledY1, pos, relY, currentLayer, skyColor);
        int sideLowColor = ColorUtils.recolor(MeshBuilder.sideColor, y0, pos, relY, currentLayer, skyColor);
        
        colorBatch.reset();
        colorBatch.add(topColor);
        colorBatch.add(bottomColor);
        colorBatch.add(sideColor);
        
        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            float colorR = colorBatch.getR(0);
            float colorG = colorBatch.getG(0);
            float colorB = colorBatch.getB(0);
            float colorA = colorBatch.getA(0);

            VertexBuilder.quad(bb, 
                x0, scaledY1, z1,
                x1, scaledY1, z1,
                x1, scaledY1, z0,
                x0, scaledY1, z0,
                colorR, colorG, colorB, colorA
            );
        }
        
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            float colorR = colorBatch.getR(1);
            float colorG = colorBatch.getG(1);
            float colorB = colorBatch.getB(1);
            float colorA = colorBatch.getA(1);
            
            bb.addVertex(x0, y0, z0).setColor(colorR, colorG, colorB, colorA);
            bb.addVertex(x1, y0, z0).setColor(colorR, colorG, colorB, colorA);
            bb.addVertex(x1, y0, z1).setColor(colorR, colorG, colorB, colorA);
            bb.addVertex(x0, y0, z1).setColor(colorR, colorG, colorB, colorA);
        }
        
        // Sides
        float colorAR = colorBatch.getR(2);
        float colorAG = colorBatch.getG(2);
        float colorAB = colorBatch.getB(2);
        float colorAA = colorBatch.getA(2);

        float colorBR = ARGB.redFloat(sideLowColor);
        float colorBG = ARGB.greenFloat(sideLowColor);
        float colorBB = ARGB.blueFloat(sideLowColor);
        float colorBA = ARGB.alphaFloat(sideLowColor);

        // S
        if (Texture.isSouthEmpty(cell)) {
            bb.addVertex(x0, y0, z1).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x1, y0, z1).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x1, scaledY1, z1).setColor(colorAR, colorAG, colorAB, colorAA);
            bb.addVertex(x0, scaledY1, z1).setColor(colorAR, colorAG, colorAB, colorAA);
        }

        // W
        if (Texture.isWestEmpty(cell)) {
            bb.addVertex(x0, y0, z0).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x0, y0, z1).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x0, scaledY1, z1).setColor(colorAR, colorAG, colorAB, colorAA);
            bb.addVertex(x0, scaledY1, z0).setColor(colorAR, colorAG, colorAB, colorAA);
        }

        // N
        if (Texture.isNorthEmpty(cell)) {
            bb.addVertex(x1, y0, z0).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x0, y0, z0).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x0, scaledY1, z0).setColor(colorAR, colorAG, colorAB, colorAA);
            bb.addVertex(x1, scaledY1, z0).setColor(colorAR, colorAG, colorAB, colorAA);
        }

        // E
        if (Texture.isEastEmpty(cell)) {
            bb.addVertex(x1, y0, z1).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x1, y0, z0).setColor(colorBR, colorBG, colorBB, colorBA);
            bb.addVertex(x1, scaledY1, z0).setColor(colorAR, colorAG, colorAB, colorAA);
            bb.addVertex(x1, scaledY1, z1).setColor(colorAR, colorAG, colorAB, colorAA);
        }
    }
}
