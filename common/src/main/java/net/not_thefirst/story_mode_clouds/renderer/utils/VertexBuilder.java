package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;

public class VertexBuilder {
    public static void triangle(
        BufferBuilder bb,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float r, float g, float b, float a
    ) {
        quad(
            bb,
            x0, y0, z0,
            x1, y1, z1,
            x2, y2, z2,
            x2, y2, z2,
            r, g, b, a
        );
    }

    public static void quad(
        BufferBuilder bb,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float r, float g, float b, float a
    ) {
        bb.addVertex(x0, y0, z0).setColor(r, g, b, a);
        bb.addVertex(x1, y1, z1).setColor(r, g, b, a);
        bb.addVertex(x2, y2, z2).setColor(r, g, b, a);
        bb.addVertex(x3, y3, z3).setColor(r, g, b, a);
    }

    public static void quadColored(
        BufferBuilder bb,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int color0, int color1, int color2, int color3
    ) {
        bb.addVertex(x0, y0, z0).setColor(color0);
        bb.addVertex(x1, y1, z1).setColor(color1);
        bb.addVertex(x2, y2, z2).setColor(color2);
        bb.addVertex(x3, y3, z3).setColor(color3);
    }
}
