package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder.FaceDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder.FaceMask;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;

public class BeveledMeshBuilder implements MeshTypeBuilder {

    @Override
    public BufferBuilder build(
        BufferBuilder bb,
        LayerState state, 
        int cx, int cz, float relY,
        int currentLayer, 
        int skyColor) {

        int range = CloudsConfiguration.getInstance().CLOUD_GRID_SIZE;
        Texture.TextureData tex = state.texture();
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
                    buildCell(bb, dx, dz, cell, relY, currentLayer, skyColor, x, z, state);
                }
            }
        }

        return bb;
    }
    
    private static void buildCell(BufferBuilder bb,
                            int cx, int cz, long cell, float relY,
                            int currentLayer, int skyColor, int cellIdxX, int cellIdxZ, LayerState state) {

        float cellSize = MeshBuilder.CELL_SIZE_IN_BLOCKS;

        float x0 = cx * cellSize;
        float x1 = x0 + cellSize;
        float z0 = cz * cellSize;
        float z1 = z0 + cellSize;

        float y0 = 0.0f;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration =
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED
                ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE
                : 1.0f);

        boolean n = Texture.isNorthEmpty(cell);
        boolean s = Texture.isSouthEmpty(cell);
        boolean w = Texture.isWestEmpty(cell);
        boolean e = Texture.isEastEmpty(cell);

        FaceMask excluded = new FaceMask();

        if (!w) excluded.add(FaceDir.NEG_X);
        if (!e) excluded.add(FaceDir.POS_X);
        if (!n) excluded.add(FaceDir.NEG_Z);
        if (!s) excluded.add(FaceDir.POS_Z);

        float bevelRadius = layerConfiguration.BEVEL.BEVEL_SIZE;
        int edgeSegments = layerConfiguration.BEVEL.BEVEL_EDGE_SEGMENTS;
        int cornerSegments = layerConfiguration.BEVEL.BEVEL_CORNER_SEGMENTS;

        CubeBuilder.buildBeveledCube(
                bb,
                x0, x1,
                y0, scaledY1,
                z0, z1,
                bevelRadius,
                edgeSegments,
                cornerSegments,
                excluded,
                currentLayer,
                state, relY, cellIdxX, cellIdxZ, skyColor
        );
    }
}
