package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.vertex.GLVertexBuilder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;

import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class ClassicFastMeshBuilder implements MeshTypeBuilder {
    
    public AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int colorModifier) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);
        
        int range = CloudsConfiguration.getInstance().getCloudGridRange();
        Texture.TextureData tex = state.texture();
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        WrappedCoordinates wrapped = new WrappedCoordinates(cx, cz, range, w, h);

        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                int cellIdx = wrapped.getCellIndex(dx, dz, range);
                long cell = cells[cellIdx];
                int color = Texture.getColor(cell);
                if (cell != 0L) {
                    buildFlatCell(bb, dx, dz, currentLayer, relY, layerConfiguration.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR ? color : colorModifier);
                }
            }
        }

        return bb;
    }

    private static void buildFlatCell(AbstractStaticMesh.Builder<?, ?> bb, int cx, int cz, int currentLayer, float y, int colorModifier) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        GLVertexBuilder.quad(bb, 
            x0, 0, z1,
            x1, 0, z1,
            x1, 0, z0,
            x0, 0, z0,
            colorModifier
        );

        GLVertexBuilder.quad(bb, 
            x0, 0, z0,
            x1, 0, z0,
            x1, 0, z1,
            x0, 0, z1,
            colorModifier
        );
    }
}
