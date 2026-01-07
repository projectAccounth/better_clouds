package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.GLProgram;

public final class ShaderRenderType extends RenderType {

    private final ShaderState shader;

    public ShaderRenderType(
        String name,
        ShaderState shader,
        CompositeRenderState state
    ) {
        super(name, merge(shader, state));
        this.shader = shader;
    }

    private static CompositeRenderState merge(
        ShaderState shader,
        CompositeRenderState state
    ) {
        RenderState[] merged = new RenderState[stateStates(state).length + 1];
        merged[0] = shader;
        System.arraycopy(stateStates(state), 0, merged, 1, stateStates(state).length);
        return new CompositeRenderState(merged);
    }

    static RenderState[] stateStates(CompositeRenderState s) {
        try {
            var f = CompositeRenderState.class.getDeclaredField("states");
            f.setAccessible(true);
            return (RenderState[]) f.get(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public GLProgram program() {
        return shader.program();
    }
}