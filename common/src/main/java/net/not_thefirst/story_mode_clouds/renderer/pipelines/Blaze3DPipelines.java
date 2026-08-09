package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import java.util.Arrays;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.PipelineProvider;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DPipeline;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DRenderPassFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DRenderTargetFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DTextureFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d.B3DUBODataBufferFactory;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

public class Blaze3DPipelines {
    private Blaze3DPipelines() {}

    public static final IdentifierWrapper PIPELINE_IDS = IdentifierWrapper.of(Initializer.MOD_ID, "pipeline/pos_tex_c");
    public static final IdentifierWrapper SHADER_LOCATION = IdentifierWrapper.of(Initializer.MOD_ID, "b3d/rt_clouds");
    public static final IdentifierWrapper SHADER_LOCATION_OUTLINE = IdentifierWrapper.of(Initializer.MOD_ID, "b3d/rt_clouds_outline");
    public static final IdentifierWrapper NO_OP_SHADER_LOCATION = IdentifierWrapper.of(Initializer.MOD_ID, "b3d/no_op");
    public static final IdentifierWrapper SCREENQUAD = 
        IdentifierWrapper.of(Initializer.MOD_ID, "b3d/screenquad");
    public static final IdentifierWrapper STENCIL_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "b3d/stencil");
    public static final IdentifierWrapper BLIT_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "b3d/blit");


    private static final B3DPipeline.Builder COMMON_BUILDER = (B3DPipeline.Builder) 
        new B3DPipeline.Builder("COMMON")
            .withPrimitive(GLPrimitive.QUADS)
            .withVertexShader(SHADER_LOCATION.toString())
            .withFragmentShader(SHADER_LOCATION.toString())
            .withVertexFormat(VertexFormat.POSITION_COLOR_NORMAL)
            .withCullState(CullState.CULL)
            .withFaceCull(FaceCullState.BACK)
            .withDepthTestState(DepthTestState.GEQUAL)
            .withUniformBlocks(Map.ofEntries(
                Map.entry("Transforms", 0),
                Map.entry("CloudInfo", 1),
                Map.entry("Lighting", 2),
                Map.entry("Camera", 3),
                Map.entry("Outline", 4)
            ));

    public static final B3DPipeline POSITION_COLOR_NO_DEPTH = COMMON_BUILDER
        .withName("POSITION_COLOR_NO_DEPTH")
        .withId("cloud_tweaks:pipeline_nd")
        .withBlendState(BlendState.TRANSLUCENT)
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final B3DPipeline CUSTOM_POSITION_COLOR = COMMON_BUILDER
        .withName("CUSTOM_POSITION_COLOR")
        .withId("cloud_tweaks:pipeline_cc")
        .withBlendState(BlendState.TRANSLUCENT)
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final B3DPipeline POSITION_COLOR_DEPTH_ONLY = COMMON_BUILDER
        .withName("POSITION_COLOR_DEPTH_ONLY")
        .withId("cloud_tweaks:pipeline_do")
        .withVertexShader(NO_OP_SHADER_LOCATION.toString())
        .withFragmentShader(NO_OP_SHADER_LOCATION.toString())
        .withBlendState(BlendState.OPAQUE)
        .withMaskState(MaskState.DEPTH_ONLY)
        .build();

    public static final B3DPipeline OUTLINE = COMMON_BUILDER
        .withName("OUTLINE")
        .withId("cloud_tweaks:pipeline_outline")
        .withVertexShader(SHADER_LOCATION_OUTLINE.toString())
        .withFragmentShader(SHADER_LOCATION_OUTLINE.toString())
        .withBlendState(BlendState.TRANSLUCENT)
        .withDepthTestState(DepthTestState.GEQUAL)
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final B3DPipeline STENCIL = new B3DPipeline.Builder("STENCIL")
        .withName("STENCIL")
        .withId("cloud_tweaks:stencil")
        .withVertexShader(SCREENQUAD.toString())
        .withFragmentShader(STENCIL_FRAG.toString())
        .withBlendState(BlendState.OPAQUE)
        .withDepthTestState(DepthTestState.ALWAYS)
        .withMaskState(MaskState.COLOR_DEPTH)
        .withUniforms(Arrays.asList("colorTex0", "colorTex1", "depthTex0", "depthTex1"))
        .build();

    public static final B3DPipeline BLIT = new B3DPipeline.Builder("BLIT")
        .withName("BLIT")
        .withId("cloud_tweaks:blit")
        .withVertexShader(SCREENQUAD.toString())
        .withFragmentShader(BLIT_FRAG.toString())
        .withBlendState(BlendState.PREMULTIPLIED_ALPHA)
        .withDepthTestState(DepthTestState.GEQUAL)
        .withMaskState(MaskState.COLOR_DEPTH)
        .withUniforms(Arrays.asList("colorTex0", "colorTex1", "depthTex0", "depthTex1"))
        .build();


    public static final PipelineProvider BLAZE3D = PipelineProvider.register("BLAZE3D");

    public static void init() {
        PipelineManager manager = PipelineManager.getInstance();
        manager.registerPipeline(POSITION_COLOR_NO_DEPTH, BLAZE3D);
        manager.registerPipeline(CUSTOM_POSITION_COLOR, BLAZE3D);
        manager.registerPipeline(POSITION_COLOR_DEPTH_ONLY, BLAZE3D);
        manager.registerPipeline(OUTLINE, BLAZE3D);
        manager.registerPipeline(STENCIL, BLAZE3D);
        manager.registerPipeline(BLIT, BLAZE3D);

        manager.registerDataBufferFactory(  BLAZE3D, B3DUBODataBufferFactory::new);
        manager.registerRenderPassFactory(  BLAZE3D, B3DRenderPassFactory::new);
        manager.registerRenderTargetFactory(BLAZE3D, B3DRenderTargetFactory::new);
        manager.registerTextureFactory(     BLAZE3D, B3DTextureFactory::new);
    }
}
