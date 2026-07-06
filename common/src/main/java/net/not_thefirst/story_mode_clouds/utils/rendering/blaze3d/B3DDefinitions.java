package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;

class B3DDefinitions {
    private B3DDefinitions() {}
    
    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.buildSnippet();
	public static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
	public static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();
}
