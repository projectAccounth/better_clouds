package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import org.lwjgl.opengl.GL11;

public final class CullState implements RenderState {

    private final boolean enabled;

    public static final CullState CULL = new CullState(true);
    public static final CullState NO_CULL = new CullState(false);

    private CullState(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void apply() {
        if (enabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
    }

    @Override
    public void clear() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
