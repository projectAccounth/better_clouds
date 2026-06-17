package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractRenderPass;

public class B3DRenderPass extends AbstractRenderPass<B3DPipeline> {
    public B3DRenderPass(String name, B3DPipeline... pipelines) {
        super(name, pipelines);
    }

    @Override
    public void setup() {
        // nothing at the moment 
    }

    @Override
    public void render() {
        // nothing at the moment 
    }

    @Override
    public void cleanup() {
        // nothing at the moment 
    }

    @Override
    public void setUniform1i(String name, int value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform1i'");
    }

    @Override
    public void setUniform2i(String name, int x, int y) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform2i'");
    }

    @Override
    public void setUniform3i(String name, int x, int y, int z) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform3i'");
    }

    @Override
    public void setUniform4i(String name, int x, int y, int z, int w) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform4i'");
    }

    @Override
    public void setUniform1f(String name, float value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform1f'");
    }

    @Override
    public void setUniform2f(String name, float x, float y) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform2f'");
    }

    @Override
    public void setUniform3f(String name, float x, float y, float z) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform3f'");
    }

    @Override
    public void setUniform4f(String name, float x, float y, float z, float w) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniform4f'");
    }

    @Override
    public void setUniformMatrix3f(String name, float[] matrix) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniformMatrix3f'");
    }

    @Override
    public void setUniformMatrix4f(String name, float[] matrix) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUniformMatrix4f'");
    }

    @Override
    public void bindTexture(String name, int textureId, int slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bindTexture'");
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bindTextureArray'");
    }
}
