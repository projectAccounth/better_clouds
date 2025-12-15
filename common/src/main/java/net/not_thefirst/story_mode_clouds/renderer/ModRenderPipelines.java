package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.Initializer;

public class ModRenderPipelines {
    public static RenderPipeline CLOUDS_CUSTOM_PIPELINE;
    public static RenderPipeline CUSTOM_POSITION_COLOR;
    public static RenderPipeline POSITION_COLOR_DEPTH;

    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.buildSnippet();
	private static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
	private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();

    public static void registerCloudPipelines() {
        ResourceLocation loc1 = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, "pipeline/pos_tex_c");
        ResourceLocation lc1 = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, "rt_clouds");
        var builder = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation(loc1)
            .withVertexShader(lc1)
            .withFragmentShader(lc1)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(true)
            .withUniform("Model", UniformType.UNIFORM_BUFFER);
        CUSTOM_POSITION_COLOR = builder.withColorWrite(true).withDepthWrite(true).build();
        POSITION_COLOR_DEPTH = builder.withColorWrite(false).withDepthWrite(true).build();
    }
}
