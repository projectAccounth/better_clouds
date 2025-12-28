package net.not_thefirst.story_mode_clouds.renderer.utils;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.not_thefirst.story_mode_clouds.mixin.LightingAccessor;

public class VertexBuilder {
    private static final float AMBIENT_LIGHT = 0.6f;
    private static final float DIFFUSE_LIGHT_STRENGTH = 0.7f;
    private static final float MAX_SHADED = 0.8f;
    
    // Advanced shading constants
    private static final float RIM_POWER = 4.0f;
    private static final float RIM_STRENGTH = 0.3f;
    private static final float HEMISPHERICAL_STRENGTH = 0.5f;

    public static void triangle(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float r, float g, float b, float a) {
        quad(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, r, g, b, a);
    }

    public static void triangleColored(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2) {
        quadColored(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    public static void trianglePreshaded(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float r, float g, float b, float a) {
        quadPreshaded(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, r, g, b, a);
    }

    public static void triangleColoredPreshaded(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2) {
        quadColoredPreshaded(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    public static void triangleNormal(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2) {
        quadNormal(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    private static int multiplyColor(int color, float shade) {
        int a = (color >> 24) & 0xFF;
        int r = (int)((color >> 16 & 0xFF) * shade);
        int g = (int)((color >> 8 & 0xFF) * shade);
        int b = (int)((color & 0xFF) * shade);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Blends two colors with a factor (0-1).
     */
    private static int blendColors(int color1, int color2, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 * (1 - factor) + a2 * factor);
        int r = (int)(r1 * (1 - factor) + r2 * factor);
        int g = (int)(g1 * (1 - factor) + g2 * factor);
        int b = (int)(b1 * (1 - factor) + b2 * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Applies hemispherical + rim lighting to a vertex shade.
     * Hemispherical lighting blends sky and ground colors based on normal.
     * Rim lighting adds a silhouette glow.
     */
    private static float applyAdvancedShading(float nx, float ny, float nz,
                                             float vx, float vy, float vz,
                                             float baseShade) {
        // Hemispherical influence: upward-facing = more sky, downward = more ground
        float hemisphereInfluence = (ny + 1.0f) * 0.5f; // Map -1..1 to 0..1
        float hemisphereShade = 0.7f + HEMISPHERICAL_STRENGTH * hemisphereInfluence;

        // Rim lighting: silhouette effect
        float viewDot = Math.abs(vx * nx + vy * ny + vz * nz);
        float rim = (float)Math.pow(Math.max(0, 1.0f - viewDot), RIM_POWER) * RIM_STRENGTH;

        return baseShade * hemisphereShade + rim;
    }

    public static void quad(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float r, float g, float b, float a) {
        bb.addVertex(x0, y0, z0).setColor(r, g, b, a);
        bb.addVertex(x1, y1, z1).setColor(r, g, b, a);
        bb.addVertex(x2, y2, z2).setColor(r, g, b, a);
        bb.addVertex(x3, y3, z3).setColor(r, g, b, a);
    }

    public static void quadColored(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3) {
        bb.addVertex(x0, y0, z0).setColor(c0);
        bb.addVertex(x1, y1, z1).setColor(c1);
        bb.addVertex(x2, y2, z2).setColor(c2);
        bb.addVertex(x3, y3, z3).setColor(c3);
    }

    public static void quadPreshaded(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float r, float g, float b, float a) {
        Vector3f light0 = LightingAccessor.getLight0Direction();
        Vector3f light1 = LightingAccessor.getLight1Direction();
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] positions = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};

        for (int i = 0; i < 4; i++) {
            float nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            float l0 = Math.max(0, nx * light0.x() + ny * light0.y() + nz * light0.z());
            float l1 = Math.max(0, nx * light1.x() + ny * light1.y() + nz * light1.z());
            float shade = Math.min(MAX_SHADED, AMBIENT_LIGHT + DIFFUSE_LIGHT_STRENGTH * Math.max(l0, l1));
            bb.addVertex(positions[i][0], positions[i][1], positions[i][2])
                .setColor(r * shade, g * shade, b * shade, a);
        }
    }

    public static void quadColoredPreshaded(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3) {
        Vector3f light0 = LightingAccessor.getLight0Direction();
        Vector3f light1 = LightingAccessor.getLight1Direction();
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] positions = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        int[] colors = {c0, c1, c2, c3};

        for (int i = 0; i < 4; i++) {
            float nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            float l0 = Math.max(0, nx * light0.x() + ny * light0.y() + nz * light0.z());
            float l1 = Math.max(0, nx * light1.x() + ny * light1.y() + nz * light1.z());
            float shade = Math.min(MAX_SHADED, AMBIENT_LIGHT + DIFFUSE_LIGHT_STRENGTH * Math.max(l0, l1));
            bb.addVertex(positions[i][0], positions[i][1], positions[i][2])
                .setColor(multiplyColor(colors[i], shade));
        }
    }

    public static void quadNormal(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3) {
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] positions = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        int[] colors = {c0, c1, c2, c3};

        for (int i = 0; i < 4; i++) {
            bb.addVertex(positions[i][0], positions[i][1], positions[i][2])
                .setNormal(normals[i][0], normals[i][1], normals[i][2])
                .setColor(colors[i]);
        }
    }

    /**
     * Helper: computes and normalizes vertex normals from quad positions.
     */
    private static float[][] computeNormals(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3
    ) {
        float e0ax = x1 - x0, e0ay = y1 - y0, e0az = z1 - z0;
        float e0bx = x3 - x0, e0by = y3 - y0, e0bz = z3 - z0;

        float e1ax = x2 - x1, e1ay = y2 - y1, e1az = z2 - z1;
        float e1bx = x0 - x1, e1by = y0 - y1, e1bz = z0 - z1;

        float e2ax = x3 - x2, e2ay = y3 - y2, e2az = z3 - z2;
        float e2bx = x1 - x2, e2by = y1 - y2, e2bz = z1 - z2;

        float e3ax = x0 - x3, e3ay = y0 - y3, e3az = z0 - z3;
        float e3bx = x2 - x3, e3by = y2 - y3, e3bz = z2 - z3;

        float[][] normals = new float[4][3];
        
        // Cross products and normalization
        float[] n = new float[3];
        for (int i = 0; i < 4; i++) {
            float eax, eay, eaz, ebx, eby, ebz;
            if (i == 0) { eax = e0ax; eay = e0ay; eaz = e0az; ebx = e0bx; eby = e0by; ebz = e0bz; }
            else if (i == 1) { eax = e1ax; eay = e1ay; eaz = e1az; ebx = e1bx; eby = e1by; ebz = e1bz; }
            else if (i == 2) { eax = e2ax; eay = e2ay; eaz = e2az; ebx = e2bx; eby = e2by; ebz = e2bz; }
            else { eax = e3ax; eay = e3ay; eaz = e3az; ebx = e3bx; eby = e3by; ebz = e3bz; }
            
            n[0] = eay * ebz - eaz * eby;
            n[1] = eaz * ebx - eax * ebz;
            n[2] = eax * eby - eay * ebx;
            
            float invLen = (float)(1.0 / Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]));
            if (Float.isFinite(invLen)) {
                normals[i][0] = n[0] * invLen;
                normals[i][1] = n[1] * invLen;
                normals[i][2] = n[2] * invLen;
            }
        }
        return normals;
    }

    /**
     * Helper: computes and normalizes view vectors from camera.
     */
    private static float[][] computeViewVectors(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float camX, float camY, float camZ
    ) {
        float[][] views = new float[4][3];
        float[][] vecs = {
            {camX - x0, camY - y0, camZ - z0},
            {camX - x1, camY - y1, camZ - z1},
            {camX - x2, camY - y2, camZ - z2},
            {camX - x3, camY - y3, camZ - z3}
        };
        for (int i = 0; i < 4; i++) {
            float vlen = (float)Math.sqrt(vecs[i][0]*vecs[i][0] + vecs[i][1]*vecs[i][1] + vecs[i][2]*vecs[i][2]);
            if (vlen > 0) {
                views[i][0] = vecs[i][0] / vlen;
                views[i][1] = vecs[i][1] / vlen;
                views[i][2] = vecs[i][2] / vlen;
            }
        }
        return views;
    }

    /**
     * Advanced shading variant with hemispherical + rim lighting. Float RGB variant.
     */
    public static void quadPreshadedAdvanced(BufferBuilder bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float r, float g, float b, float a,
        float camX, float camY, float camZ) {
        Vector3f diffuseLight0 = LightingAccessor.getLight0Direction();
        Vector3f diffuseLight1 = LightingAccessor.getLight1Direction();

        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] views = computeViewVectors(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, camX, camY, camZ);

        float[][] positions = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        for (int i = 0; i < 4; i++) {
            float nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            float vx = views[i][0], vy = views[i][1], vz = views[i][2];
            
            float light0 = Math.max(0, nx * diffuseLight0.x() + ny * diffuseLight0.y() + nz * diffuseLight0.z());
            float light1 = Math.max(0, nx * diffuseLight1.x() + ny * diffuseLight1.y() + nz * diffuseLight1.z());
            float baseShade = Math.min(MAX_SHADED, AMBIENT_LIGHT + DIFFUSE_LIGHT_STRENGTH * Math.max(light0, light1));
            float shade = applyAdvancedShading(nx, ny, nz, vx, vy, vz, baseShade);
            
            bb.addVertex(positions[i][0], positions[i][1], positions[i][2])
                .setColor(r * shade, g * shade, b * shade, a);
        }
    }

    /**
     * Advanced shading variant with hemispherical + rim lighting. Integer color variant.
     */
    public static void quadColoredPreshadedAdvanced(
        BufferBuilder bb,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int color0, int color1, int color2, int color3,
        float camX, float camY, float camZ
    ) {
        Vector3f diffuseLight0 = LightingAccessor.getLight0Direction();
        Vector3f diffuseLight1 = LightingAccessor.getLight1Direction();

        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] views = computeViewVectors(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, camX, camY, camZ);

        float[][] positions = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        int[] colors = {color0, color1, color2, color3};
        
        for (int i = 0; i < 4; i++) {
            float nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            float vx = views[i][0], vy = views[i][1], vz = views[i][2];
            
            float light0 = Math.max(0, nx * diffuseLight0.x() + ny * diffuseLight0.y() + nz * diffuseLight0.z());
            float light1 = Math.max(0, nx * diffuseLight1.x() + ny * diffuseLight1.y() + nz * diffuseLight1.z());
            float baseShade = Math.min(MAX_SHADED, AMBIENT_LIGHT + DIFFUSE_LIGHT_STRENGTH * Math.max(light0, light1));
            float shade = applyAdvancedShading(nx, ny, nz, vx, vy, vz, baseShade);
            
            bb.addVertex(positions[i][0], positions[i][1], positions[i][2])
                .setColor(multiplyColor(colors[i], shade));
        }
    }
}
