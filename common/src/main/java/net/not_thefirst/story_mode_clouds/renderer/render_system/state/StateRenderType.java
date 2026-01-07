package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

public final class StateRenderType extends RenderType {

    public StateRenderType(String name, CompositeRenderState state) {
        super(name, state);
    }

    public void run(Runnable drawCall) {
        setup();
        drawCall.run();
        clear();
    }
}
