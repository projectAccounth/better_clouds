package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;

public abstract class PipelineAllocator {

    protected PipelineAllocator() {}

    private static PipelineAllocator INSTANCE;

    public static PipelineAllocator getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("PipelineAllocator not initialized");
        }
        return INSTANCE;
    }

    public static void initializeAllocator(PipelineAllocator allocator) {
        if (INSTANCE != null) {
            throw new IllegalStateException("PipelineAllocator already initialized");
        }
        INSTANCE = allocator;
        INSTANCE.initialize();
    }

    public abstract void initialize();
    public abstract AbstractPipeline.Builder<?> createBuilder(String name);
    public abstract void registerPipeline(AbstractPipeline pipeline);
    public abstract AbstractPipeline getPipeline(String name);
}
