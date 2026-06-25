package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.PipelineProvider;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.rendering.gl.GLPipeline;
import net.not_thefirst.story_mode_clouds.utils.rendering.gl.factories.GLRenderPassFactory;
import net.not_thefirst.story_mode_clouds.utils.rendering.gl.factories.GLUBODataBufferFactory;

public class GLPipelines {
    private GLPipelines() {}

    public static final IdentifierWrapper CLOUD_SHADER_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/rt_clouds.vert");
    public static final IdentifierWrapper CLOUD_SHADER_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/rt_clouds.frag");

    private static GLPipeline.Builder base = (GLPipeline.Builder) new GLPipeline.Builder("CUSTOM_POSITION_COLOR")
        .withBlendState(BlendState.TRANSLUCENT)
        .withCullState(CullState.CULL)
        .withFaceCull(FaceCullState.BACK)
        .withVertexFormat(VertexFormat.POSITION_COLOR_NORMAL)
        .withVertexShader(CLOUD_SHADER_VERT.toString())
        .withFragmentShader(CLOUD_SHADER_FRAG.toString())
        .withUniformBlocks(Map.of(
            "Transforms", 0,
            "CloudInfo" , 1,
            "Lighting"  , 2,
            "Camera"    , 3
        ));

    public static final GLPipeline NONE = base
        .withId("NONE")
        .withName("NONE")
        .withMaskState(MaskState.NONE)
        .build();

    public static final GLPipeline CUSTOM_POSITION_COLOR = base
        .withId("CUSTOM_POSITION_COLOR")
        .withName("CUSTOM_POSITION_COLOR")
        .withMaskState(MaskState.COLOR_DEPTH)
        .build();

    public static final GLPipeline POSITION_COLOR_NO_DEPTH = base
        .withId("POSITION_COLOR_NO_DEPTH")
        .withName("POSITION_COLOR_NO_DEPTH")
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .build();

    public static final GLPipeline POSITION_COLOR_DEPTH_ONLY = base
        .withId("POSITION_COLOR_DEPTH_ONLY")
        .withName("POSITION_COLOR_DEPTH_ONLY")
        .withMaskState(MaskState.DEPTH_ONLY)
        .build();


    public static void init() {
        var manager = PipelineManager.getInstance();
        manager.registerDataBufferFactory(PipelineProvider.DEFAULT, GLUBODataBufferFactory::new);
        manager.registerRenderPassFactory(PipelineProvider.DEFAULT, GLRenderPassFactory::new);

        manager.registerPipeline(CUSTOM_POSITION_COLOR, PipelineProvider.DEFAULT);
        manager.registerPipeline(POSITION_COLOR_NO_DEPTH, PipelineProvider.DEFAULT);
        manager.registerPipeline(POSITION_COLOR_DEPTH_ONLY, PipelineProvider.DEFAULT);
        manager.registerPipeline(NONE, PipelineProvider.DEFAULT);
    }
}