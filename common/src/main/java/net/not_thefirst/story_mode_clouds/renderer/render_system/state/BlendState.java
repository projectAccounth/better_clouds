package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import org.lwjgl.opengl.GL11;

public final class BlendState implements RenderState {

    private final int src;
    private final int dst;

    public static final BlendState NONE =
        new BlendState(-1, -1);

    public static final BlendState TRANSLUCENT =
        new BlendState(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    public BlendState(int src, int dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void apply() {
        if (src < 0) {
            GL11.glDisable(GL11.GL_BLEND);
        } else {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(src, dst);
        }
    }

    @Override
    public void clear() {
        GL11.glDisable(GL11.GL_BLEND);
    }
}