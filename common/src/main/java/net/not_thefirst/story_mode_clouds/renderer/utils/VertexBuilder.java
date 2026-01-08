package net.not_thefirst.story_mode_clouds.renderer.utils;

import java.util.List;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.math.ColorUtils;

public class VertexBuilder {
    private static CloudsConfiguration CONFIG = CloudsConfiguration.INSTANCE;
    
    public static void quad(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float r, float g, float b, float a) {
        
        bb.addVertex(x0, y0, z0).setColor(r, g, b, a);
        bb.addVertex(x1, y1, z1).setColor(r, g, b, a);
        bb.addVertex(x2, y2, z2).setColor(r, g, b, a);
        bb.addVertex(x3, y3, z3).setColor(r, g, b, a);
    }

    public static void triangle(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float r, float g, float b, float a) {

        quad(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, r, g, b, a);
    }

    public static int[] decomposeARGB(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return new int[]{a, r, g, b};
    }

    public static int[] decomposeRGBA(int color) {
        int r = (color >> 24) & 0xFF;
        int g = (color >> 16) & 0xFF;
        int b = (color >> 8) & 0xFF;
        int a = color & 0xFF;
        return new int[]{r, g, b, a};
    }

    public static void quadColored(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3) {

        bb.addVertex(x0, y0, z0).setColor(c0);
        bb.addVertex(x1, y1, z1).setColor(c1);
        bb.addVertex(x2, y2, z2).setColor(c2);
        bb.addVertex(x3, y3, z3).setColor(c3);
    }

    public static void triangleColored(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2) {
        quadColored(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    public static void quadPreshaded(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float r, float g, float b, float a, 
        List<DiffuseLight> diffuseLights) {
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);

        for (int i = 0; i < 4; i++) {
            float shade = calculateDiffuseShade(normals[i], diffuseLights);
            float[] pos = getVertexPosition(i, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
            bb.addVertex(pos[0], pos[1], pos[2]).setColor(r * shade, g * shade, b * shade, a);
        }
    }

    public static void trianglePreshaded(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float r, float g, float b, float a,
        List<DiffuseLight> diffuseLights) {
        quadPreshaded(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, r, g, b, a, diffuseLights);
    }

    public static void quadColoredPreshaded(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3,
        List<DiffuseLight> diffuseLights) {
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        int[] colors = {c0, c1, c2, c3};

        for (int i = 0; i < 4; i++) {
            float shade = calculateDiffuseShade(normals[i], diffuseLights);
            float[] pos = getVertexPosition(i, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3); 
            bb.addVertex(pos[0], pos[1], pos[2])
                .setColor(multiplyColor(colors[i], shade));
        }
    }

    public static void triangleColoredPreshaded(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2,
        List<DiffuseLight> diffuseLights) {
        quadColoredPreshaded(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2, diffuseLights);
    }

    public static void quadNormal(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int c0, int c1, int c2, int c3) {
        float[][] normals = computeNormals(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);

        for (int i = 0; i < 4; i++) {
            float[] pos = getVertexPosition(i, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
            int color = getVertexColor(i, c0, c1, c2, c3);
            bb.addVertex(pos[0], pos[1], pos[2])
                .setNormal(normals[i][0], normals[i][1], normals[i][2])
                .setColor(color)
                ;
        }
    }

    public static void triangleNormal(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int c0, int c1, int c2) {
        quadNormal(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    private static float calculateDiffuseShade(float[] normal, List<DiffuseLight> diffuseLights) {
        float totalLighting = 0f;
        
        for (DiffuseLight light : diffuseLights) {
            float dot = normal[0] * light.direction.x() +
                        normal[1] * light.direction.y() +
                        normal[2] * light.direction.z();
            totalLighting += Math.max(dot, 0f) * light.intensity;
        }

        final float AMBIENT_LIGHT = CONFIG.LIGHTING.AMBIENT_LIGHTING_STRENGTH;
        final float MAX_SHADED = CONFIG.LIGHTING.MAX_LIGHTING_SHADING;
        
        return Math.min(MAX_SHADED, AMBIENT_LIGHT + totalLighting);
    }

    private static int multiplyColor(int color, float shade) {
        int a = (color >> 24) & 0xFF;
        int r = (int)((color >> 16 & 0xFF) * shade);
        int g = (int)((color >> 8 & 0xFF) * shade);
        int b = (int)((color & 0xFF) * shade);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float[] getVertexPosition(int index, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3) {

        switch (index) {
            case 0: return new float[]{x0, y0, z0};
            case 1: return new float[]{x1, y1, z1};
            case 2: return new float[]{x2, y2, z2};
            case 3: return new float[]{x3, y3, z3};
        }
        return new float[]{0, 0, 0};
    }

    private static int getVertexColor(int index, int c0, int c1, int c2, int c3) {
        int result = 0;
        switch (index) {
            case 0: result = c0; break;
            case 1: result = c1; break;
            case 2: result = c2; break;
            case 3: result = c3; break;
            default: result = 0; break;
        }
        return result;
    }

    private static float[][] computeNormals(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3
    ) {
        float[][] edges = computeEdges(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        float[][] normals = new float[4][3];

        for (int i = 0; i < 4; i++) {
            float[] n = crossProduct(edges[i * 2], edges[i * 2 + 1]);
            normalize(n);
            normals[i] = n;
        }
        return normals;
    }

    private static float[][] computeEdges(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3
    ) {
        return new float[][] {
            {x1 - x0, y1 - y0, z1 - z0}, {x3 - x0, y3 - y0, z3 - z0},
            {x2 - x1, y2 - y1, z2 - z1}, {x0 - x1, y0 - y1, z0 - z1},
            {x3 - x2, y3 - y2, z3 - z2}, {x1 - x2, y1 - y2, z1 - z2},
            {x0 - x3, y0 - y3, z0 - z3}, {x2 - x3, y2 - y3, z2 - z3}
        };
    }

    private static float[] crossProduct(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    private static void normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (Float.isFinite(len) && len > 0) {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        }
    }

    public static void quad(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, int layer, float relY, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration =
            CloudsConfiguration.INSTANCE.getLayer(layer);

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = layerConfiguration.APPEARANCE.BASE_ALPHA / 255.0f;

        if (layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR) {
            r = ARGB.redFloat(layerConfiguration.APPEARANCE.LAYER_COLOR);
            g = ARGB.greenFloat(layerConfiguration.APPEARANCE.LAYER_COLOR);
            b = ARGB.blueFloat(layerConfiguration.APPEARANCE.LAYER_COLOR);
        }

        int color = ARGB.colorFromFloat(a, r, g, b);

        quadNormal(
            bb,
            x0, y0, z0,
            x1, y1, z1,
            x2, y2, z2,
            x3, y3, z3,
            ARGB.WHITE, ARGB.WHITE, ARGB.WHITE, ARGB.WHITE
        );
    }

    public static void triangle(BuildingMesh bb, float x0, float y0, float z0,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int layer, float relY, int skyColor) {

        quad(bb, x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, layer, relY, skyColor);
    }
}
