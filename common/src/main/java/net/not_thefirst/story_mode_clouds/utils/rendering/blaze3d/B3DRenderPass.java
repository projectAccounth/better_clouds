package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;

public class B3DRenderPass extends AbstractRenderPass<B3DPipeline> {
    public B3DRenderPass(String name, B3DPipeline... pipelines) {
        super(name, pipelines);
        throw new UnsupportedOperationException("Blaze3D is not supported in 1.21.4 and below");
    }

    @Override
    public void setup() {
    }

    @Override
    public void render() {
    }


    @Override
    public void cleanup() {
    }

    @Override
    public void bindUniformBlock(String name, AbstractUBODataBuffer<?, ?> buffer) {
    }

    @Override
    public void bindTexture(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTexture'");
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTextureArray'");
    }

    @Override
    public void setMesh(AbstractStaticMesh<?> mesh, int indexCount) {
    }
}
