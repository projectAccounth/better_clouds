package net.not_thefirst.story_mode_clouds.utils.rendering;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class PipelineManager {
    private static final PipelineManager INSTANCE = new PipelineManager();
    private static final PipelineProvider CURRENT_PROVIDER = PipelineProvider.BLAZE3D;

    // short: from 26.2 onwards, a reversed depth buffer is used
    public static final DepthBufferImplType DEPTH_BUFFER_IMPL = DepthBufferImplType.REVERSED_Z;

    /**
     * Table structure:
     * - Row key: pipeline name, String
     * - Column key: Pipeline provider of type {@link PipelineProvider}
     * - Value: Corresponding {@link AbstractPipeline} instance
     */
    private final Table<String, PipelineProvider, AbstractPipeline> pipelineTable = HashBasedTable.create();
    
    private PipelineManager() {}

    public void init() {
        // setup all abstract pipelines
        for (PipelineProvider provider : PipelineProvider.values()) {
            for (AbstractPipeline pipeline : INSTANCE.pipelineTable.column(provider).values()) {
                pipeline.setup();
            }
        }
    }

    public static PipelineManager getInstance() {
        return INSTANCE;
    }

    public void registerPipeline(AbstractPipeline pipeline) {
        registerPipeline(pipeline, CURRENT_PROVIDER);
    }

    public void registerPipeline(AbstractPipeline pipeline, PipelineProvider provider) {
        pipelineTable.put(pipeline.getName(), provider, pipeline);
    }

    public void registerPipelines(AbstractPipeline... pipelines) {
        for (AbstractPipeline pipeline : pipelines) {
            registerPipeline(pipeline);
        }
    }

    public void registerPipelines(PipelineProvider provider, AbstractPipeline... pipelines) {
        for (AbstractPipeline pipeline : pipelines) {
            registerPipeline(pipeline, provider);
        }
    }


    public AbstractPipeline getPipeline(String name) {
        return pipelineTable.get(name, CURRENT_PROVIDER);
    }

    public enum PipelineProvider {
        BLAZE3D, RAW_OGL
    }

    public enum DepthBufferImplType {
        STANDARD, REVERSED_Z
    }
}
