package net.not_thefirst.story_mode_clouds.renderer.render_system.mesh;

import java.util.ArrayDeque;

import net.not_thefirst.story_mode_clouds.renderer.render_system.vertex.VertexFormat;

public final class MeshPool {

    private final ArrayDeque<BuildingMesh> pool = new ArrayDeque<>();

    private final VertexFormat format;
    private final int mode;

    MeshPool(VertexFormat format, int mode) {
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
