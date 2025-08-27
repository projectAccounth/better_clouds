package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.StoryModeClouds;

public class ModRenderPipelines {
    public static RenderPipeline CLOUDS_CUSTOM_PIPELINE;

    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.buildSnippet();
	private static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
	private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();

    public static void registerCloudPipelines() {
        // shader paths (resource path: assets/<modid>/shaders/...)
        ResourceLocation vs = ResourceLocation.fromNamespaceAndPath(StoryModeClouds.MOD_ID, "clouds");
        ResourceLocation fs = ResourceLocation.fromNamespaceAndPath(StoryModeClouds.MOD_ID, "clouds");
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(StoryModeClouds.MOD_ID, "pipeline/clouds");
        
        // Create shader program / pipeline description
        // NOTE: exact builder API may vary — adapt to your runtime mappings if needed.
        // The idea: compile/attach vertex+fragment into a pipeline object that accepts
        // the UBO "CloudInfo" and the isamplerBuffer "CloudFaces".
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                .withVertexShader(vs)
                .withFragmentShader(fs)
                .withLocation(loc)
                // ensure vertex format matches the quad vertex buffer (no extra attributes)
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.QUADS)
                // set blend/depth states similar to vanilla clouds:
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
		        .withUniform("CloudFaces", UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
                .withBlend(BlendFunction.TRANSLUCENT)
                // set other state as needed (cull face, polygon offset, etc.)
                ;

        CLOUDS_CUSTOM_PIPELINE = builder.build();
    }

}
