package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;

import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractPipeline;

public class B3DPipeline extends AbstractPipeline {
    private RenderPipeline pipeline;

    private IdentifierWrapper vertexShaderId;
    private IdentifierWrapper fragmentShaderId;

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
        
        final String vertexShader,
        final String fragmentShader,
        final String id,
        final VertexFormat vertexFormat
    ) {
        this.vertexShaderId = IdentifierWrapper.tryParse(vertexShader);
        this.fragmentShaderId = IdentifierWrapper.tryParse(fragmentShader);
        validateShader(vertexShader, fragmentShader);
        
        super(name, uniforms, uniformBlocks, textures, textureArrays, blendState, cullState, maskState, depthTestState, vertexShader, fragmentShader, id, vertexFormat);
    }

    public static void validateShader(String vertexPath, String fragmentPath) {
        IdentifierWrapper vertexShaderId = IdentifierWrapper.tryParse(vertexPath);
        IdentifierWrapper fragmentShaderId = IdentifierWrapper.tryParse(fragmentPath);

        if (vertexShaderId == null) {
            throw new IllegalArgumentException("Invalid vertex shader ID: " + vertexPath);
        }
        if (fragmentShaderId == null) {
            throw new IllegalArgumentException("Invalid fragment shader ID: " + fragmentPath);
        }

        if (vertexShaderId.getDelegate() == null) {
            throw new IllegalArgumentException("Vertex shader resource not found: " + vertexPath);
        }
        if (fragmentShaderId.getDelegate() == null) {
            throw new IllegalArgumentException("Fragment shader resource not found: " + fragmentPath);
        }
    }

    @Override
    public void setup() {
        RenderPipeline.Builder builder = RenderPipeline.builder(B3DDefinitions.MATRICES_FOG_SNIPPET)
            .withVertexShader(vertexShaderId.getDelegate())
            .withFragmentShader(fragmentShaderId.getDelegate());

        CompareOp compareOp = B3DConversions.toCompareOp(depthTestState);
        
        DepthStencilState depthStencilState = new DepthStencilState(compareOp, maskState.writeDepth());
        ColorTargetState colorTargetState = B3DConversions.toColorTargetState(blendState);

        BindGroupLayout.Builder bindGroupBuilder = BindGroupLayout.builder();
        for (var entry : uniformBlocks.entrySet()) {
            bindGroupBuilder.withUniform(entry.getKey(), UniformType.UNIFORM_BUFFER);
        }

        builder.withBindGroupLayout(bindGroupBuilder.build())
            .withDepthStencilState(depthStencilState)
            .withColorTargetState(colorTargetState)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withVertexBinding(0, B3DConversions.toVanillaVertexFormat(vertexFormat));

        this.pipeline = builder.build();
    }

    @Override
    public void bind() {
        // no-op
    }

    @Override
    public void unbind() {
        // no-op
    }

    public RenderPipeline getPipeline() {
        return this.pipeline;
    }
    
    public static class Builder extends AbstractPipeline.Builder<B3DPipeline> {
        public Builder(String name) {
            super(name);
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
                vertexShader, 
                fragmentShader,
                id,
                vertexFormat
            );
        }
    }
}
