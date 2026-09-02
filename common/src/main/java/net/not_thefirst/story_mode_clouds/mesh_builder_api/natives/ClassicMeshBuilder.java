package net.not_thefirst.story_mode_clouds.mesh_builder_api.natives;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh.Builder;
import net.not_thefirst.lib.gl_render_system.vertex.GLVertexBuilder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class ClassicMeshBuilder implements MeshTypeBuilder {
    
    public AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz,  
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
                    buildExtrudedCell(bb, dx, dz, cell, currentLayer, layerConfiguration.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR ? color : colorModifier);
                }
            }
        }

        return bb;
    }

    @Override
    public Builder<?, ?> buildOutline(Builder<?, ?> bb, LayerState state, int cx, int cz,  int currentLayer,
            int colorModifier) {
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
                    
                    buildOutlineCell(bb, dx, dz, cell, currentLayer, 
                        layerConfiguration.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR ? color : colorModifier,
                        cells,
                        wrapped,
                        range);
                }
            }
        }

        return bb;
    }

    private static void buildExtrudedCell(AbstractStaticMesh.Builder<?, ?> bb,
                                   int cx, int cz, long cell,  int currentLayer, int colorModifier) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        float scaledY1 = y1 * layerConfiguration.APPEARANCE.CLOUD_Y_SCALE;      
        
        // Top face
        GLVertexBuilder.quad(bb, 
            x0, scaledY1, z1,
            x1, scaledY1, z1,
            x1, scaledY1, z0,
            x0, scaledY1, z0,
            colorModifier
        );
        
        // Bottom face
        GLVertexBuilder.quad(bb, 
            x0, y0, z0,
            x1, y0, z0,
            x1, y0, z1,
            x0, y0, z1,
            colorModifier
        );

        // S
        if (Texture.isSouthEmpty(cell)) {
            GLVertexBuilder.quad(bb, 
                x0, y0, z1,
                x1, y0, z1,
                x1, scaledY1, z1,
                x0, scaledY1, z1,
                colorModifier
            );
        }

        // W
        if (Texture.isWestEmpty(cell)) {
            GLVertexBuilder.quad(bb, 
                x0, y0, z0,
                x0, y0, z1,
                x0, scaledY1, z1,
                x0, scaledY1, z0,
                colorModifier
            );
        }

        // N
        if (Texture.isNorthEmpty(cell)) {
            GLVertexBuilder.quad(bb, 
                x1, y0, z0,
                x0, y0, z0,
                x0, scaledY1, z0,
                x1, scaledY1, z0,
                colorModifier
            );
        }

        // E
        if (Texture.isEastEmpty(cell)) {
            GLVertexBuilder.quad(bb, 
                x1, y0, z1,
                x1, y0, z0,
                x1, scaledY1, z0,
                x1, scaledY1, z1,
                colorModifier
            );
        }
    }

    private static void buildOutlineCell(
        AbstractStaticMesh.Builder<?, ?> bb,
        int cx,
        int cz,
        long cell,
        
        int currentLayer,
        int colorModifier,
        long[] cells,
        WrappedCoordinates wrapped,
        int range) {
        float x0 = cx * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float x1 = x0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float y0 = 0.0F;
        float y1 = MeshBuilder.HEIGHT_IN_BLOCKS;
        float z0 = cz * MeshBuilder.CELL_SIZE_IN_BLOCKS;
        float z1 = z0 + MeshBuilder.CELL_SIZE_IN_BLOCKS;

        CloudsConfiguration.LayerConfiguration layerConfiguration =
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        float scaledY1 = y1 * layerConfiguration.APPEARANCE.CLOUD_Y_SCALE;
        float outlineSize = layerConfiguration.APPEARANCE.OUTLINE_SIZE;

        // pos 0-3 are the top face vertices, pos 4-7 are the bottom face vertices (ccw, vertex i aligns vertically to vertex i+4, 0 < i < 4)
        // pos 1, 0, 4, 5 are the south face vertices (+z)
        // pos 2, 1, 5, 6 are the east face vertices (+x)
        // pos 3, 2, 6, 7 are the north face vertices (-z)
        // pos 0, 3, 7, 4 are the west face vertices (-x)
        float[] posX = new float[8];
        float[] posY = new float[8];
        float[] posZ = new float[8];

        boolean southEmpty = Texture.isSouthEmpty(cell);
        boolean westEmpty = Texture.isWestEmpty(cell);
        boolean northEmpty = Texture.isNorthEmpty(cell);
        boolean eastEmpty = Texture.isEastEmpty(cell);

        posY[0] = scaledY1 + outlineSize;
        posY[1] = scaledY1 + outlineSize;
        posY[2] = scaledY1 + outlineSize;
        posY[3] = scaledY1 + outlineSize;

        posX[0] = x0;
        posX[1] = x1;
        posX[2] = x1;
        posX[3] = x0;

        posZ[0] = z1;
        posZ[1] = z1;
        posZ[2] = z0;
        posZ[3] = z0;

        posY[4] = y0 - outlineSize;
        posY[5] = y0 - outlineSize;
        posY[6] = y0 - outlineSize;
        posY[7] = y0 - outlineSize;

        posX[4] = x0;
        posX[5] = x1;
        posX[6] = x1;
        posX[7] = x0;

        posZ[4] = z1;
        posZ[5] = z1;
        posZ[6] = z0;
        posZ[7] = z0;

        if (southEmpty) {
            posZ[0] += outlineSize;
            posZ[1] += outlineSize;
            posZ[4] += outlineSize;
            posZ[5] += outlineSize;
        }

        if (westEmpty) {
            posX[0] -= outlineSize;
            posX[3] -= outlineSize;
            posX[4] -= outlineSize;
            posX[7] -= outlineSize;
        }

        if (northEmpty) {
            posZ[2] -= outlineSize;
            posZ[3] -= outlineSize;
            posZ[6] -= outlineSize;
            posZ[7] -= outlineSize;
        }

        if (eastEmpty) {
            posX[1] += outlineSize;
            posX[2] += outlineSize;
            posX[5] += outlineSize;
            posX[6] += outlineSize;
        }

        if (outlineSize <= 0.0f) {
            return;
        }

        pushBackfacingQuad(bb,
            posX[0], posY[0], posZ[0],
            posX[1], posY[1], posZ[1],
            posX[2], posY[2], posZ[2],
            posX[3], posY[3], posZ[3],
            colorModifier);

        pushBackfacingQuad(bb,
            posX[7], posY[7], posZ[7],
            posX[6], posY[6], posZ[6],
            posX[5], posY[5], posZ[5],
            posX[4], posY[4], posZ[4],
            colorModifier);

        long nw = getCell(cells, wrapped, cx - 1, cz - 1, range);
        long n  = getCell(cells, wrapped, cx,     cz - 1, range);
        long ne = getCell(cells, wrapped, cx + 1, cz - 1, range);

        long w  = getCell(cells, wrapped, cx - 1, cz,     range);
        long e  = getCell(cells, wrapped, cx + 1, cz,     range);

        long sw = getCell(cells, wrapped, cx - 1, cz + 1, range);
        long s  = getCell(cells, wrapped, cx,     cz + 1, range);
        long se = getCell(cells, wrapped, cx + 1, cz + 1, range);

        boolean clipNW =
            (n == 0L || w == 0L) && nw != 0L;

        boolean clipNE =
            (n == 0L || e == 0L) && ne != 0L;

        boolean clipSW =
            (s == 0L || w == 0L) && sw != 0L;

        boolean clipSE =
            (s == 0L || e == 0L) && se != 0L;

        if (southEmpty) {
            float sx0 = posX[0];
            float sx1 = posX[1];

            if (clipSW)
                sx0 += outlineSize;

            if (clipSE)
                sx1 -= outlineSize;

            pushBackfacingQuad(
                bb,
                sx1, posY[1], posZ[1],
                sx0, posY[0], posZ[0],
                sx0, posY[4], posZ[4],
                sx1, posY[5], posZ[5],
                colorModifier);
        }

        if (northEmpty) {
            float nx0 = posX[3];
            float nx1 = posX[2];

            if (clipNW)
                nx0 += outlineSize;

            if (clipNE)
                nx1 -= outlineSize;

            pushBackfacingQuad(
                bb,
                nx0, posY[3], posZ[3],
                nx1, posY[2], posZ[2],
                nx1, posY[6], posZ[6],
                nx0, posY[7], posZ[7],
                colorModifier);
        }

        if (westEmpty) {
            float wz0 = posZ[3];
            float wz1 = posZ[0];

            if (clipNW)
                wz0 += outlineSize;

            if (clipSW)
                wz1 -= outlineSize;

            pushBackfacingQuad(
                bb,
                posX[0], posY[0], wz1,
                posX[3], posY[3], wz0,
                posX[7], posY[7], wz0,
                posX[4], posY[4], wz1,
                colorModifier);
        }

        if (eastEmpty) {
            float ez0 = posZ[2];
            float ez1 = posZ[1];

            if (clipNE)
                ez0 += outlineSize;

            if (clipSE)
                ez1 -= outlineSize;

            pushBackfacingQuad(
                bb,
                posX[2], posY[2], ez0,
                posX[1], posY[1], ez1,
                posX[5], posY[5], ez1,
                posX[6], posY[6], ez0,
                colorModifier);
        }
    }

    /**
     * Emits a backfacing quad, vertex coordinates should be passed in in the original CCW order.
     */
    private static void pushBackfacingQuad(AbstractStaticMesh.Builder<?, ?> bb,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int colorModifier) {
        bb.addVertex(x3, y3, z3).setColor(colorModifier).setNormal(0.0f, 0.0f, 0.0f);
        bb.addVertex(x2, y2, z2).setColor(colorModifier).setNormal(0.0f, 0.0f, 0.0f);
        bb.addVertex(x1, y1, z1).setColor(colorModifier).setNormal(0.0f, 0.0f, 0.0f);
        bb.addVertex(x0, y0, z0).setColor(colorModifier).setNormal(0.0f, 0.0f, 0.0f);
    }

    private static long getCell(long[] cells, WrappedCoordinates wrapped,
                         int x, int z, int range) {
        if (x < -range || x > range || z < -range || z > range)
            return 0L;
        return cells[wrapped.getCellIndex(x, z, range)];
    }
}
