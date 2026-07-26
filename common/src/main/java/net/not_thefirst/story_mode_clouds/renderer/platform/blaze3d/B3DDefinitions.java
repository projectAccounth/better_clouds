package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import java.util.Optional;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;

class B3DDefinitions {
    private B3DDefinitions() {}

    public static final ColorTargetState TRANSLUCENT_BLEND_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(BlendFunction.TRANSLUCENT), 
            GpuFormat.RGBA8_UNORM, 
            ColorTargetState.WRITE_ALL);

    public static final ColorTargetState NONE = 
        new ColorTargetState(
            Optional.empty(), 
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_NONE);
    
    private static final BindGroupLayout MATRICES_PROJECTION = BindGroupLayout.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .build();

    private static final BindGroupLayout FOG = BindGroupLayout.builder()
        .withUniform("Fog", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withBindGroupLayout(MATRICES_PROJECTION)
		.buildSnippet();
	public static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder()
        .withBindGroupLayout(FOG)
        .buildSnippet();
	public static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = 
        RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();
}
