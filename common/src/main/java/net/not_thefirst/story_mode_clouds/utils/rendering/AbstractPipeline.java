package net.not_thefirst.story_mode_clouds.utils.rendering;

import java.util.List;
import java.util.Map;

public abstract class AbstractPipeline {
    private final String name;
    private final List<String> uniforms;
    private final Map<String, Integer> uniformBlocks;

    // placeholder (not implemented)
    private final List<String> textures;
    private final List<String> textureArrays;

    protected AbstractPipeline(String name) {
        this.name = name;
        this.uniforms = List.of();
        this.uniformBlocks = Map.of();
        this.textures = List.of();
        this.textureArrays = List.of();
    }

    protected AbstractPipeline(String name, List<String> uniforms, Map<String, Integer> uniformBlocks, List<String> textures, List<String> textureArrays) {
        this.name = name;
        this.uniforms = uniforms;
        this.uniformBlocks = uniformBlocks;
        this.textures = textures;
        this.textureArrays = textureArrays;
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

    public void addUniform(String name) {
        if (!uniforms.contains(name)) {
            uniforms.add(name);
        }
    }

    public void addUniformBlock(String name, int bindingPoint) {
        uniformBlocks.computeIfAbsent(name, k -> bindingPoint);
    }

    public void addTexture(String name) {
        throw new UnsupportedOperationException("Adding textures is not supported in this pipeline.");
    }

    public void addTextureArray(String name) {
        throw new UnsupportedOperationException("Adding texture arrays is not supported in this pipeline.");
    }

    public abstract void bind();
    public abstract void unbind();
    public abstract void setup();

    public static class Builder<T extends AbstractPipeline> {
        protected final String name;
        protected List<String> uniforms;
        protected Map<String, Integer> uniformBlocks;
        protected List<String> textures;
        protected List<String> textureArrays;

        public Builder(String name) {
            this.name = name;
            this.uniforms = List.of();
            this.uniformBlocks = Map.of();
            this.textures = List.of();
            this.textureArrays = List.of();
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

        public T build() {
            throw new UnsupportedOperationException("Build method must be implemented in subclasses.");
        }
    }
}
