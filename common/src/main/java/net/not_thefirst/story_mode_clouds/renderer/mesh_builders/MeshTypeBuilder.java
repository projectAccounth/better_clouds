package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;
import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.BuildingMesh;
import net.not_thefirst.story_mode_clouds.utils.Texture;

public interface MeshTypeBuilder {
    BuildingMesh Build(
        BuildingMesh bb, Texture.TextureData tex, 
        RelativeCameraPos pos, LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor,
        float chunkOffX, float chunkOffZ);
}