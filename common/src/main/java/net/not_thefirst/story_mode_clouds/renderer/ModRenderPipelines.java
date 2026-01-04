package net.not_thefirst.story_mode_clouds.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.Initializer;

public class ModRenderPipelines {
    public static RenderPipeline POSITION_COLOR_NO_DEPTH;
    public static RenderPipeline CUSTOM_POSITION_COLOR;
    public static RenderPipeline POSITION_COLOR_DEPTH;

    public static RenderPipeline.Snippet MATRICES_SNIPPET = 
        RenderPipeline.builder(new RenderPipeline.Snippet[0])
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .buildSnippet();

    public static RenderPipeline.Snippet FOG_NO_COLOR_SNIPPET = 
        RenderPipeline.builder(new RenderPipeline.Snippet[0])
            .withUniform("FogStart", UniformType.FLOAT)
            .withUniform("FogEnd", UniformType.FLOAT)
            .withUniform("FogShape", UniformType.INT)
            .buildSnippet();

    public static RenderPipeline.Snippet FOG_SNIPPET = 
        RenderPipeline.builder(new RenderPipeline.Snippet[]{FOG_NO_COLOR_SNIPPET})
            .withUniform("FogColor", UniformType.VEC4)
            .buildSnippet();

    public static RenderPipeline.Snippet MATRICES_COLOR_SNIPPET = RenderPipeline
        .builder(new RenderPipeline.Snippet[]{MATRICES_SNIPPET})
        .withUniform("ColorModulator", UniformType.VEC4)
        .buildSnippet();

    public static RenderPipeline.Snippet MATRICES_COLOR_FOG_SNIPPET = 
        RenderPipeline.builder(new RenderPipeline.Snippet[]{MATRICES_COLOR_SNIPPET, FOG_SNIPPET})
        .buildSnippet();

    public static void registerCloudPipelines() {
        ResourceLocation loc1 = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, "pipeline/pos_tex_c");
        ResourceLocation lc1 = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, "rt_clouds");
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_COLOR_FOG_SNIPPET)
            .withLocation(loc1)
            .withVertexShader(lc1)
            .withFragmentShader(lc1)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(true)
            .withUniform("CloudColor", UniformType.VEC4)
            .withUniform("Config", UniformType.INT)
            .withUniform("CloudFogStart", UniformType.INT)
            .withUniform("CloudFogEnd", UniformType.INT);
        
        POSITION_COLOR_NO_DEPTH = builder.withColorWrite(true).withDepthWrite(false).build();
        CUSTOM_POSITION_COLOR = builder.withColorWrite(true).withDepthWrite(true).build();
        POSITION_COLOR_DEPTH = builder.withColorWrite(false).withDepthWrite(true).build();
    }
}
