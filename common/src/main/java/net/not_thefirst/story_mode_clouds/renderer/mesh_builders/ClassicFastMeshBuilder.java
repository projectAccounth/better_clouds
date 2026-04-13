package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexBuilder;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class ClassicFastMeshBuilder implements MeshTypeBuilder {
    
    public BuildingMesh build(
        BuildingMesh bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor) {
        
        int range = CloudsConfiguration.getInstance().CLOUD_GRID_SIZE;
        Texture.TextureData tex = state.texture();
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, range, w, h);

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int cellIdx = wrapped.getCellIndex(dx, dz, range);
                long cell = cells[cellIdx];
                if (cell != 0L) {
                    buildFlatCell(bb, dx, dz, currentLayer, relY, skyColor);
                }
            }
        }

        return bb;
    }

    private static void buildFlatCell(BuildingMesh bb, int cx, int cz, int currentLayer, float y, int skyColor) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        VertexBuilder.quad(bb, 
            x0, 0, z1,
            x1, 0, z1,
            x1, 0, z0,
            x0, 0, z0,
            currentLayer, y, skyColor
        );

        VertexBuilder.quad(bb, 
            x0, 0, z0,
            x1, 0, z0,
            x1, 0, z1,
            x0, 0, z1,
            currentLayer, y, skyColor
        );
    }
}
