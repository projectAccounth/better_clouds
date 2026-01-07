package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

public class RenderType {

    private final String name;
    private final CompositeRenderState state;

    public RenderType(String name, CompositeRenderState state) {
        this.name = name;
        this.state = state;
    }

    public void setup() {
        state.apply();
    }

    public void clear() {
        state.clear();
    }

    @Override
    public String toString() {
        return "RenderType[" + name + "]";
    }
}

