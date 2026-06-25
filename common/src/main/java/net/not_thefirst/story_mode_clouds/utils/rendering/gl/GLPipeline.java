package net.not_thefirst.story_mode_clouds.utils.rendering.gl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL31;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.UniformBufferObject;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.state.RenderStateBuilder;
import net.not_thefirst.lib.gl_render_system.state.ShaderRenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

public class GLPipeline extends AbstractPipeline {

    private ShaderRenderType renderType;

    private final Map<String, UniformBufferObject> ubos =
        new LinkedHashMap<>();

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
        final FaceCullState faceCullState,

        final String vertexShader,
        final String fragmentShader,
        final String id,
        final VertexFormat vertexFormat
    ) {
        super(
            name,
            uniforms,
            uniformBlocks,
            textures,
            textureArrays,
            blendState,
            cullState,
            maskState,
            depthTestState,
            faceCullState,
            vertexShader,
            fragmentShader,
            id,
            vertexFormat
        );

        Objects.requireNonNull(id, "Pipeline id");
        Objects.requireNonNull(vertexShader, "Vertex shader");
        Objects.requireNonNull(fragmentShader, "Fragment shader");
    }

    public Map<String, UniformBufferObject> getUbos() {
        return ubos;
    }

    // unused 
    private void createUniformBuffers(GLProgram program) {

        for (Entry<String, Integer> entry :
                uniformBlocks.entrySet()) {

            String blockName = entry.getKey();
            int binding = entry.getValue();

            int blockIndex =
                GL31.glGetUniformBlockIndex(
                    program.id(),
                    blockName
                );

            if (blockIndex == GL31.GL_INVALID_INDEX) {
                throw new IllegalStateException(
                    "Uniform block '" +
                    blockName +
                    "' not found in shader '" +
                    id +
                    "'"
                );
            }

            int blockSize =
                GL31.glGetActiveUniformBlocki(
                    program.id(),
                    blockIndex,
                    GL31.GL_UNIFORM_BLOCK_DATA_SIZE
                );

            int alignedSize = blockSize;

            program.bindUniformBlock(
                blockName,
                binding
            );

            ubos.put(
                blockName,
                new UniformBufferObject(
                    binding,
                    alignedSize
                )
            );
        }
    }

    @Override
    public void setup() {

        GLResourceHandler
            .getProgramManager()
            .register(
                id,
                vertexShader,
                fragmentShader
            );

        GLProgram program =
            GLResourceHandler
                .getProgramManager()
                .get(id);

        createUniformBuffers(program);

        renderType = new ShaderRenderType(
            name,
            new ShaderState(program),
            new RenderStateBuilder()
                .blend(blendState)
                .cull(cullState)
                .mask(maskState)
                .depthTest(depthTestState)
                .cullFace(faceCullState)
                .build()
        );
    }

    public GLProgram getProgram() {
        return renderType.program();
    }

    @Override
    public void bind() {
        renderType.setup();
        getProgram().use();
    }

    @Override
    public void unbind() {
        getProgram().stop();
        renderType.clear();
    }

    @Override
    public void cleanup() {
        ubos.values().forEach(UniformBufferObject::close);
        ubos.clear();
    }

    public static class Builder
            extends AbstractPipeline.Builder<GLPipeline> {

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
                faceCullState,
                vertexShader,
                fragmentShader,
                id,
                vertexFormat
            );
        }
    }
}