package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import java.util.Optional;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;

class B3DDefinitions {
    private B3DDefinitions() {}

    public static final ColorTargetState TRANSLUCENT_BLEND_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(BlendFunction.TRANSLUCENT),
            ColorTargetState.WRITE_ALL);

    public static final ColorTargetState NONE = 
        new ColorTargetState(
            Optional.empty(),
            ColorTargetState.WRITE_NONE);
    
    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.buildSnippet();
	public static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
	public static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();
}
