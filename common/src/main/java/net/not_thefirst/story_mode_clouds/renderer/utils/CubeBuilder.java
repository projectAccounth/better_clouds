package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.renderer.utils.BevelWrappers.EdgeDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.BevelWrappers.Sign;

public class CubeBuilder {
    public enum FaceDir {
        POS_X(0),
        NEG_X(1),
        POS_Y(2),
        NEG_Y(3),
        POS_Z(4),
        NEG_Z(5);

        public final int value;
        FaceDir(int val) {
            this.value = val;
        }
    };

    public static float signToFloat(Sign s) {
        return (s == Sign.POS) ? 1.0f : -1.0f;
    }

    public static class FaceMask {
        private int flag;
        public int get() { return flag; }

        public FaceMask(int flag) {
            this();
            this.flag = flag;
        }

        public FaceMask() {
            this.flag = 0;
        }

        public void addMask(FaceDir f) {
            flag |= 1 << f.value;
        }

        public void removeMask(FaceDir f) {
            flag ^= 1 << f.value;
        }
    }

    public static FaceMask faceBit(FaceDir f) {
        return new FaceMask(1 << f.value);
    }

    public static boolean hasFace(FaceMask mask, FaceDir f) {
        return (mask.get() & faceBit(f).get()) != 0;
    }

    public static FaceDir faceFromSignX(Sign sx) {
        return (sx == Sign.POS) ? FaceDir.POS_X : FaceDir.NEG_X;
    }

    public static FaceDir faceFromSignY(Sign sy) {
        return (sy == Sign.POS) ? FaceDir.POS_Y : FaceDir.NEG_Y;
    }

    public static FaceDir faceFromSignZ(Sign sz) {
        return (sz == Sign.POS) ? FaceDir.POS_Z : FaceDir.NEG_Z;
    }

    public static boolean edgeTouchesExcludedSide(
        EdgeDir dir,
        FaceMask excludedFaces
    ) {
        switch (dir) {
            case EdgeDir.NORTH: return hasFace(excludedFaces, FaceDir.NEG_Z);
            case EdgeDir.SOUTH: return hasFace(excludedFaces, FaceDir.POS_Z);
            case EdgeDir.WEST:  return hasFace(excludedFaces, FaceDir.NEG_X);
            case EdgeDir.EAST:  return hasFace(excludedFaces, FaceDir.POS_X);
        }
        return false;
    }
    public static boolean cornerVisible(Sign sx, Sign sy, Sign sz, FaceMask excluded) {
        return !hasFace(excluded, faceFromSignX(sx))
            && !hasFace(excluded, faceFromSignY(sy))
            && !hasFace(excluded, faceFromSignZ(sz));
    }

    public static boolean verticalEdgeVisible(Sign sx, Sign sz, FaceMask excluded) {
        return !hasFace(excluded, faceFromSignX(sx))
            && !hasFace(excluded, faceFromSignZ(sz));
    }

    public static boolean horizontalEdgeVisible(EdgeDir dir, boolean top, FaceMask excluded) {
        if (top && hasFace(excluded, FaceDir.POS_Y)) return false;
        if (!top && hasFace(excluded, FaceDir.NEG_Y)) return false;
        return !edgeTouchesExcludedSide(dir, excluded);
    }

    public static void emitCorner(
        BufferBuilder bb,
        float x, float y, float z,
        Sign sx, Sign sy, Sign sz,
        float radius,
        int segments,
        float r, float g, float b, float a
    ) {
        float fx = signToFloat(sx);
        float fy = signToFloat(sy);
        float fz = signToFloat(sz);

        boolean flip = (fx * fy * fz) >= 0.0f;

        GeometryUtils.buildSphericalCorner(
            bb,
            x, y, z,
            fx, 0.0f, 0.0f,
            0.0f, fy, 0.0f,
            0.0f, 0.0f, fz,
            radius,
            segments,
            flip,
            r, g, b, a
        );
    }

    public static void emitTopAndBottomEdges(
        BufferBuilder bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yTop, float yBot,
        float radius,
        int segments,
        float r, float g, float b, float a
    ) {
        for (EdgeDir dir : EdgeDir.values()) {
            BevelWrappers.topEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yTop,
                radius,
                segments,
                r, g, b, a
            );

            BevelWrappers.bottomEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yBot,
                radius,
                segments,
                r, g, b, a
            );
        }
    }

    static class VEdge {
        Sign sx;
        Sign sz;
        float x;
        float z;

        VEdge(Sign x, Sign z, float xV, float zV) {
            this.sx = x;
            this.sz = z;
            this.x = xV;
            this.z = zV;
        }
    };

    static class VVert {
        float x, y, z;
        Sign sx, sy, sz;

        VVert(float xV, float yV, float zV,
                Sign xS, Sign yS, Sign zS) {
            this.x = xV;
            this.y = yV;
            this.z = zV;
            this.sx = xS;
            this.sy = yS;
            this.sz = zS;
        }
    }

    public static void emitVerticalEdges(
        BufferBuilder bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float y0, float y1,
        float radius,
        int segments,
        float r, float g, float b, float a
    ) {
        final VEdge[] edges = {
            new VEdge(Sign.POS, Sign.POS, maxX - radius, maxZ - radius),
            new VEdge(Sign.NEG, Sign.NEG, minX + radius, minZ + radius),
            new VEdge(Sign.NEG, Sign.POS, minX + radius, maxZ - radius),
            new VEdge(Sign.POS, Sign.NEG, maxX - radius, minZ + radius)
        };

        for (VEdge e : edges) {
            BevelWrappers.verticalEdge(
                bb,
                e.sx,
                e.sz,
                e.x,
                e.z,
                y0,
                y1,
                radius,
                segments,
                r, g, b, a
            );
        }
    }

    public static void emitInsetFaces(
        BufferBuilder bb,
        float minX, float maxX,
        float minY, float maxY,
        float minZ, float maxZ,
        float radius,
        FaceMask excludedFaces,
        float r, float g, float b, float a
    ) {
        float ix0 = hasFace(excludedFaces, FaceDir.NEG_X) ? minX : minX + radius;
        float ix1 = hasFace(excludedFaces, FaceDir.POS_X) ? maxX : maxX - radius;

        float iy0 = hasFace(excludedFaces, FaceDir.NEG_Y) ? minY : minY + radius;
        float iy1 = hasFace(excludedFaces, FaceDir.POS_Y) ? maxY : maxY - radius;

        float iz0 = hasFace(excludedFaces, FaceDir.NEG_Z) ? minZ : minZ + radius;
        float iz1 = hasFace(excludedFaces, FaceDir.POS_Z) ? maxZ : maxZ - radius;

        if (!hasFace(excludedFaces, FaceDir.POS_Y)) {
            VertexBuilder.quad(bb,
                ix0, maxY, iz1,
                ix1, maxY, iz1,
                ix1, maxY, iz0,
                ix0, maxY, iz0,
                r, g, b, a
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_Y)) {
            VertexBuilder.quad(bb,
                ix1, minY, iz0,
                ix1, minY, iz1,
                ix0, minY, iz1,
                ix0, minY, iz0,
                r, g, b, a
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_X)) {
            VertexBuilder.quad(bb,
                minX, iy0, iz1,
                minX, iy1, iz1,
                minX, iy1, iz0,
                minX, iy0, iz0,
                r, g, b, a
            );
        }

        if (!hasFace(excludedFaces, FaceDir.POS_X)) {
            VertexBuilder.quad(bb,
                maxX, iy0, iz0,
                maxX, iy1, iz0,
                maxX, iy1, iz1,
                maxX, iy0, iz1,
                r, g, b, a
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_Z)) {
            VertexBuilder.quad(bb,
                ix0, iy0, minZ,
                ix0, iy1, minZ,
                ix1, iy1, minZ,
                ix1, iy0, minZ,
                r, g, b, a
            );
        }

        if (!hasFace(excludedFaces, FaceDir.POS_Z)) {
            VertexBuilder.quad(bb,
                ix1, iy0, maxZ,
                ix1, iy1, maxZ,
                ix0, iy1, maxZ,
                ix0, iy0, maxZ,
                r, g, b, a
            );
        }
    }

    private static float[] extendEdgeRangeForExcludedFace(
        EdgeDir dir,
        float radius,
        FaceMask excluded,

        float initialMinX, float initialMaxX,
        float initialMinZ, float initialMaxZ
    ) {
        float minX = initialMinX;
        float maxX = initialMaxX;
        float minZ = initialMinZ;
        float maxZ = initialMaxZ;

        switch (dir) {
            case EdgeDir.NORTH:
            case EdgeDir.SOUTH:
                if (hasFace(excluded, FaceDir.POS_X)) {
                    maxX += radius;
                }
                if (hasFace(excluded, FaceDir.NEG_X)) {
                    minX -= radius;
                }
                break;

            case EdgeDir.WEST:
            case EdgeDir.EAST:
                if (hasFace(excluded, FaceDir.NEG_Z)) {
                    minZ -= radius;
                }
                if (hasFace(excluded, FaceDir.POS_Z)) {
                    maxZ += radius;
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid EdgeDir");
        }

        return new float[] {
            minX, maxX,
            minZ, maxZ
        };
    }

    public static void buildBeveledCube(
        BufferBuilder bb,
        float minX, float maxX,
        float minY, float maxY,
        float minZ, float maxZ,
        float radius,
        int edgeSegments,
        int cornerSegments,
        FaceMask excludedFaces,
        float r, float g, float b, float a
    ) {
        float yTop = hasFace(excludedFaces, FaceDir.POS_Y)
            ? maxY
            : maxY - radius;

        float yBot = hasFace(excludedFaces, FaceDir.NEG_Y)
            ? minY
            : minY + radius;

        for (EdgeDir dir : EdgeDir.values()) {
            float ex0 = minX;
            float ex1 = maxX;
            float ez0 = minZ;
            float ez1 = maxZ;

            float[] spanning = extendEdgeRangeForExcludedFace(
                dir,
                radius,
                excludedFaces,
                ex0, ex1,
                ez0, ez1
            );

            ex0 = spanning[0];
            ex1 = spanning[1];
            ez0 = spanning[2];
            ez1 = spanning[3];

            if (horizontalEdgeVisible(dir, true, excludedFaces)) {
                BevelWrappers.topEdge(
                    bb, dir,
                    ex0, ex1,
                    ez0, ez1,
                    yTop,
                    radius,
                    edgeSegments,
                    r, g, b, a
                );
            }

            if (horizontalEdgeVisible(dir, false, excludedFaces)) {
                BevelWrappers.bottomEdge(
                    bb, dir,
                    ex0, ex1,
                    ez0, ez1,
                    yBot,
                    radius,
                    edgeSegments,
                    r, g, b, a
                );
            }
        }

        final VEdge[] edges = {
            new VEdge(Sign.POS, Sign.POS, maxX - radius, maxZ - radius),
            new VEdge(Sign.NEG, Sign.NEG, minX + radius, minZ + radius),
            new VEdge(Sign.NEG, Sign.POS, minX + radius, maxZ - radius),
            new VEdge(Sign.POS, Sign.NEG, maxX - radius, minZ + radius)
        };

        for (VEdge e : edges) {
            if (!verticalEdgeVisible(e.sx, e.sz, excludedFaces)) {
                continue;
            }

            BevelWrappers.verticalEdge(
                bb,
                e.sx,
                e.sz,
                e.x,
                e.z,
                yBot,
                yTop,
                radius,
                edgeSegments,
                r, g, b, a
            );
        }

        VVert[] corners = {
            new VVert(maxX - radius, maxY - radius, maxZ - radius, Sign.POS, Sign.POS, Sign.POS),
            new VVert(minX + radius, maxY - radius, maxZ - radius, Sign.NEG, Sign.POS, Sign.POS),
            new VVert(minX + radius, maxY - radius, minZ + radius, Sign.NEG, Sign.POS, Sign.NEG),
            new VVert(maxX - radius, maxY - radius, minZ + radius, Sign.POS, Sign.POS, Sign.NEG),

            new VVert(maxX - radius, minY + radius, maxZ - radius, Sign.POS, Sign.NEG, Sign.POS),
            new VVert(minX + radius, minY + radius, maxZ - radius, Sign.NEG, Sign.NEG, Sign.POS),
            new VVert(minX + radius, minY + radius, minZ + radius, Sign.NEG, Sign.NEG, Sign.NEG),
            new VVert(maxX - radius, minY + radius, minZ + radius, Sign.POS, Sign.NEG, Sign.NEG)
        };

        for (VVert c : corners) {
            if (!cornerVisible(c.sx, c.sy, c.sz, excludedFaces)) {
                continue;
            }

            emitCorner(
                bb,
                c.x, c.y, c.z,
                c.sx, c.sy, c.sz,
                radius,
                cornerSegments,
                r, g, b, a
            );
        }

        emitInsetFaces(
            bb,
            minX, maxX,
            minY, maxY,
            minZ, maxZ,
            radius,
            excludedFaces,
            r, g, b, a
        );
    }
}
