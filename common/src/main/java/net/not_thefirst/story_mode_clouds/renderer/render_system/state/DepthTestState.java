package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import org.lwjgl.opengl.GL11;

public final class DepthTestState implements RenderState {

    private final int func;

    public static final DepthTestState LEQUAL =
        new DepthTestState(GL11.GL_LEQUAL);

    public static final DepthTestState GEQUAL =
        new DepthTestState(GL11.GL_GEQUAL);

    public static final DepthTestState ALWAYS =
        new DepthTestState(GL11.GL_ALWAYS);

    public DepthTestState(int func) {
        this.func = func;
    }

    @Override
    public void apply() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(func);
    }

    @Override
    public void clear() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }
}
