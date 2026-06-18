package net.not_thefirst.story_mode_clouds.utils.rendering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRenderPass<T extends AbstractPipeline> implements AutoCloseable {
    protected final String name;
    protected final List<T> pipelines;
    protected final Map<String, UniformValue> uniforms = new HashMap<>();

    protected AbstractRenderPass(String name, T[] pipelines) {
        this.name = name;
        this.pipelines = List.of(pipelines);
    }

    public String getName() {
        return name;
    }

    public List<T> getPipelines() {
        return pipelines;
    }

    public abstract void setup();
    public abstract void render();
    public abstract void cleanup();

    @Override
    public void close() {
        cleanup();
    }

    public void setUniform1i(String name, int value) {
        uniforms.put(name, new UniformValue(
            UniformType.INT1,
            value
        ));
    }

    public void setUniform2i(String name, int x, int y) {
        uniforms.put(name, new UniformValue(
            UniformType.INT2,
            new int[]{x, y}
        ));
    }

    public void setUniform3i(String name, int x, int y, int z) {
        uniforms.put(name, new UniformValue(
            UniformType.INT3,
            new int[]{x, y, z}
        ));
    }

    public void setUniform4i(String name, int x, int y, int z, int w) {
        uniforms.put(name, new UniformValue(
            UniformType.INT4,
            new int[]{x, y, z, w}
        ));
    }

    public void setUniform1f(String name, float value) {
        uniforms.put(name, new UniformValue(
            UniformType.FLOAT1,
            value
        ));
    }

    public void setUniform2f(String name, float x, float y) {
        uniforms.put(name, new UniformValue(
            UniformType.FLOAT2,
            new float[]{x, y}
        ));
    }

    public void setUniform3f(String name, float x, float y, float z) {
        uniforms.put(name, new UniformValue(
            UniformType.FLOAT3,
            new float[]{x, y, z}
        ));
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        uniforms.put(name, new UniformValue(
            UniformType.FLOAT4,
            new float[]{x, y, z, w}
        ));
    }

    public void setUniformMatrix3f(String name, float[] matrix) {
        uniforms.put(name, new UniformValue(
            UniformType.MAT3,
            matrix
        ));
    }

    public void setUniformMatrix4f(String name, float[] matrix) {
        uniforms.put(name, new UniformValue(
            UniformType.MAT4,
            matrix
        ));
    }

    public abstract void bindTexture(String name, int textureId, int slot);
    public abstract void bindTextureArray(String name, int textureId, int slot);

    public abstract void bindUniformBlock(String name, AbstractUBODataBuffer ubo);
}
