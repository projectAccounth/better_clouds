package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.Minecraft;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder.FaceDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.CubeBuilder.FaceMask;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
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
                    buildCell(pos, bb, dx, dz, cell, relY, currentLayer, skyColor, x + z * w, state);
                }
            }
        }

        return bb;
    }
    
    private static void buildCell(RelativeCameraPos pos, BufferBuilder bb,
                            int cx, int cz, long cell, float relY,
                            int currentLayer, int skyColor, int cellIdx, LayerState state) {

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

        if (pos == RelativeCameraPos.BELOW_CLOUDS) {
            // excluded.addMask(FaceDir.POS_Y);
        }

        if (pos == RelativeCameraPos.ABOVE_CLOUDS) {
            // excluded.addMask(FaceDir.NEG_Y);
        }

        float bevelRadius = layerConfiguration.BEVEL.BEVEL_SIZE;
        int edgeSegments = layerConfiguration.BEVEL.BEVEL_EDGE_SEGMENTS;
        int cornerSegments = layerConfiguration.BEVEL.BEVEL_CORNER_SEGMENTS;

        var client = Minecraft.getInstance(); 
        var cam = client.getCameraEntity().getPosition(1.0f);

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
                (float) cam.x, (float) cam.y, (float) cam.z,
                state, pos, relY, cellIdx, skyColor
        );
    }
}
