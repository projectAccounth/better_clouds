package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;

public class ModRenderPipelines {
    public static RenderPipeline POSITION_COLOR_NO_DEPTH;
    public static RenderPipeline CUSTOM_POSITION_COLOR;
    public static RenderPipeline POSITION_COLOR_DEPTH;

    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.buildSnippet();
	private static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
	private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();

    public static void registerCloudPipelines() {
        IdentifierWrapper loc1 = IdentifierWrapper.of(Initializer.MOD_ID, "pipeline/pos_tex_c");
        IdentifierWrapper lc1 = IdentifierWrapper.of(Initializer.MOD_ID, "rt_clouds");
        var builder = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation(loc1.getDelegate())
            .withVertexShader(lc1.getDelegate())
            .withFragmentShader(lc1.getDelegate())
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withCull(true)
            .withUniform("Transforms", UniformType.UNIFORM_BUFFER)
            .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withUniform("Camera", UniformType.UNIFORM_BUFFER);
        
        POSITION_COLOR_NO_DEPTH = builder.withColorWrite(true).withDepthWrite(false).build();
        CUSTOM_POSITION_COLOR = builder.withColorWrite(true).withDepthWrite(true).build();
        POSITION_COLOR_DEPTH = builder.withColorWrite(false).withDepthWrite(true).build();
    }
}
