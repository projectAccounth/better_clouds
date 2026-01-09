package net.not_thefirst.story_mode_clouds.renderer.render_system.mesh;

import java.util.Arrays;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;

public final class BuildingMesh {

    private float[] positions;
    private float[] normals;
    private float[] uvs;
    private int[] colors;

    private int capacity;
    private int currentIndex;

    private boolean vertexOpen = false;

    private final VertexFormat format;
    private final int mode;

    public BuildingMesh(VertexFormat format, int mode) {
        this.format = format;
        this.mode = mode;

        this.capacity = 1024;

        this.positions = new float[capacity * 3];
        this.normals   = new float[capacity * 3];
        this.uvs       = new float[capacity * 2];
        this.colors    = new int[capacity];
    }

    private void ensureCapacity(int additionalVertices) {
        int required = currentIndex + additionalVertices;
        if (required <= capacity)
            return;

        int newCapacity = capacity;
        while (newCapacity < required) {
            newCapacity = newCapacity + (newCapacity >> 1);
        }

        positions = Arrays.copyOf(positions, newCapacity * 3);
        normals   = Arrays.copyOf(normals,   newCapacity * 3);
        uvs       = Arrays.copyOf(uvs,       newCapacity * 2);
        colors    = Arrays.copyOf(colors,    newCapacity);

        capacity = newCapacity;
    }

    private void beginVert() {
        if (vertexOpen) {
            currentIndex++;
            vertexOpen = false;
        }
    }

    public BuildingMesh addVertex(float x, float y, float z) {
        beginVert();
        ensureCapacity(1);

        int p = currentIndex * 3;
        positions[p]     = x;
        positions[p + 1] = y;
        positions[p + 2] = z;

        vertexOpen = true;
        return this;
    }

    public BuildingMesh setNormal(float nx, float ny, float nz) {
        if (!vertexOpen) throw new IllegalStateException("Cannot set normal without starting a vertex");
        int p = currentIndex * 3;
        normals[p]     = nx;
        normals[p + 1] = ny;
        normals[p + 2] = nz;
        return this;
    }

    public BuildingMesh setUv(float u, float v) {
        if (!vertexOpen) throw new IllegalStateException("Cannot set UV without starting a vertex");
        int p = currentIndex * 2;
        uvs[p]     = u;
        uvs[p + 1] = v;
        return this;
    }

    public BuildingMesh setColor(int color) {
        if (!vertexOpen) throw new IllegalStateException("Cannot set color without starting a vertex");
        colors[currentIndex] = color;
        return this;
    }

    public BuildingMesh setColor(float r, float g, float b, float a) {
        int ir = Math.min(255, Math.max(0, (int)(r * 255.0f)));
        int ig = Math.min(255, Math.max(0, (int)(g * 255.0f)));
        int ib = Math.min(255, Math.max(0, (int)(b * 255.0f)));
        int ia = Math.min(255, Math.max(0, (int)(a * 255.0f)));

        int color = ARGB.color(ia, ir, ig, ib);
        return setColor(color);
    }

    public BuildingMesh endVertex() {
        beginVert();
        return this;
    }

    public void reset() {
        currentIndex = 0;
        vertexOpen = false;
    }

    public int capacity() {
        return capacity;
    }

    public int vertexCount() {
        return currentIndex + (vertexOpen ? 1 : 0);
    }

    public CompiledMesh compile() {
        beginVert();

        int count = currentIndex;

        float[] finalPositions = Arrays.copyOf(positions, count * 3);
        float[] finalNormals   = Arrays.copyOf(normals,   count * 3);
        float[] finalUvs       = Arrays.copyOf(uvs,       count * 2);
        int[]   finalColors    = Arrays.copyOf(colors,    count);

        return new CompiledMesh(
            finalPositions,
            finalNormals,
            finalUvs,
            finalColors,
            count,
            format,
            mode
        );
    }

    
}
