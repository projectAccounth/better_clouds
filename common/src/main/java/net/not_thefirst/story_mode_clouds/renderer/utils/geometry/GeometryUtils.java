package net.not_thefirst.story_mode_clouds.renderer.utils.geometry;

import net.not_thefirst.lib.gl_render_system.mesh.BuildingMesh;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexBuilder;

public final class GeometryUtils {

    /**
     * Builds a cylindrical bevel strip between two parallel edges.
     *
     * The arc is swept in the plane defined by (normal, sweepDir).
     * The strip is extruded along edgeDir.
     */
    public static void buildCylindricalStrip(
            BuildingMesh bb,
            float edgeStartX, float edgeStartY, float edgeStartZ,
            float edgeEndX,   float edgeEndY,   float edgeEndZ,
            float normalX, float normalY, float normalZ,
            float sweepX,  float sweepY,  float sweepZ,
            float radius,
            int segments,
            boolean flip,
            int layer, float relY, int skyColor
    ) {
        final float HALF_PI = 1.57079632679f;
        final float invSeg = 1.0f / segments;
        final float step = HALF_PI * invSeg;

        final float cosStep = (float)Math.cos(step);
        final float sinStep = (float)Math.sin(step);

        float edgeDX = edgeEndX - edgeStartX;
        float edgeDY = edgeEndY - edgeStartY;
        float edgeDZ = edgeEndZ - edgeStartZ;

        float n0 = radius;
        float s0 = 0.0f;

        for (int i = 0; i < segments; i++) {
            float n1 = n0 * cosStep - s0 * sinStep;
            float s1 = n0 * sinStep + s0 * cosStep;

            float ax0 = edgeStartX + normalX * n0 + sweepX * s0;
            float ay0 = edgeStartY + normalY * n0 + sweepY * s0;
            float az0 = edgeStartZ + normalZ * n0 + sweepZ * s0;

            float ax1 = edgeStartX + normalX * n1 + sweepX * s1;
            float ay1 = edgeStartY + normalY * n1 + sweepY * s1;
            float az1 = edgeStartZ + normalZ * n1 + sweepZ * s1;

            float bx0 = ax0 + edgeDX;
            float by0 = ay0 + edgeDY;
            float bz0 = az0 + edgeDZ;

            float bx1 = ax1 + edgeDX;
            float by1 = ay1 + edgeDY;
            float bz1 = az1 + edgeDZ;

            if (!flip) {
                VertexBuilder.quad(
                    bb,
                    ax0, ay0, az0,
                    ax1, ay1, az1,
                    bx1, by1, bz1,
                    bx0, by0, bz0,
                    layer, relY, skyColor
                );
            } else {
                VertexBuilder.quad(
                    bb,
                    bx0, by0, bz0,
                    bx1, by1, bz1,
                    ax1, ay1, az1,
                    ax0, ay0, az0,
                    layer, relY, skyColor
                );
            }

            n0 = n1;
            s0 = s1;
        }
    }


    private static void emit(
        float px, float py, float pz,
        float cx, float cy, float cz,

        float axx, float axy, float axz,
        float ayx, float ayy, float ayz,
        float azx, float azy, float azz,

        float radius,
        float[] out,
        int base
    ) {
        float invLen = radius / (float)Math.sqrt(px * px + py * py + pz * pz);

        px *= invLen;
        py *= invLen;
        pz *= invLen;

        out[base    ] = cx + axx * px + ayx * py + azx * pz;
        out[base + 1] = cy + axy * px + ayy * py + azy * pz;
        out[base + 2] = cz + axz * px + ayz * py + azz * pz;
    }

    public static void buildSphericalCorner(
            BuildingMesh bb,
            float cx, float cy, float cz,

            float axx, float axy, float axz,
            float ayx, float ayy, float ayz,
            float azx, float azy, float azz,

            float radius,
            int segments,
            boolean flip,

            int layer, float relY, int skyColor
    ) {
        final float invSeg = 1.0f / segments;

        float[] prevRow = new float[(segments + 1) * 3];
        float[] currRow = new float[(segments + 1) * 3];

        for (int i = 0; i <= segments; i++) {
            float u = i * invSeg;
            int rowCount = segments - i;

            for (int j = 0; j <= rowCount; j++) {
                float v = j * invSeg;

                float px = u;
                float py = 1.0f - u - v;
                float pz = v;

                emit(
                    px, py, pz,
                    cx, cy, cz,
                    axx, axy, axz,
                    ayx, ayy, ayz,
                    azx, azy, azz,
                    radius,
                    currRow,
                    j * 3
                );
            }

            if (i > 0) {
                int quadCount = segments - (i - 1);

                for (int j = 0; j < quadCount; j++) {
                    int i0 = j * 3;
                    int i1 = i0 + 3;

                    float v00x = prevRow[i0];
                    float v00y = prevRow[i0 + 1];
                    float v00z = prevRow[i0 + 2];

                    float v01x = prevRow[i1];
                    float v01y = prevRow[i1 + 1];
                    float v01z = prevRow[i1 + 2];

                    float v10x = currRow[i0];
                    float v10y = currRow[i0 + 1];
                    float v10z = currRow[i0 + 2];

                    if (j + 1 <= segments - i) {
                        float v11x = currRow[i1];
                        float v11y = currRow[i1 + 1];
                        float v11z = currRow[i1 + 2];

                        if (!flip) {
                            VertexBuilder.quad(
                                bb,
                                v00x, v00y, v00z,
                                v10x, v10y, v10z,
                                v11x, v11y, v11z,
                                v01x, v01y, v01z,
                                layer, relY, skyColor
                            );
                        } else {
                            VertexBuilder.quad(
                                bb,
                                v01x, v01y, v01z,
                                v11x, v11y, v11z,
                                v10x, v10y, v10z,
                                v00x, v00y, v00z,
                                layer, relY, skyColor
                            );
                        }
                    } else {
                        if (!flip) {
                            VertexBuilder.triangle(
                                bb,
                                v00x, v00y, v00z,
                                v10x, v10y, v10z,
                                v01x, v01y, v01z,
                                layer, relY, skyColor
                            );
                        } else {
                            VertexBuilder.triangle(
                                bb,
                                v01x, v01y, v01z,
                                v10x, v10y, v10z,
                                v00x, v00y, v00z,
                                layer, relY, skyColor
                            );
                        }
                    }
                }
            }

            float[] tmp = prevRow;
            prevRow = currRow;
            currRow = tmp;
        }
    }
}
