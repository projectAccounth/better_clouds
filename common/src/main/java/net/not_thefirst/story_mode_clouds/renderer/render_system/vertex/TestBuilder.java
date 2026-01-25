package net.not_thefirst.story_mode_clouds.renderer.render_system.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;

public class TestBuilder {
    private static void addQuad(
        BufferBuilder mesh,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float nx, float ny, float nz,
        float r, float g, float b, float a
    ) {
        mesh.addVertex(x0, y0, z0).setNormal(nx, ny, nz).setColor(r, g, b, a);
        mesh.addVertex(x1, y1, z1).setNormal(nx, ny, nz).setColor(r, g, b, a);
        mesh.addVertex(x2, y2, z2).setNormal(nx, ny, nz).setColor(r, g, b, a);

        mesh.addVertex(x0, y0, z0).setNormal(nx, ny, nz).setColor(r, g, b, a);
        mesh.addVertex(x2, y2, z2).setNormal(nx, ny, nz).setColor(r, g, b, a);
        mesh.addVertex(x3, y3, z3).setNormal(nx, ny, nz).setColor(r, g, b, a);
    }

    public static void addUnitCube(BufferBuilder mesh) {
        float r = (float)Math.random();
        float g = (float)Math.random();
        float b = (float)Math.random();
        float a = 1.0f;

        float min = -0.5f;
        float max =  0.5f;

        addQuad(mesh,
            max, min, min,
            max, max, min,
            max, max, max,
            max, min, max,
            1.0f, 0.0f, 0.0f,
            r, g, b, a
        );

        addQuad(mesh,
            min, min, max,
            min, max, max,
            min, max, min,
            min, min, min,
        -1.0f, 0.0f, 0.0f,
            r, g, b, a
        );

        addQuad(mesh,
            min, max, min,
            min, max, max,
            max, max, max,
            max, max, min,
            0.0f, 1.0f, 0.0f,
            r, g, b, a
        );

        addQuad(mesh,
            min, min, max,
            min, min, min,
            max, min, min,
            max, min, max,
            0.0f,-1.0f, 0.0f,
            r, g, b, a
        );

        addQuad(mesh,
            max, min, max,
            max, max, max,
            min, max, max,
            min, min, max,
            0.0f, 0.0f, 1.0f,
            r, g, b, a
        );

        addQuad(mesh,
            min, min, min,
            min, max, min,
            max, max, min,
            max, min, min,
            0.0f, 0.0f,-1.0f,
            r, g, b, a
        );
    }

}
