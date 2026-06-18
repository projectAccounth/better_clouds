package net.not_thefirst.story_mode_clouds.utils.rendering.opengl;

import java.util.List;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.GLStateGuard;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.state.RenderStateBuilder;
import net.not_thefirst.lib.gl_render_system.state.ShaderRenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractPipeline;

public class GLPipeline extends AbstractPipeline {

    private ShaderRenderType renderType;
    private GLProgram program;

    private IdentifierWrapper vertexShaderId;
    private IdentifierWrapper fragmentShaderId;
    private IdentifierWrapper pipelineId;

    public GLPipeline(
        final String name, 

        final List<String> uniforms, 
        final Map<String, Integer> uniformBlocks,
        final List<String> textures,
        final List<String> textureArrays,

        final BlendState blendState,
        final CullState cullState,
        final MaskState maskState,
        final DepthTestState depthTestState,
        
        final String vertexShader,
        final String fragmentShader,
        final String id,
        final VertexFormat vertexFormat
    ) {
        super(name, uniforms, uniformBlocks, textures, textureArrays, blendState, cullState, maskState, depthTestState, vertexShader, fragmentShader, id, vertexFormat);

        pipelineId = IdentifierWrapper.tryParse(id);
        vertexShaderId = IdentifierWrapper.tryParse(vertexShader);
        fragmentShaderId = IdentifierWrapper.tryParse(fragmentShader);

        if (pipelineId == null) {
            throw new IllegalArgumentException("Invalid pipeline ID: " + id);
        }
        if (vertexShaderId == null) {
            throw new IllegalArgumentException("Invalid vertex shader ID: " + vertexShader);
        }
        if (fragmentShaderId == null) {
            throw new IllegalArgumentException("Invalid fragment shader ID: " + fragmentShader);
        }

        GLResourceHandler.getProgramManager().register(vertexShaderId, vertexShader, fragmentShader);
    }

    @Override
    public void setup() {
        program = GLResourceHandler.getProgramManager().get(vertexShaderId);

        for (var entry : uniformBlocks.entrySet()) {
            program.bindUniformBlock(entry.getKey(), entry.getValue().intValue());
        }

        renderType = new ShaderRenderType(
            name, 
            new ShaderState(program),
            new RenderStateBuilder()
                .blend(blendState)
                .cull(cullState)
                .mask(maskState)
                .depthTest(depthTestState)
                .build()
        );
    }

    public GLProgram getProgram() {
        return this.renderType.program();
    }

    @Override
    public void bind() {
        this.renderType.setup();
        getProgram().use();
    }

    @Override
    public void unbind() {
        getProgram().close();
        this.renderType.clear();
    }
 
    public static class Builder extends AbstractPipeline.Builder<GLPipeline> {
        public Builder(String name) {
            super(name);
        }

        @Override
        public GLPipeline build() {
            return new GLPipeline(
                name, 
                uniforms, 
                uniformBlocks, 
                textures, 
                textureArrays, 
                blendState, 
                cullState, 
                maskState, 
                depthTestState,
                vertexShader,
                fragmentShader,
                id,
                vertexFormat
            );
        }
    }
}
