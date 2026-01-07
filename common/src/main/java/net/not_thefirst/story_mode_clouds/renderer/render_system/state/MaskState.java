package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import org.lwjgl.opengl.GL11;

public class MaskState implements RenderState {
    private final boolean writeColor;
    private final boolean writeDepth;

    public static final MaskState COLOR_DEPTH = new MaskState(true, true);
    public static final MaskState COLOR_NO_DEPTH = new MaskState(true, false);
    public static final MaskState DEPTH_ONLY = new MaskState(false, true);
    public static final MaskState NONE = new MaskState(false, false);

    public MaskState(boolean color, boolean depth) {
        this.writeColor = color;
        this.writeDepth = depth;
    }

    @Override
    public void apply() {
        GL11.glDepthMask(this.writeDepth);
        GL11.glColorMask(this.writeColor, this.writeColor, this.writeColor, this.writeColor);
    }

    @Override
    public void clear() {
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
    }

    public boolean writeColor() {
        return this.writeColor;
    }

    public boolean writeDepth() { 
        return this.writeDepth;
    }
}
