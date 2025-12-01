package net.not_thefirst.story_mode_clouds.renderer;

import net.not_thefirst.story_mode_clouds.utils.ARGB;

public class MeshBuilder {
    public static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    public static final float HEIGHT_IN_BLOCKS = 4.0F;

    public static CloudShape SHAPE     = CloudShape.CUBE;
    public static PuffMode   PUFF_MODE = PuffMode.SCATTERED;

    public static enum CloudShape {
        CUBE,
        CROSS
    }

    public static enum PuffMode {
        SCATTERED,
        COMPACT
    }

    public final static int topColor    = ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
    public final static int bottomColor = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
    public final static int sideColor   = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
    public final static int innerColor  = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);
}
