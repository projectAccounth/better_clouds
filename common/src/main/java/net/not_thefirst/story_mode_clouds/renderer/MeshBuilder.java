package net.not_thefirst.story_mode_clouds.renderer;

public class MeshBuilder {
    public static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    public static final float HEIGHT_IN_BLOCKS = 4.0F;

    public static final CloudShape SHAPE     = CloudShape.CUBE;
    public static final PuffMode   PUFF_MODE = PuffMode.SCATTERED;

    public enum CloudShape {
        CUBE,
        CROSS
    }

    public enum PuffMode {
        SCATTERED,
        COMPACT
    }
}
