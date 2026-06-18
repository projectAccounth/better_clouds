package net.not_thefirst.story_mode_clouds.utils.rendering;

import java.util.List;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.utils.rendering.PipelineManager.DepthBufferImplType;

public abstract class AbstractPipeline {
    protected final String name;
    protected final List<String> uniforms;
    protected final Map<String, Integer> uniformBlocks;

    // placeholder (not implemented)
    protected final List<String> textures;
    protected final List<String> textureArrays;

    protected final BlendState blendState;
    protected final CullState cullState;
    protected final MaskState maskState;
    protected final DepthTestState depthTestState;

    protected final String vertexShader;
    protected final String fragmentShader;
    protected final String id;

    protected final VertexFormat vertexFormat;

    protected AbstractPipeline(
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
        this.name = name;
        this.uniforms = uniforms;
        this.uniformBlocks = uniformBlocks;
        this.textures = textures;
        this.textureArrays = textureArrays;
        this.blendState = blendState;
        this.cullState = cullState;
        this.maskState = maskState;
        this.depthTestState = depthTestState;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.id = id;
        this.vertexFormat = vertexFormat;
    }

    public Map<String, Integer> getUniformBlocks() {
        return uniformBlocks;
    }

    public List<String> getTextures() {
        return textures;
    }

    public List<String> getTextureArrays() {
        return textureArrays;
    }

    public String getName() {
        return name;
    }

    public List<String> getUniforms() {
        return uniforms;
    }

    public boolean hasUniform(String name) {
        return uniforms.contains(name);
    }

    public boolean hasUniformBlock(String name) {
        return uniformBlocks.containsKey(name);
    }

    public boolean hasTexture(String name) {
        return textures.contains(name);
    }

    public boolean hasTextureArray(String name) {
        return textureArrays.contains(name);
    }

    public BlendState getBlendState() {
        return blendState;
    }

    public CullState getCullState() {
        return cullState;
    }

    public MaskState getMaskState() {
        return maskState;
    }

    public DepthTestState getDepthTestState() {
        return depthTestState;
    }

    public abstract void bind();
    public abstract void unbind();

    /**
     * Sets up the pipeline at game initialization/resource reload. This is where shader compilation and other expensive setup should occur.
     */
    public abstract void setup();

    public static class Builder<T extends AbstractPipeline> {
        protected final String name;
        protected List<String> uniforms;
        protected Map<String, Integer> uniformBlocks;
        protected List<String> textures;
        protected List<String> textureArrays;
        protected BlendState blendState;
        protected CullState cullState;
        protected MaskState maskState;
        protected DepthTestState depthTestState;
        protected String vertexShader;
        protected String fragmentShader;
        protected String id;
        protected VertexFormat vertexFormat;

        public Builder(String name) {
            this.name = name;
            this.uniforms = List.of();
            this.uniformBlocks = Map.of();
            this.textures = List.of();
            this.textureArrays = List.of();

            cullState = CullState.CULL;
            maskState = MaskState.COLOR_DEPTH;
            depthTestState = 
                PipelineManager.DEPTH_BUFFER_IMPL == DepthBufferImplType.STANDARD ? 
                    DepthTestState.LEQUAL : DepthTestState.GEQUAL;
            blendState = BlendState.TRANSLUCENT;
            vertexFormat = VertexFormat.POSITION_COLOR;
        }

        public Builder<T> withName(String name) {
            return new Builder<>(name);
        }

        public Builder<T> withUniforms(List<String> uniforms) {
            this.uniforms = uniforms;
            return this;
        }

        public Builder<T> withUniformBlocks(Map<String, Integer> uniformBlocks) {
            this.uniformBlocks = uniformBlocks;
            return this;
        }

        public Builder<T> withTextures(List<String> textures) {
            this.textures = textures;
            return this;
        }

        public Builder<T> withTextureArrays(List<String> textureArrays) {
            this.textureArrays = textureArrays;
            return this;
        }

        public Builder<T> withBlendState(BlendState blendState) {
            this.blendState = blendState;
            return this;
        }

        public Builder<T> withCullState(CullState cullState) {
            this.cullState = cullState;
            return this;
        }

        public Builder<T> withMaskState(MaskState maskState) {
            this.maskState = maskState;
            return this;
        }

        public Builder<T> withDepthTestState(DepthTestState depthTestState) {
            this.depthTestState = depthTestState;
            return this;
        }

        public Builder<T> withVertexShader(String vert) {
            this.vertexShader = vert;
            return this;
        }

        public Builder<T> withFragmentShader(String frag) {
            this.fragmentShader = frag;
            return this;
        }

        public Builder<T> withId(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> withVertexFormat(VertexFormat vertexFormat) {
            this.vertexFormat = vertexFormat;
            return this;
        }

        public T build() {
            throw new UnsupportedOperationException("Build method must be implemented in subclasses.");
        }
    }
}
