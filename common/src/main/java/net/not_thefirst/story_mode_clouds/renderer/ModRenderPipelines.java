package net.not_thefirst.story_mode_clouds.renderer;

import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.state.RenderState;
import net.not_thefirst.lib.gl_render_system.state.RenderStateBuilder;
public class ModRenderPipelines {
    private ModRenderPipelines() {}

    // enabled on render end to reset 
    public static final RenderState DEFAULT_RESET_STATE = new RenderStateBuilder()
        .blend(BlendState.TRANSLUCENT)
        .cull(CullState.CULL)
        .depthTest(DepthTestState.LEQUAL)
        .mask(MaskState.COLOR_DEPTH)
        .build();
}