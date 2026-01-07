package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.renderer.utils.WrappedCoordinates;
import net.not_thefirst.story_mode_clouds.renderer.utils.VertexBuilder;
import net.not_thefirst.story_mode_clouds.utils.Texture;

public class ClassicMeshBuilder implements MeshTypeBuilder {
    
    public BuildingMesh Build(
        BuildingMesh bb, Texture.TextureData tex, 
        RelativeCameraPos pos, LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor,
        float chunkOffX, float chunkOffZ) {
        
        int range = 32;
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, range, w, h);

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int cellIdx = wrapped.getCellIndex(dx, dz, range);
                long cell = cells[cellIdx];
                if (cell != 0L) {
                    buildExtrudedCell(pos, bb, dx, dz, cell, relY, currentLayer, skyColor);
                }
            }
        }

        return bb;
    }

    private static void buildExtrudedCell(RelativeCameraPos pos, BuildingMesh bb,
                                   int cx, int cz, long cell, float relY, int currentLayer, int skyColor) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE : 1.0f);        
        
        // Top face
        if (pos != RelativeCameraPos.BELOW_CLOUDS) {
            VertexBuilder.quad(bb, 
                x0, scaledY1, z1,
                x1, scaledY1, z1,
                x1, scaledY1, z0,
                x0, scaledY1, z0,
                currentLayer, pos, relY, skyColor
            );
        }
        
        // Bottom face
        if (pos != RelativeCameraPos.ABOVE_CLOUDS) {
            VertexBuilder.quad(bb, 
                x0, y0, z0,
                x1, y0, z0,
                x1, y0, z1,
                x0, y0, z1,
                currentLayer, pos, relY, skyColor
            );
        }

        // S
        if (Texture.isSouthEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x0, y0, z1,
                x1, y0, z1,
                x1, scaledY1, z1,
                x0, scaledY1, z1,
                currentLayer, pos, relY, skyColor
            );
        }

        // W
        if (Texture.isWestEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x0, y0, z0,
                x0, y0, z1,
                x0, scaledY1, z1,
                x0, scaledY1, z0,
                currentLayer, pos, relY, skyColor
            );
        }

        // N
        if (Texture.isNorthEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x1, y0, z0,
                x0, y0, z0,
                x0, scaledY1, z0,
                x1, scaledY1, z0,
                currentLayer, pos, relY, skyColor
            );
        }

        // E
        if (Texture.isEastEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x1, y0, z1,
                x1, y0, z0,
                x1, scaledY1, z0,
                x1, scaledY1, z1,
                currentLayer, pos, relY, skyColor
            );
        }
    }
}
