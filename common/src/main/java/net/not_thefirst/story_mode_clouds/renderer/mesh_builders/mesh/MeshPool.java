package net.not_thefirst.story_mode_clouds.renderer.mesh_builders.mesh;

import java.util.ArrayDeque;

import com.mojang.blaze3d.vertex.VertexFormat;

public final class MeshPool {

    private final ArrayDeque<BuildingMesh> pool = new ArrayDeque<>();

    private final VertexFormat format;
    private final VertexFormat.Mode mode;

    MeshPool(VertexFormat format, VertexFormat.Mode mode) {
        this.format = format;
        this.mode = mode;
    }

    public BuildingMesh acquire() {
        BuildingMesh BuildingMesh = pool.pollFirst();
        if (BuildingMesh != null) {
            BuildingMesh.reset();
            return BuildingMesh;
        }
        return new BuildingMesh(format, mode);
    }

    public void release(BuildingMesh BuildingMesh) {
        pool.addFirst(BuildingMesh);
    }
}
