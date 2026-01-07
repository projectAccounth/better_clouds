package net.not_thefirst.story_mode_clouds.renderer.render_system.mesh;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;

public final class GpuMesh implements AutoCloseable {

    private final int vao;
    private final int vbo;

    private final int vertexCount;
    private final int drawMode;
    private final VertexFormat format;

    public GpuMesh(
        int vao,
        int vbo,
        int vertexCount,
        int drawMode,
        VertexFormat format
    ) {
        this.vao = vao;
        this.vbo = vbo;
        this.vertexCount = vertexCount;
        this.drawMode = drawMode;
        this.format = format;
    }

    public void draw() {
        format.enable();
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(drawMode, 0, vertexCount);
        GL30.glBindVertexArray(0);
        format.disable();
    }

    @Override
    public void close() {
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
    }
}
