package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import java.util.Map;

import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.rendering.PipelineManager;
import net.not_thefirst.story_mode_clouds.utils.rendering.PipelineManager.PipelineProvider;
import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DPipeline;

public class Blaze3DPipelines {
    private Blaze3DPipelines() {}

    public static final IdentifierWrapper PIPELINE_IDS = IdentifierWrapper.of(Initializer.MOD_ID, "pipeline/pos_tex_c");
    public static final IdentifierWrapper SHADER_LOCATION = IdentifierWrapper.of(Initializer.MOD_ID, "rt_clouds");

    private static final B3DPipeline.Builder COMMON_BUILDER = (B3DPipeline.Builder) 
        new B3DPipeline.Builder("COMMON")
            .withVertexShader(SHADER_LOCATION.toString())
            .withFragmentShader(SHADER_LOCATION.toString())
            .withId(PIPELINE_IDS.toString())
            .withVertexFormat(VertexFormat.POSITION_COLOR_NORMAL)
            .withCullState(CullState.CULL)
            .withUniformBlocks(Map.ofEntries(
                Map.entry("Transforms", 0),
                Map.entry("CloudInfo", 1),
                Map.entry("Lighting", 2),
                Map.entry("Camera", 3),
                Map.entry("DynamicTransforms", 4),
                Map.entry("Projection", 5),
                Map.entry("Fog", 6)
            ));

    public static final B3DPipeline POSITION_COLOR_NO_DEPTH = COMMON_BUILDER
        .withName("POSITION_COLOR_NO_DEPTH")
        .withBlendState(BlendState.TRANSLUCENT)
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final B3DPipeline CUSTOM_POSITION_COLOR = COMMON_BUILDER
        .withName("CUSTOM_POSITION_COLOR")
        .withBlendState(BlendState.TRANSLUCENT)
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final B3DPipeline POSITION_COLOR_DEPTH_ONLY = COMMON_BUILDER
        .withName("POSITION_COLOR_DEPTH_ONLY")
        .withBlendState(BlendState.NONE)
        .withMaskState(MaskState.DEPTH_ONLY)
        .build();

    static {
        PipelineManager manager = PipelineManager.getInstance();
        manager.registerPipeline(POSITION_COLOR_NO_DEPTH, PipelineProvider.BLAZE3D);
        manager.registerPipeline(CUSTOM_POSITION_COLOR, PipelineProvider.BLAZE3D);
        manager.registerPipeline(POSITION_COLOR_DEPTH_ONLY, PipelineProvider.BLAZE3D);
    }
}
