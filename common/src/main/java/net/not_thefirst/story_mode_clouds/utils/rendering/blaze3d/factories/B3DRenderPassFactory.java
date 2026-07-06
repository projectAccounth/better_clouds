package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.factories;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.RenderPassFactory;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DPipeline;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DRenderPass;

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