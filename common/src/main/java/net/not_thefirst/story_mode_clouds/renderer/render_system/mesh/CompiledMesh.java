package net.not_thefirst.story_mode_clouds.renderer.render_system.mesh;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;

// for the scripting engine
public final class CompiledMesh {
    final float[] positions;
    final float[] normals;
    final float[] uvs;
    final int[] colors;
    final int vertexCount;

    final VertexFormat format;
    final int mode;

    CompiledMesh(
        float[] positions,
        float[] normals,
        float[] uvs,
        int[] colors,
        int vertexCount,
        VertexFormat format,
        int mode
    ) {
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.colors = colors;
        this.vertexCount = vertexCount;
        this.format = format;
        this.mode = mode;
    }

    public int mode() { return this.mode; }
    public VertexFormat format() { return this.format; }
    public float[] positions() { return this.positions; }
    public float[] normals() { return this.normals; }
    public float[] uvs() { return this.uvs; }
    public int[] colors() { return this.colors; }
    public int vertexCount() { return this.vertexCount; }
}
