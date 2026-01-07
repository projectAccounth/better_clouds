package net.not_thefirst.story_mode_clouds.renderer.render_system.mesh;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;

public final class MeshUploader {

    private MeshUploader() {}

    public static GpuMesh upload(CompiledMesh mesh) {
        VertexFormat format = mesh.format();
        int vertexCount = mesh.vertexCount();

        int stride = format.strideBytes();
        int totalBytes = vertexCount * stride;

        ByteBuffer buffer = BufferUtils.createByteBuffer(totalBytes);

        float[] pos = mesh.positions();
        float[] nor = mesh.normals();
        float[] uv  = mesh.uvs();
        int[]   col = mesh.colors();

        for (int i = 0; i < vertexCount; i++) {
            format.putVertex(buffer, i, pos, nor, uv, col);
        }

        buffer.flip();

        int vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        format.enable();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        assert buffer.position() == vertexCount * format.strideBytes();

        return new GpuMesh(
            vao,
            vbo,
            vertexCount,
            mesh.mode(),
            format
        );
    }
}
