package net.not_thefirst.story_mode_clouds.renderer.utils.geometry;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexBuilder;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.BevelWrappers.EdgeDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.geometry.BevelWrappers.Sign;

public class CubeBuilder {
    public enum FaceDir {
        POS_X(1 << 0),
        NEG_X(1 << 1),
        POS_Y(1 << 2),
        NEG_Y(1 << 3),
        POS_Z(1 << 4),
        NEG_Z(1 << 5);

        public final int bit;

        FaceDir(int bit) {
            this.bit = bit;
        }
    }

    public static float signToFloat(Sign s) {
        return (s == Sign.POS) ? 1.0f : -1.0f;
    }
    public static final class FaceMask {
        private int bits;

        public FaceMask() {
            this.bits = 0;
        }

        public FaceMask(int bits) {
            this.bits = bits;
        }

        public boolean has(FaceDir f) {
            return (bits & f.bit) != 0;
        }

        public void add(FaceDir f) {
            bits |= f.bit;
        }

        public void remove(FaceDir f) {
            bits &= ~f.bit;
        }

        public int getRaw() {
            return bits;
        }
    }


    public static boolean hasFace(FaceMask mask, FaceDir f) {
        return (mask.getRaw() & f.bit) != 0;
    }

    private static final FaceDir[][] SIGN_TO_FACE = {
        { FaceDir.NEG_X, FaceDir.POS_X },
        { FaceDir.NEG_Y, FaceDir.POS_Y },
        { FaceDir.NEG_Z, FaceDir.POS_Z }
    };

    private static FaceDir faceFrom(int axis, Sign s) {
        return SIGN_TO_FACE[axis][s == Sign.POS ? 1 : 0];
    }

    public static boolean edgeTouchesExcludedSide(
        EdgeDir dir,
        FaceMask excluded
    ) {
        final FaceDir[] EDGE_TOUCH_FACE = {
            FaceDir.NEG_Z, // NORTH
            FaceDir.POS_Z, // SOUTH
            FaceDir.NEG_X, // WEST
            FaceDir.POS_X  // EAST
        };
        return excluded.has(EDGE_TOUCH_FACE[dir.ordinal()]);
    }

    public static boolean cornerVisible(
        Sign sx, Sign sy, Sign sz,
        FaceMask excluded
    ) {
        return !excluded.has(faceFrom(0, sx))
            && !excluded.has(faceFrom(1, sy))
            && !excluded.has(faceFrom(2, sz));
    }

    public static boolean verticalEdgeVisible(
        Sign sx, Sign sz,
        FaceMask excluded
    ) {
        return !excluded.has(faceFrom(0, sx))
            && !excluded.has(faceFrom(2, sz));
    }

    public static boolean horizontalEdgeVisible(
        EdgeDir dir,
        boolean top,
        FaceMask excluded
    ) {
        FaceDir yFace = top ? FaceDir.POS_Y : FaceDir.NEG_Y;
        return !excluded.has(yFace)
            && !edgeTouchesExcludedSide(dir, excluded);
    }


    public static void emitCorner(
        BuildingMesh bb,
        float x, float y, float z,
        Sign sx, Sign sy, Sign sz,
        float radius,
        int segments,
        int layer,
        float relY, int skyColor
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
            layer, relY, skyColor
        );
    }

    public static void emitTopAndBottomEdges(
        BuildingMesh bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yTop, float yBot,
        float radius,
        int segments,
        int layer,
        float relY, int skyColor
    ) {
        for (EdgeDir dir : EdgeDir.values()) {
            BevelWrappers.topEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yTop,
                radius,
                segments,
                layer, relY, skyColor
            );

            BevelWrappers.bottomEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yBot,
                radius,
                segments,
                layer, 
                relY, skyColor
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

    private static final class CornerCap {
        final FaceDir fx;
        final FaceDir fz;
        final EdgeDir dir;
        final boolean top;
        final boolean flip;

        CornerCap(FaceDir fx, FaceDir fz, EdgeDir dir, boolean top, boolean flip) {
            this.fx = fx;
            this.fz = fz;
            this.dir = dir;
            this.top = top;
            this.flip = flip;
        }
    }

    private static final CornerCap[] CORNER_CAPS = {
        // NEG_X.NEG_Z
        new CornerCap(FaceDir.NEG_X, FaceDir.NEG_Z, EdgeDir.WEST,  true,  false),
        new CornerCap(FaceDir.NEG_X, FaceDir.NEG_Z, EdgeDir.NORTH, true,  true),
        new CornerCap(FaceDir.NEG_X, FaceDir.NEG_Z, EdgeDir.WEST,  false, true),
        new CornerCap(FaceDir.NEG_X, FaceDir.NEG_Z, EdgeDir.NORTH, false, false),

        // POS_X.NEG_Z
        new CornerCap(FaceDir.POS_X, FaceDir.NEG_Z, EdgeDir.EAST,  true,  true),
        new CornerCap(FaceDir.POS_X, FaceDir.NEG_Z, EdgeDir.NORTH, true,  true),
        new CornerCap(FaceDir.POS_X, FaceDir.NEG_Z, EdgeDir.EAST,  false, false),
        new CornerCap(FaceDir.POS_X, FaceDir.NEG_Z, EdgeDir.NORTH, false, false),

        // NEG_X.POS_Z
        new CornerCap(FaceDir.NEG_X, FaceDir.POS_Z, EdgeDir.WEST,  true,  false),
        new CornerCap(FaceDir.NEG_X, FaceDir.POS_Z, EdgeDir.SOUTH, true,  false),
        new CornerCap(FaceDir.NEG_X, FaceDir.POS_Z, EdgeDir.WEST,  false, true),
        new CornerCap(FaceDir.NEG_X, FaceDir.POS_Z, EdgeDir.SOUTH, false, true),

        // POS_X.POS_Z
        new CornerCap(FaceDir.POS_X, FaceDir.POS_Z, EdgeDir.EAST,  true,  true),
        new CornerCap(FaceDir.POS_X, FaceDir.POS_Z, EdgeDir.SOUTH, true,  false),
        new CornerCap(FaceDir.POS_X, FaceDir.POS_Z, EdgeDir.EAST,  false, false),
        new CornerCap(FaceDir.POS_X, FaceDir.POS_Z, EdgeDir.SOUTH, false, true),
    };

    private static float cap0(Sign s, float min, float max, float r) {
        return (s == Sign.NEG) ? min : max - r;
    }

    private static float cap1(Sign s, float min, float max, float r) {
        return (s == Sign.NEG) ? min + r : max;
    }


    private static float backOffX(EdgeDir dir, float x, float r) {
        return (dir == EdgeDir.WEST) ? x + r :
            (dir == EdgeDir.EAST) ? x - r : x;
    }

    private static float backOffZ(EdgeDir dir, float z, float r) {
        return (dir == EdgeDir.NORTH) ? z + r :
            (dir == EdgeDir.SOUTH) ? z - r : z;
    }

    private static void emitHorizontalCornerCaps(
        BuildingMesh bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yTop, float yBot,
        float radius,
        int segments,
        FaceMask excluded,
        int layer,
        float relY, int skyColor
    ) {
        for (CornerCap cap : CORNER_CAPS) {
            if (!excluded.has(cap.fx) || !excluded.has(cap.fz)) {
                continue;
            }

            Sign sx = (cap.fx == FaceDir.POS_X) ? Sign.POS : Sign.NEG;
            Sign sz = (cap.fz == FaceDir.POS_Z) ? Sign.POS : Sign.NEG;

            float x0 = cap0(sx, minX, maxX, radius);
            float x1 = cap1(sx, minX, maxX, radius);
            float z0 = cap0(sz, minZ, maxZ, radius);
            float z1 = cap1(sz, minZ, maxZ, radius);

            float y  = cap.top ? yTop : yBot;
            float ny = cap.top ? 1.0f : -1.0f;

            float ex0, ez0, ex1, ez1;

            if (cap.dir == EdgeDir.NORTH || cap.dir == EdgeDir.SOUTH) {
                ex0 = x0; ex1 = x1;
                ez0 = (cap.dir == EdgeDir.NORTH) ? z0 : z1;
                ez0 = backOffZ(cap.dir, ez0, radius);
                ez1 = ez0;
            } else {
                ex0 = (cap.dir == EdgeDir.WEST) ? x0 : x1;
                ex0 = backOffX(cap.dir, ex0, radius);
                ex1 = ex0;
                ez0 = z0; ez1 = z1;
            }

            GeometryUtils.buildCylindricalStrip(
                bb,
                ex0, y, ez0,
                ex1, y, ez1,
                0.0f, ny, 0.0f,
                (float)BevelWrappers.dx(cap.dir), 0.0f, (float)BevelWrappers.dz(cap.dir),
                radius,
                segments,
                cap.flip,
                layer, relY, skyColor
            );
        }
    }

    private static final FaceDir[] INSET_FACES = {
        FaceDir.POS_Y,
        FaceDir.NEG_Y,
        FaceDir.NEG_X,
        FaceDir.POS_X,
        FaceDir.NEG_Z,
        FaceDir.POS_Z
    };

    public static void emitInsetFaces(
        BuildingMesh bb,
        float minX, float maxX,
        float minY, float maxY,
        float minZ, float maxZ,
        float radius,
        FaceMask excluded,
        int layer,
        float relY, int skyColor
    ) {
        float ix0 = excluded.has(FaceDir.NEG_X) ? minX : minX + radius;
        float ix1 = excluded.has(FaceDir.POS_X) ? maxX : maxX - radius;
        float iy0 = excluded.has(FaceDir.NEG_Y) ? minY : minY + radius;
        float iy1 = excluded.has(FaceDir.POS_Y) ? maxY : maxY - radius;
        float iz0 = excluded.has(FaceDir.NEG_Z) ? minZ : minZ + radius;
        float iz1 = excluded.has(FaceDir.POS_Z) ? maxZ : maxZ - radius;

        for (FaceDir f : INSET_FACES) {
            if (excluded.has(f)) {
                continue;
            }

            switch (f) {
                case POS_Y:
                    VertexBuilder.quad(bb,
                        ix0, maxY, iz1,
                        ix1, maxY, iz1,
                        ix1, maxY, iz0,
                        ix0, maxY, iz0,
                        layer, relY, skyColor
                    );
                    break;

                case NEG_Y:
                    VertexBuilder.quad(bb,
                        ix1, minY, iz0,
                        ix1, minY, iz1,
                        ix0, minY, iz1,
                        ix0, minY, iz0,
                        layer, relY, skyColor
                    );
                    break;

                case NEG_X:
                    VertexBuilder.quad(bb,
                        minX, iy0, iz1,
                        minX, iy1, iz1,
                        minX, iy1, iz0,
                        minX, iy0, iz0,
                        layer, relY, skyColor
                    );
                    break;

                case POS_X:
                    VertexBuilder.quad(bb,
                        maxX, iy0, iz0,
                        maxX, iy1, iz0,
                        maxX, iy1, iz1,
                        maxX, iy0, iz1,
                        layer, relY, skyColor
                    );
                    break;

                case NEG_Z:
                    VertexBuilder.quad(bb,
                        ix0, iy0, minZ,
                        ix0, iy1, minZ,
                        ix1, iy1, minZ,
                        ix1, iy0, minZ,
                        layer, relY, skyColor
                    );
                    break;

                case POS_Z:
                    VertexBuilder.quad(bb,
                        ix1, iy0, maxZ,
                        ix1, iy1, maxZ,
                        ix0, iy1, maxZ,
                        ix0, iy0, maxZ,
                        layer, relY, skyColor
                    );
                    break;
            }
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
            case NORTH, SOUTH:
                if (hasFace(excluded, FaceDir.POS_X)) {
                    maxX += radius;
                }
                if (hasFace(excluded, FaceDir.NEG_X)) {
                    minX -= radius;
                }
                break;

            case EAST, WEST:
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

    private static final Sign[] EDGE_SIGNS = {
        Sign.POS, Sign.NEG
    };

    private static void emitVerticalEdges(
        BuildingMesh bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yBot, float yTop,
        float radius,
        int segments,
        FaceMask excluded,
        int layer,
        float relY, int skyColor
    ) {
        for (Sign sx : EDGE_SIGNS) {
            for (Sign sz : EDGE_SIGNS) {
                if (!verticalEdgeVisible(sx, sz, excluded)) {
                    continue;
                }

                float x = (sx == Sign.POS) ? maxX - radius : minX + radius;
                float z = (sz == Sign.POS) ? maxZ - radius : minZ + radius;

                BevelWrappers.verticalEdge(
                    bb,
                    sx, sz,
                    x, z,
                    yBot, yTop,
                    radius,
                    segments,
                    layer, relY, skyColor
                );
            }
        }
    }

    private static void emitCorners(
        BuildingMesh bb,
        float minX, float maxX,
        float minY, float maxY,
        float minZ, float maxZ,
        float radius,
        int segments,
        FaceMask excluded,
        int layer,
        float relY, int skyColor
    ) {
        for (Sign sx : EDGE_SIGNS) {
            for (Sign sy : EDGE_SIGNS) {
                for (Sign sz : EDGE_SIGNS) {
                    if (!cornerVisible(sx, sy, sz, excluded)) {
                        continue;
                    }

                    float x = (sx == Sign.POS) ? maxX - radius : minX + radius;
                    float y = (sy == Sign.POS) ? maxY - radius : minY + radius;
                    float z = (sz == Sign.POS) ? maxZ - radius : minZ + radius;

                    emitCorner(
                        bb,
                        x, y, z,
                        sx, sy, sz,
                        radius,
                        segments,
                        layer, relY, skyColor
                    );
                }
            }
        }
    }

    public static void buildBeveledCube(
        BuildingMesh bb,
        float minX, float maxX,
        float minY, float maxY,
        float minZ, float maxZ,
        float radius,
        int edgeSegments,
        int cornerSegments,
        FaceMask excludedFaces,
        int layer,
        CustomCloudRenderer.LayerState state,
        float relY, int idxX, int idxY, 
        int skyColor
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
                    layer, 
                    relY, skyColor
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
                    layer,
                    relY, skyColor
                );
            }
        }

        emitVerticalEdges(bb, 
            minX, maxX, 
            minZ, maxZ, 
            yBot, yTop, 
            radius, edgeSegments, 
            excludedFaces, 
            layer, relY, skyColor
        );

        emitCorners(
            bb, 
            minX, maxX, 
            minY, maxY, 
            minZ, maxZ, radius, 
            cornerSegments, excludedFaces, 
            layer, relY, skyColor
        );

        if (state.texture().neighbors[idxX + idxY * state.texture().width] < 8) {
            emitHorizontalCornerCaps(
                bb, 
                minX, maxX, 
                minZ, maxZ, 
                yTop, yBot, 
                radius, 
                cornerSegments, 
                excludedFaces, 
                layer,  
                relY, skyColor
            );
        }

        emitInsetFaces(
            bb,
            minX, maxX,
            minY, maxY,
            minZ, maxZ,
            radius,
            excludedFaces,
            layer, 
            relY, skyColor
        );
    }
}
