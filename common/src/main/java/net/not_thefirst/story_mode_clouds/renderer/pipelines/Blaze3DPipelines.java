package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.DepthBufferImplType;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.PipelineProvider;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DPipeline;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.factories.B3DRenderPassFactory;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.factories.B3DUBODataBufferFactory;

public class Blaze3DPipelines {
    private Blaze3DPipelines() {}

    public static final IdentifierWrapper PIPELINE_IDS = IdentifierWrapper.of(Initializer.MOD_ID, "pipeline/pos_tex_c");
    public static final IdentifierWrapper SHADER_LOCATION = IdentifierWrapper.of(Initializer.MOD_ID, "rt_clouds");
    public static final IdentifierWrapper OUTLINE_SHADER_LOCATION = IdentifierWrapper.of(Initializer.MOD_ID, "rt_clouds_outline");

    private static final B3DPipeline.Builder COMMON_BUILDER = (B3DPipeline.Builder) 
        new B3DPipeline.Builder("COMMON")
            .withPrimitive(GLPrimitive.QUADS)
            .withVertexShader(SHADER_LOCATION.toString())
            .withFragmentShader(SHADER_LOCATION.toString())
            .withVertexFormat(VertexFormat.POSITION_COLOR_NORMAL)
            .withCullState(CullState.CULL)
            .withDepthTestState(DepthTestState.GEQUAL)
            .withUniformBlocks(Map.ofEntries(
                Map.entry("Transforms", 0),
                Map.entry("CloudInfo", 1),
                Map.entry("Lighting", 2),
                Map.entry("Camera", 3)
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
        .withBlendState(BlendState.NONE)
        .withMaskState(MaskState.DEPTH_ONLY)
        .build();

    public static final B3DPipeline OUTLINE_PIPELINE = COMMON_BUILDER
        .withName("OUTLINE_PIPELINE")
        .withId("cloud_tweaks:pipeline_outline")
        .withVertexShader(OUTLINE_SHADER_LOCATION.toString())
        .withFragmentShader(OUTLINE_SHADER_LOCATION.toString())
        .withBlendState(BlendState.TRANSLUCENT)
        .withFaceCull(FaceCullState.FRONT)
        .build();

    public static final PipelineProvider BLAZE3D = PipelineProvider.register("BLAZE3D");

    public static void init() {
        PipelineManager manager = PipelineManager.getInstance();
        manager.registerPipeline(POSITION_COLOR_NO_DEPTH, BLAZE3D);
        manager.registerPipeline(CUSTOM_POSITION_COLOR, BLAZE3D);
        manager.registerPipeline(POSITION_COLOR_DEPTH_ONLY, BLAZE3D);

        manager.registerDataBufferFactory(BLAZE3D, B3DUBODataBufferFactory::new);
        manager.registerRenderPassFactory(BLAZE3D, B3DRenderPassFactory::new);
        PipelineManager.setDepthBufferImplType(DepthBufferImplType.REVERSED_Z);
    }
}
