package net.not_thefirst.story_mode_clouds.renderer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;

public final class GeometryUtils {

    /**
     * Builds a cylindrical bevel strip between two parallel edges.
     *
     * The arc is swept in the plane defined by (normal, sweepDir).
     * The strip is extruded along edgeDir.
     */
    public static void buildCylindricalStrip(
        BufferBuilder bb,
        float edgeStartX, float edgeStartY, float edgeStartZ,
        float edgeEndX,   float edgeEndY,   float edgeEndZ,
        float normalX, float normalY, float normalZ,
        float sweepX,  float sweepY,  float sweepZ,
        float radius,
        int segments,
        boolean flip,
        float r, float g, float b, float a
    ) {
        final float HALF_PI = 3.1415926535f * 0.5f;

        float edgeDX = edgeEndX - edgeStartX;
        float edgeDY = edgeEndY - edgeStartY;
        float edgeDZ = edgeEndZ - edgeStartZ;

        for (int i = 0; i < segments; i++) {
            float t0 = (float)i / segments;
            float t1 = (float)(i + 1) / segments;

            float a0 = t0 * HALF_PI;
            float a1 = t1 * HALF_PI;

            float n0 = (float) Math.cos(a0) * radius;
            float n1 = (float) Math.cos(a1) * radius;
            float s0 = (float) Math.sin(a0) * radius;
            float s1 = (float) Math.sin(a1) * radius;

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
                VertexBuilder.quad(bb,
                    ax0, ay0, az0,
                    ax1, ay1, az1,
                    bx1, by1, bz1,
                    bx0, by0, bz0,
                    r, g, b, a
                );
            } else {
                VertexBuilder.quad(bb,
                    bx0, by0, bz0,
                    bx1, by1, bz1,
                    ax1, ay1, az1,
                    ax0, ay0, az0,
                    r, g, b, a
                );
            }
        }
    }

    private static Vector3f emit(
        float px, float py, float pz,
        float cx, float cy, float cz,
        
        float axx, float axy, float axz,
        float ayx, float ayy, float ayz,
        float azx, float azy, float azz,
        
        float radius) {
        float len = (float) Math.sqrt(px * px + py * py + pz * pz);
        px = px / len * radius;
        py = py / len * radius;
        pz = pz / len * radius;

        // Return world coordinates
        return new Vector3f(
            cx + axx * px + ayx * py + azx * pz,
            cy + axy * px + ayy * py + azy * pz,
            cz + axz * px + ayz * py + azz * pz
        );
    }

    /**
     * Builds a spherical corner patch.
     *
     * Produces (n^2 - n + 1) polygons.
     */
    public static void buildSphericalCorner(
        BufferBuilder bb,
        float cx, float cy, float cz,

        float axx, float axy, float axz,
        float ayx, float ayy, float ayz,
        float azx, float azy, float azz,

        float radius,
        int segments,
        boolean flip,

        float r, float g, float b, float a
    ) {
        // Triangular grid
        List<List<Vector3f>> grid = new ArrayList<>(segments + 1);

        for (int i = 0; i <= segments; i++) {
            grid.add(new ArrayList<>(segments - i + 1));
        }

        for (int i = 0; i <= segments; i++) {
            float u = (float)i / segments;

            int rowCount = segments - i;

            for (int j = 0; j <= rowCount; j++) {
                float v = (float)j / segments;

                float px = u;
                float py = 1.0f - u - v;
                float pz = v;

                grid.get(i).add(emit(
                    px, py, pz, 
                    cx, cy, cz, 
                    axx, axy, axz, 
                    ayx, ayy, ayz, 
                    azx, azy, azz, 
                    radius));
            }
        }

        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < (int)grid.get(i).size() - 1; j++) {
                Vector3f v00 = grid.get(i).get(j);
                Vector3f v10 = grid.get(i + 1).get(j);
                Vector3f v01 = grid.get(i).get(j + 1);

                if (j + 1 < (int)grid.get(i + 1).size()) {
                    Vector3f v11 = grid.get(i + 1).get(j + 1);

                    if (!flip) {
                        VertexBuilder.quad(
                            bb,
                            v00.x, v00.y, v00.z,
                            v10.x, v10.y, v10.z,
                            v11.x, v11.y, v11.z,
                            v01.x, v01.y, v01.z,
                            r, g, b, a
                        );
                    } else {
                        VertexBuilder.quad(
                            bb,
                            v01.x, v01.y, v01.z,
                            v11.x, v11.y, v11.z,
                            v10.x, v10.y, v10.z,
                            v00.x, v00.y, v00.z,
                            r, g, b, a
                        );
                    }
                } else {
                    // Final triangle
                    if (!flip) {
                        VertexBuilder.triangle(
                            bb,
                            v00.x, v00.y, v00.z,
                            v10.x, v10.y, v10.z,
                            v01.x, v01.y, v01.z,
                            r, g, b, a
                        );
                    } else {
                        VertexBuilder.triangle(
                            bb,
                            v01.x, v01.y, v01.z,
                            v10.x, v10.y, v10.z,
                            v00.x, v00.y, v00.z,
                            r, g, b, a
                        );
                    }
                }
            }
        }
    }

}
