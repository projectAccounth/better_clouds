package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder.FaceDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.CubeBuilder.FaceMask;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;

public class BeveledMeshBuilder implements MeshTypeBuilder {

    /**
     * Enum for corner positions to check diagonal neighbors.
     */
    public enum CornerPos {
        NEG_X_NEG_Z(0), POS_X_NEG_Z(1), NEG_X_POS_Z(2), POS_X_POS_Z(3);
        
        final int index;
        CornerPos(int index) { this.index = index; }
    }

    /**
     * Pre-cache neighboring cells for faster lookup during corner generation.
     */
    public static class NeighborCache {
        public long[] neighbors = new long[4]; // Only XZ diagonals needed for corners
        
        void cacheNeighbors(int cellX, int cellZ, Texture.TextureData tex) {
            int w = tex.width;
            int h = tex.height;
            
            // NEG_X_NEG_Z
            neighbors[0] = tex.cells[Math.floorMod(cellX - 1, w) + Math.floorMod(cellZ - 1, h) * w];
            // POS_X_NEG_Z
            neighbors[1] = tex.cells[Math.floorMod(cellX + 1, w) + Math.floorMod(cellZ - 1, h) * w];
            // NEG_X_POS_Z
            neighbors[2] = tex.cells[Math.floorMod(cellX - 1, w) + Math.floorMod(cellZ + 1, h) * w];
            // POS_X_POS_Z
            neighbors[3] = tex.cells[Math.floorMod(cellX + 1, w) + Math.floorMod(cellZ + 1, h) * w];
        }
    }

    public AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state, 
        int cx, int cz, float relY,
        int currentLayer, 
        int colorModifier) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        int range = CloudsConfiguration.getInstance().getCloudGridRange();
        Texture.TextureData tex = state.texture();
        long[] cells = tex.cells;
        int w = tex.width;
        int h = tex.height;

        int dxStart = -range;
        int dzStart = -range;

        int dxEnd = range;
        int dzEnd = range;

        int dxStep = 1;
        int dzStep = 1;

        for (int dz = dzStart; dz != dzEnd; dz += dzStep) {
            for (int dx = dxStart; dx != dxEnd; dx += dxStep) {
                int x = Math.floorMod(cx + dx, w);
                int z = Math.floorMod(cz + dz, h);
                long cell = cells[x + z * w];
                int color = Texture.getColor(cell);
                if (cell != 0L) {
                    buildCell(bb, dx, dz, cell, relY, currentLayer, layerConfiguration.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR ? color : colorModifier, x, z, state);
                }
            }
        }

        return bb;
    }
    
    private static void buildCell(AbstractStaticMesh.Builder<?, ?> bb,
                            int cx, int cz, long cell, float relY,
                            int currentLayer, int colorModifier, int cellIdxX, int cellIdxZ, LayerState state) {

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
                state, cellIdxX, cellIdxZ, ARGB.WHITE
        );
    }
}
