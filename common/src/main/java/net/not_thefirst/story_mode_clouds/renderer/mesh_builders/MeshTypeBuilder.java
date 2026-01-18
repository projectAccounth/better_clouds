package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.BuildingMesh;

public interface MeshTypeBuilder {
    BuildingMesh build(
        BuildingMesh bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor);
}