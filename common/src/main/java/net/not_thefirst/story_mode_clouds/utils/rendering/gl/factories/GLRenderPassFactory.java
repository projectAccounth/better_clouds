package net.not_thefirst.story_mode_clouds.utils.rendering.gl.factories;

import net.not_thefirst.story_mode_clouds.utils.rendering.gl.GLPipeline;
import net.not_thefirst.story_mode_clouds.utils.rendering.gl.GLRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.RenderPassFactory;

public class GLRenderPassFactory implements RenderPassFactory<GLPipeline, GLRenderPass> {
    public GLRenderPassFactory() { /* */}
    
    @Override
    public Class<GLPipeline> getPipelineClass() {
        return GLPipeline.class;
    }
    
    @Override
    public GLRenderPass create(String name, AbstractPipeline[] pipelines) {
        verifyPipelines(pipelines);
        GLPipeline[] converted = new GLPipeline[pipelines.length];
        for (int i = 0; i < pipelines.length; ++i) converted[i] = (GLPipeline) pipelines[i];
        return new GLRenderPass(name, converted);
    }
}