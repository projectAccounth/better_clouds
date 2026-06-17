package net.not_thefirst.story_mode_clouds.utils.rendering;

import java.util.List;

public abstract class AbstractRenderPass<T extends AbstractPipeline> {
    private final String name;
    private final List<T> pipelines;

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

    public abstract void setUniform1i(String name, int value);
    public abstract void setUniform2i(String name, int x, int y);
    public abstract void setUniform3i(String name, int x, int y, int z);
    public abstract void setUniform4i(String name, int x, int y, int z, int w);

    public abstract void setUniform1f(String name, float value);
    public abstract void setUniform2f(String name, float x, float y);
    public abstract void setUniform3f(String name, float x, float y, float z);
    public abstract void setUniform4f(String name, float x, float y, float z, float w);

    public abstract void setUniformMatrix3f(String name, float[] matrix);
    public abstract void setUniformMatrix4f(String name, float[] matrix);

    public abstract void bindTexture(String name, int textureId, int slot);
    public abstract void bindTextureArray(String name, int textureId, int slot);
}
