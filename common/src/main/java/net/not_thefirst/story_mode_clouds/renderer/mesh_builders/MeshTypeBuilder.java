package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;
import com.mojang.blaze3d.vertex.BufferBuilder;

public interface MeshTypeBuilder {
    BufferBuilder build(
        BufferBuilder bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor);
}