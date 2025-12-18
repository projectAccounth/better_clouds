package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;

public final class BevelWrappers {

    public enum EdgeDir {
        NORTH( 0, -1),
        SOUTH( 0,  1),
        WEST (-1,  0),
        EAST ( 1,  0);

        public final int dx;
        public final int dz;

        EdgeDir(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    };

    public enum Sign {
        POS(1),
        NEG(-1);

        public final int value;

        Sign(int val) {
            this.value = val;
        }
    };

    private static int dx(EdgeDir d) {
        return (d == EdgeDir.WEST) ? -1 : (d == EdgeDir.EAST) ? 1 : 0;
    }
    private static int dz(EdgeDir d) {
        return (d == EdgeDir.NORTH) ? -1 : (d == EdgeDir.SOUTH) ? 1 : 0;
    }

    public static void topEdge(
        BufferBuilder bb,
        EdgeDir dir,
        float x0, float x1,
        float z0, float z1,
        float y,
        float radius,
        int segments,
        float r, float g, float b, float a
    ) {
        float ex0, ez0, ex1, ez1;

        if (dir == EdgeDir.NORTH) {
            ex0 = x0 + radius; ex1 = x1 - radius;
            ez0 = z0 + radius; ez1 = ez0;
        } else if (dir == EdgeDir.SOUTH) {
            ex0 = x0 + radius; ex1 = x1 - radius;
            ez0 = z1 - radius; ez1 = ez0;
        } else if (dir == EdgeDir.WEST) {
            ex0 = x0 + radius; ex1 = ex0;
            ez0 = z0 + radius; ez1 = z1 - radius;
        } else {
            ex0 = x1 - radius; ex1 = ex0;
            ez0 = z0 + radius; ez1 = z1 - radius;
        }

        GeometryUtils.buildCylindricalStrip(
            bb,
            ex0, y, ez0,
            ex1, y, ez1,
            0, 1, 0,
            (float)dx(dir), 0, (float)dz(dir),
            radius,
            segments,
            !(dir == EdgeDir.SOUTH || dir == EdgeDir.WEST),
            r, g, b, a
        );
    };

    public static void bottomEdge(
        BufferBuilder bb,
        EdgeDir dir,
        float x0, float x1,
        float z0, float z1,
        float y,
        float radius,
        int segments,
        float r, float g, float b, float a
    ) {
        float ex0, ez0, ex1, ez1;

        if (dir == EdgeDir.NORTH) {
            ex0 = x0 + radius; ex1 = x1 - radius;
            ez0 = z0 + radius; ez1 = ez0;
        } else if (dir == EdgeDir.SOUTH) {
            ex0 = x0 + radius; ex1 = x1 - radius;
            ez0 = z1 - radius; ez1 = ez0;
        } else if (dir == EdgeDir.WEST) {
            ex0 = x0 + radius; ex1 = ex0;
            ez0 = z0 + radius; ez1 = z1 - radius;
        } else {
            ex0 = x1 - radius; ex1 = ex0;
            ez0 = z0 + radius; ez1 = z1 - radius;
        }

        GeometryUtils.buildCylindricalStrip(
            bb,
            ex0, y, ez0,
            ex1, y, ez1,
            0, -1, 0,
            (float)dx(dir), 0, (float)dz(dir),
            radius,
            segments,
            !(dir == EdgeDir.EAST || dir == EdgeDir.NORTH),
            r, g, b, a
        );
    }

    public static void verticalEdge(
        BufferBuilder bb,

        Sign dirX,
        Sign dirZ,

        float x,
        float z,
        float y0,
        float y1,

        float radius,
        int segments,

        float r, float g, float b, float a
    ) {
        float sx = (float)dirX.value;
        float sz = (float)dirZ.value;

        // Edge extrusion (vertical)
        float ex0 = x;
        float ey0 = y1;
        float ez0 = z;

        float ex1 = x;
        float ey1 = y0;
        float ez1 = z;

        float nx = sx;
        float ny = 0.0f;
        float nz = 0.0f;

        float sxv = 0.0f;
        float syv = 0.0f;
        float szv = sz;

        boolean flip = ((sx > 0.0f) ^ (sz > 0.0f));

        GeometryUtils.buildCylindricalStrip(
            bb,
            ex0, ey0, ez0,
            ex1, ey1, ez1,
            nx, ny, nz,
            sxv, syv, szv,
            radius,
            segments,
            flip,
            r, g, b, a
        );
    }

}
