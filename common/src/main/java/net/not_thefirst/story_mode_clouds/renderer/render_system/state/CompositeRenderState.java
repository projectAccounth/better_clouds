package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

public final class CompositeRenderState implements RenderState {

    private final RenderState[] states;

    public CompositeRenderState(RenderState... states) {
        this.states = states;
    }

    @Override
    public void apply() {
        for (RenderState s : states) {
            RenderStateTracker.apply(s);
        }
    }

    @Override
    public void clear() {
        for (int i = states.length - 1; i >= 0; i--) {
            RenderStateTracker.clear(states[i]);
        }
    }
}

