package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.utils.BevelWrappers;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder.FaceDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder.FaceMask;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.ColorUtils;
import net.not_thefirst.story_mode_clouds.utils.Texture;
import net.not_thefirst.story_mode_clouds.utils.Texture.TextureData;

public class BeveledMeshBuilder implements MeshTypeBuilder {

    @Override
    public BufferBuilder Build(
        BufferBuilder bb, 
        TextureData tex, 
        RelativeCameraPos pos, 
        LayerState state, 
        int cx, int cz, float relY,
        int currentLayer, 
        int skyColor, 
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
                    buildCell(pos, bb, dx, dz, cell, relY, currentLayer, skyColor);
                }
            }
        }

        return bb;
    }
    
    private static void buildCell(RelativeCameraPos pos, BufferBuilder bb,
                            int cx, int cz, long cell, float relY,
                            int currentLayer, int skyColor) {

        float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;

        float x0 = cx * cellSize;
        float x1 = x0 + cellSize;
        float z0 = cz * cellSize;
        float z1 = z0 + cellSize;

        float y0 = 0.0f;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration =
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED
                ? layerConfiguration.CLOUD_Y_SCALE
                : 1.0f);

        boolean n = Texture.isNorthEmpty(cell);
        boolean s = Texture.isSouthEmpty(cell);
        boolean w = Texture.isWestEmpty(cell);
        boolean e = Texture.isEastEmpty(cell);

        FaceMask excluded = new FaceMask();

        if (!w) excluded.addMask(FaceDir.NEG_X);
        if (!e) excluded.addMask(FaceDir.POS_X);
        if (!n) excluded.addMask(FaceDir.NEG_Z);
        if (!s) excluded.addMask(FaceDir.POS_Z);

        if (pos == RelativeCameraPos.BELOW_CLOUDS) {
            // excluded.addMask(FaceDir.POS_Y);
        }

        if (pos == RelativeCameraPos.ABOVE_CLOUDS) {
            // excluded.addMask(FaceDir.NEG_Y);
        }

        int sideTopColor = ColorUtils.recolor(
                MeshBuilder.sideColor, y0,
                pos, relY, currentLayer, skyColor
        );

        float r = ARGB.redFloat(sideTopColor);
        float g = ARGB.greenFloat(sideTopColor);
        float b = ARGB.blueFloat(sideTopColor);
        float a = ARGB.alphaFloat(sideTopColor);

        float bevelRadius = cellSize * 0.2f;
        int edgeSegments = 8;
        int cornerSegments = 8;

        CubeBuilder.buildBeveledCube(
                bb,
                x0, x1,
                y0, scaledY1,
                z0, z1,
                bevelRadius,
                edgeSegments,
                cornerSegments,
                excluded,
                r, g, b, a
        );
    }
}
