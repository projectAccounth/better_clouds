package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import java.util.Optional;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;

class B3DDefinitions {
    private B3DDefinitions() {}

    static final ColorTargetState TRANSLUCENT_BLEND_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(BlendFunction.TRANSLUCENT), 
            GpuFormat.RGBA8_UNORM, 
            ColorTargetState.WRITE_ALL);

    static final ColorTargetState PREMULTIPLIED_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), 
            GpuFormat.RGBA8_UNORM, 
            ColorTargetState.WRITE_ALL);

    static final ColorTargetState OPAQUE = 
        new ColorTargetState(
            Optional.empty(), 
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_ALL);
    
    static final ColorTargetState ADD_COLOR_TARGET = 
        new ColorTargetState(           
            Optional.of(BlendFunction.ADDITIVE), 
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_ALL);

    static final ColorTargetState ADDITIVE_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(BlendFunction.LIGHTNING), 
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_ALL);

    static final ColorTargetState SCREEN_COLOR_TARGET = 
        new ColorTargetState(
            Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR)), 
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_ALL);

    private static final BindGroupLayout MATRICES_PROJECTION = BindGroupLayout.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .build();

    private static final BindGroupLayout FOG = BindGroupLayout.builder()
        .withUniform("Fog", UniformType.UNIFORM_BUFFER)
        .build();

    static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withBindGroupLayout(MATRICES_PROJECTION)
		.buildSnippet();
	static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder()
        .withBindGroupLayout(FOG)
        .buildSnippet();
	static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = 
        RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();
}
