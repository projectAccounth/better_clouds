package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import org.lwjgl.opengl.GL11;

public class ShadeState implements RenderState {

    public static final ShadeState SMOOTH = new ShadeState(GL11.GL_SMOOTH);
    public static final ShadeState FLAT = new ShadeState(GL11.GL_FLAT);

    private final int type;

    public ShadeState(int type) {
        this.type = type;
    }

    @Override
    public void apply() {
        GL11.glShadeModel(this.type);
    }

    @Override
    public void clear() {
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }
}
