package net.not_thefirst.story_mode_clouds.mesh_builder_api.natives;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.LayerState;

public interface MeshTypeBuilder {
    AbstractStaticMesh.Builder<?, ?> build(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz,
        int currentLayer, int colorModifier);

    AbstractStaticMesh.Builder<?, ?> buildOutline(
        AbstractStaticMesh.Builder<?, ?> bb,
        LayerState state,
        int cx, int cz,  
        int currentLayer, int colorModifier);
}