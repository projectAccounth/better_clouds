package net.not_thefirst.story_mode_clouds.utils.rendering;

public class PipelineManager {
    private static final PipelineManager INSTANCE = new PipelineManager();

    private PipelineManager() {}

    public static PipelineManager getInstance() {
        return INSTANCE;
    }
}
