package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexBuilder;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class ClassicMeshBuilder implements MeshTypeBuilder {
    
    public BufferBuilder build(
        BufferBuilder bb,
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
                    buildExtrudedCell(bb, dx, dz, cell, relY, currentLayer, skyColor);
                }
            }
        }

        return bb;
    }

    private static void buildExtrudedCell(BufferBuilder bb,
                                   int cx, int cz, long cell, float relY, int currentLayer, int skyColor) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        float scaledY1 = y1 * (layerConfiguration.IS_ENABLED ? layerConfiguration.APPEARANCE.CLOUD_Y_SCALE : 1.0f);        
        
        // Top face
        VertexBuilder.quad(bb, 
            x0, scaledY1, z1,
            x1, scaledY1, z1,
            x1, scaledY1, z0,
            x0, scaledY1, z0,
            currentLayer, relY, skyColor
        );
        
        // Bottom face
        VertexBuilder.quad(bb, 
            x0, y0, z0,
            x1, y0, z0,
            x1, y0, z1,
            x0, y0, z1,
            currentLayer, relY, skyColor
        );

        // S
        if (Texture.isSouthEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x0, y0, z1,
                x1, y0, z1,
                x1, scaledY1, z1,
                x0, scaledY1, z1,
                currentLayer, relY, skyColor
            );
        }

        // W
        if (Texture.isWestEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x0, y0, z0,
                x0, y0, z1,
                x0, scaledY1, z1,
                x0, scaledY1, z0,
                currentLayer, relY, skyColor
            );
        }

        // N
        if (Texture.isNorthEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x1, y0, z0,
                x0, y0, z0,
                x0, scaledY1, z0,
                x1, scaledY1, z0,
                currentLayer, relY, skyColor
            );
        }

        // E
        if (Texture.isEastEmpty(cell)) {
            VertexBuilder.quad(bb, 
                x1, y0, z1,
                x1, y0, z0,
                x1, scaledY1, z0,
                x1, scaledY1, z1,
                currentLayer, relY, skyColor
            );
        }
    }
}
