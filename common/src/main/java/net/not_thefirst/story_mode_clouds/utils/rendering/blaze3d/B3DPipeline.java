package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import java.util.List;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.FaceCullState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

public class B3DPipeline extends AbstractPipeline {
    public B3DPipeline(
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
        final VertexFormat vertexFormat,
        final GLPrimitive primitive
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
            vertexFormat);
    }

    @Override
    public void setup() {
    }

    @Override
    public void bind() {
        // no-op
    }

    @Override
    public void unbind() {
        // no-op
    }
    
    public static class Builder extends AbstractPipeline.Builder<B3DPipeline> {
        private GLPrimitive primitive;
        public Builder(String name) {
            super(name);
            primitive = GLPrimitive.TRIANGLES;
        }

        public B3DPipeline.Builder withPrimitive(GLPrimitive primitive) {
            this.primitive = primitive;
            return this;
        }

        @Override
        public B3DPipeline build() {
            return new B3DPipeline(
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
                vertexFormat,
                primitive
            );
        }
    }

    @Override
    public void cleanup() {
        // nothing
    }
}
