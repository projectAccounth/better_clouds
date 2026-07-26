package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.RenderPassFactory;

public class B3DRenderPassFactory implements RenderPassFactory<B3DPipeline, B3DRenderPass> {
    public B3DRenderPassFactory() { /* */}
    
    @Override
    public Class<B3DPipeline> getPipelineClass() {
        return B3DPipeline.class;
    }
    
    @Override
    public B3DRenderPass create(String name, AbstractPipeline[] pipelines) {
        verifyPipelines(pipelines);
        B3DPipeline[] converted = new B3DPipeline[pipelines.length];
        for (int i = 0; i < pipelines.length; ++i) converted[i] = (B3DPipeline) pipelines[i];
        return new B3DRenderPass(name, converted);
    }
}