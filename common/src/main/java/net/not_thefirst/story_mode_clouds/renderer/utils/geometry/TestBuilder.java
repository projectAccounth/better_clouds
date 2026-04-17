package net.not_thefirst.story_mode_clouds.renderer.utils.geometry;

import org.lwjgl.opengl.GL11;

import net.not_thefirst.lib.gl_render_system.mesh.IndexedBuildingMesh;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

public final class TestBuilder {

    private TestBuilder() {}

    public static IndexedBuildingMesh buildCube(float size) {
        float s = size * 0.5f;

        IndexedBuildingMesh mesh =
            new IndexedBuildingMesh(
                VertexFormat.POSITION_COLOR_NORMAL,
                GL11.GL_TRIANGLES,
                IndexedBuildingMesh.IndexPattern.QUADS
            );

        // +X
        face(mesh,
            s, -s, -s,
            s,  s, -s,
            s,  s,  s,
            s, -s,  s,
            1, 0, 0,
            0xFFFF00FF
        );

        // -X
        face(mesh,
            -s, -s,  s,
            -s,  s,  s,
            -s,  s, -s,
            -s, -s, -s,
            -1, 0, 0,
            0xFF00FFFF
        );

        // +Y
        face(mesh,
            -s,  s, -s,
            -s,  s,  s,
             s,  s,  s,
             s,  s, -s,
            0, 1, 0,
            0xFF0000FF
        );

        // -Y
        face(mesh,
            -s, -s,  s,
            -s, -s, -s,
             s, -s, -s,
             s, -s,  s,
            0, -1, 0,
            0xFFFFFFFF
        );

        // +Z
        face(mesh,
            -s, -s,  s,
             s, -s,  s,
             s,  s,  s,
            -s,  s,  s,
            0, 0, 1,
            0xFFFF00FF
        );

        // -Z
        face(mesh,
             s, -s, -s,
            -s, -s, -s,
            -s,  s, -s,
             s,  s, -s,
            0, 0, -1,
            0xFF00FFFF
        );

        return mesh;
    }

    private static void face(
        IndexedBuildingMesh mesh,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float nx, float ny, float nz,
        int color
    ) {
        mesh.addVertex(x0, y0, z0).setColor(color).setNormal(nx, ny, nz);
        mesh.addVertex(x1, y1, z1).setColor(color).setNormal(nx, ny, nz);
        mesh.addVertex(x2, y2, z2).setColor(color).setNormal(nx, ny, nz);
        mesh.addVertex(x3, y3, z3).setColor(color).setNormal(nx, ny, nz);
    }
}