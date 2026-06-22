package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;

public interface MeshTypeBuilder {
    AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz, float relY, 
        int currentLayer, int skyColor);
}