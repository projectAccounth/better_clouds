package net.not_thefirst.story_mode_clouds.renderer;

import java.util.Optional;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;

public class ModRenderPipelines {
    private ModRenderPipelines() {}

    public static RenderPipeline POSITION_COLOR_NO_DEPTH;
    public static RenderPipeline CUSTOM_POSITION_COLOR;
    public static RenderPipeline POSITION_COLOR_DEPTH;

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

    private static final BindGroupLayout CLOUDS_INFO = BindGroupLayout.builder()
        .withUniform("Transforms", UniformType.UNIFORM_BUFFER)
        .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
        .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
        .withUniform("Camera", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withBindGroupLayout(MATRICES_PROJECTION)
		.buildSnippet();
	public static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withBindGroupLayout(FOG).buildSnippet();
	public static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();

    public static void registerCloudPipelines() {
        IdentifierWrapper loc1 = IdentifierWrapper.of(Initializer.MOD_ID, "pipeline/pos_tex_c");
        IdentifierWrapper lc1 = IdentifierWrapper.of(Initializer.MOD_ID, "rt_clouds");
        var builder = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation(loc1.getDelegate())
            .withVertexShader(lc1.getDelegate())
            .withFragmentShader(lc1.getDelegate())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(true)
            .withBindGroupLayout(CLOUDS_INFO);
        
        POSITION_COLOR_NO_DEPTH = builder
            .withColorTargetState(TRANSLUCENT_BLEND_COLOR_TARGET)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

        CUSTOM_POSITION_COLOR = builder
            .withColorTargetState(TRANSLUCENT_BLEND_COLOR_TARGET)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

        POSITION_COLOR_DEPTH = builder
            .withColorTargetState(NONE)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .build();
    }
}
