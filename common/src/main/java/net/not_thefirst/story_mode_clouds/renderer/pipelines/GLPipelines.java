package net.not_thefirst.story_mode_clouds.renderer.pipelines;

import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline.Builder;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.PipelineProvider;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLPipeline;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLRenderPassFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLRenderTargetFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLResourceHandler;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLTextureFactory;
import net.not_thefirst.story_mode_clouds.renderer.platform.gl.GLUBODataBufferFactory;

public class GLPipelines extends PipelineAllocator {
    public GLPipelines() {
        super();
    }

    static final IdentifierWrapper CLOUD_SHADER_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/rt_clouds.vert");
    static final IdentifierWrapper CLOUD_SHADER_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/rt_clouds.frag");

    static final IdentifierWrapper CLOUD_SHADER_OUTLINE_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/rt_clouds_outline.vert");
    static final IdentifierWrapper CLOUD_SHADER_OUTLINE_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/rt_clouds_outline.frag");

    static final IdentifierWrapper NO_OP_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/no_op.vert");
    static final IdentifierWrapper NO_OP_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/no_op.frag");

    static final IdentifierWrapper SCREENQUAD = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/screenquad.vert");
    static final IdentifierWrapper STENCIL_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/stencil.frag");
    static final IdentifierWrapper BLIT_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/gl/blit.frag");

    private static GLPipeline.Builder base = (GLPipeline.Builder) new GLPipeline.Builder("CUSTOM_POSITION_COLOR")
        .withBlendState(BlendState.TRANSLUCENT)
        .withCullState(CullState.CULL)
        .withFaceCull(FaceCullState.BACK)
        .withVertexFormat(VertexFormat.POSITION_COLOR_NORMAL)
        .withVertexShader(CLOUD_SHADER_VERT.toString())
        .withFragmentShader(CLOUD_SHADER_FRAG.toString())
        .withDepthTestState(DepthTestState.LEQUAL)
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
        .withBlendState(BlendState.OPAQUE)
        .build();

    public static final GLPipeline CUSTOM_POSITION_COLOR = base
        .withId("CUSTOM_POSITION_COLOR")
        .withName("CUSTOM_POSITION_COLOR")
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .withBlendState(BlendState.TRANSLUCENT)
        .build();

    public static final GLPipeline POSITION_COLOR_NO_DEPTH = base
        .withId("POSITION_COLOR_NO_DEPTH")
        .withName("POSITION_COLOR_NO_DEPTH")
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .withBlendState(BlendState.TRANSLUCENT)
        .build();

    public static final GLPipeline POSITION_COLOR_DEPTH_ONLY = base
        .withId("POSITION_COLOR_DEPTH_ONLY")
        .withName("POSITION_COLOR_DEPTH_ONLY")
        .withVertexShader(NO_OP_VERT.toString())
        .withFragmentShader(NO_OP_FRAG.toString())
        .withMaskState(MaskState.DEPTH_ONLY)
        .withBlendState(BlendState.OPAQUE)
        .build();

    public static final GLPipeline OUTLINE = base
        .withId("OUTLINE")
        .withName("OUTLINE")
        .withVertexShader(CLOUD_SHADER_OUTLINE_VERT.toString())
        .withFragmentShader(CLOUD_SHADER_OUTLINE_FRAG.toString())
        .withMaskState(MaskState.COLOR_NO_DEPTH)
        .withBlendState(BlendState.TRANSLUCENT)
        .build();

    public static final GLPipeline STENCIL = new GLPipeline.Builder("STENCIL")
        .withId("STENCIL")
        .withVertexShader(SCREENQUAD.toString())
        .withFragmentShader(STENCIL_FRAG.toString())
        .withBlendState(BlendState.OPAQUE)
        .withDepthTestState(DepthTestState.ALWAYS)
        .withMaskState(MaskState.COLOR_DEPTH)
        .build();

    public static final GLPipeline BLIT = new GLPipeline.Builder("BLIT")
        .withId("BLIT")
        .withVertexShader(SCREENQUAD.toString())
        .withFragmentShader(BLIT_FRAG.toString())
        .withBlendState(BlendState.PREMULTIPLIED_ALPHA)
        .withDepthTestState(DepthTestState.LEQUAL)
        .withMaskState(MaskState.COLOR_DEPTH)
        .build();

    public static final PipelineProvider GL = PipelineProvider.register("GL");

    public void initialize() {
        var manager = PipelineManager.getInstance();

        GLResourceHandler.init(ClientHelper.getClient().getResourceManager());
        manager.registerDataBufferFactory(GL, GLUBODataBufferFactory::new);
        manager.registerRenderPassFactory(GL, GLRenderPassFactory::new);
        manager.registerTextureFactory(GL, GLTextureFactory::new);
        manager.registerRenderTargetFactory(GL, GLRenderTargetFactory::new);

        manager.registerPipeline(CUSTOM_POSITION_COLOR, GL);
        manager.registerPipeline(POSITION_COLOR_NO_DEPTH, GL);
        manager.registerPipeline(POSITION_COLOR_DEPTH_ONLY, GL);
        manager.registerPipeline(OUTLINE, GL);
        manager.registerPipeline(NONE, GL);
        manager.registerPipeline(STENCIL, GL);
        manager.registerPipeline(BLIT, GL);
    }

    @Override
    public Builder<?> createBuilder(String name) {
        return new GLPipeline.Builder(name);
    }

    @Override
    public void registerPipeline(AbstractPipeline pipeline) {
        if (!(pipeline instanceof GLPipeline)) {
            throw new IllegalArgumentException("Pipeline must be an instance of GLPipeline");
        }
        PipelineManager.getInstance().registerPipeline(pipeline, GL);
    }

    @Override
    public AbstractPipeline getPipeline(String name) {
        PipelineManager.setPipelineProvider(GL);
        return PipelineManager.getInstance().getPipeline(name);
    }
}