package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.utils.BevelWrappers.EdgeDir;
import net.not_thefirst.story_mode_clouds.renderer.utils.BevelWrappers.Sign;
import net.not_thefirst.story_mode_clouds.utils.Texture;

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
        int layer,
        RelativeCameraPos pos, float relY, int skyColor
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
            layer, pos, relY, skyColor
        );
    }

    public static void emitTopAndBottomEdges(
        BufferBuilder bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yTop, float yBot,
        float radius,
        int segments,
        int layer,
        RelativeCameraPos pos, float relY, int skyColor
    ) {
        for (EdgeDir dir : EdgeDir.values()) {
            BevelWrappers.topEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yTop,
                radius,
                segments,
                layer, pos, relY, skyColor
            );

            BevelWrappers.bottomEdge(
                bb, dir,
                minX, maxX,
                minZ, maxZ,
                yBot,
                radius,
                segments,
                layer, 
                pos, relY, skyColor
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

    private static float capX0(Sign sx, float minX, float maxX, float r) {
        return (sx == Sign.NEG) ? minX : maxX - r;
    }

    private static float capX1(Sign sx, float minX, float maxX, float r) {
        return (sx == Sign.NEG) ? minX + r : maxX;
    }

    private static float capZ0(Sign sz, float minZ, float maxZ, float r) {
        return (sz == Sign.NEG) ? minZ : maxZ - r;
    }

    private static float capZ1(Sign sz, float minZ, float maxZ, float r) {
        return (sz == Sign.NEG) ? minZ + r : maxZ;
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
        BufferBuilder bb,
        float minX, float maxX,
        float minZ, float maxZ,
        float yTop, float yBot,
        float radius,
        int segments,
        FaceMask excluded,
        int layer,
        RelativeCameraPos pos, float relY, int skyColor
    ) {
        for (CornerCap cap : CORNER_CAPS) {
            if (!hasFace(excluded, cap.fx) || !hasFace(excluded, cap.fz)) {
                continue;
            }

            Sign sx = (cap.fx == FaceDir.POS_X) ? Sign.POS : Sign.NEG;
            Sign sz = (cap.fz == FaceDir.POS_Z) ? Sign.POS : Sign.NEG;

            float x0 = capX0(sx, minX, maxX, radius);
            float x1 = capX1(sx, minX, maxX, radius);
            float z0 = capZ0(sz, minZ, maxZ, radius);
            float z1 = capZ1(sz, minZ, maxZ, radius);

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
                0, ny, 0,
                (float)BevelWrappers.dx(cap.dir), 0, (float)BevelWrappers.dz(cap.dir),
                radius,
                segments,
                cap.flip,
                layer, pos, relY, skyColor
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
        int layer,
        RelativeCameraPos pos, float relY, int skyColor
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
                layer,
                pos, relY, skyColor
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_Y)) {
            VertexBuilder.quad(bb,
                ix1, minY, iz0,
                ix1, minY, iz1,
                ix0, minY, iz1,
                ix0, minY, iz0,
                layer,
                pos, relY, skyColor
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_X)) {
            VertexBuilder.quad(bb,
                minX, iy0, iz1,
                minX, iy1, iz1,
                minX, iy1, iz0,
                minX, iy0, iz0,
                layer,
                pos, relY, skyColor
            );
        }

        if (!hasFace(excludedFaces, FaceDir.POS_X)) {
            VertexBuilder.quad(bb,
                maxX, iy0, iz0,
                maxX, iy1, iz0,
                maxX, iy1, iz1,
                maxX, iy0, iz1,
                layer,
                pos, relY, skyColor
            );
        }

        if (!hasFace(excludedFaces, FaceDir.NEG_Z)) {
            VertexBuilder.quad(bb,
                ix0, iy0, minZ,
                ix0, iy1, minZ,
                ix1, iy1, minZ,
                ix1, iy0, minZ,
                layer,
                pos, relY, skyColor
            );
        }

        if (!hasFace(excludedFaces, FaceDir.POS_Z)) {
            VertexBuilder.quad(bb,
                ix1, iy0, maxZ,
                ix1, iy1, maxZ,
                ix0, iy1, maxZ,
                ix0, iy0, maxZ,
                layer,
                pos, relY, skyColor
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
        int layer,
        float camX, float camY, float camZ,
        CustomCloudRenderer.LayerState state,
        RelativeCameraPos pos, float relY, int cellIdx, int skyColor
    ) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
            CloudsConfiguration.INSTANCE.getLayer(layer);

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
                    pos, relY, skyColor
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
                    pos, relY, skyColor
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
                layer, 
                pos, relY, skyColor
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
                layer, 
                pos, relY, skyColor
            );
        }

        long cell = state.texture.cells[cellIdx];

        int northClear = Texture.clearOnNorth(cell) ? 1 : 0;
        int southClear = Texture.clearOnSouth(cell) ? 1 : 0;
        int westClear  = Texture.clearOnWest(cell)  ? 1 : 0;
        int eastClear  = Texture.clearOnEast(cell)  ? 1 : 0;

        // emit if has less than 8 neighbors (exposed corner/edge)
        // AND is not the intersection of a T/L junction or a cross.
        if (state.texture.neighbors[cellIdx] < 8) {
            emitHorizontalCornerCaps(
                bb, 
                minX, maxX, 
                minZ, maxZ, 
                yTop, yBot, 
                radius, 
                cornerSegments, 
                excludedFaces, 
                layer,  
                pos, relY, skyColor
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
            pos, relY, skyColor
        );
    }
}
