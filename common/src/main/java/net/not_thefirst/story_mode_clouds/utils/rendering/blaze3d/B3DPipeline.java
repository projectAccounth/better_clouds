package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractPipeline;

public class B3DPipeline extends AbstractPipeline {
    private RenderPipeline pipeline;
    public B3DPipeline(String name) {
        super(name);
    }

    @Override
    public void setup() {
        // nothing at the moment 
    }

    @Override
    public void bind() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bind'");
    }

    @Override
    public void unbind() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unbind'");
    }
    
    public static class Builder extends AbstractPipeline.Builder<B3DPipeline> {
        public Builder(String name) {
            super(name);
        }

        @Override
        public B3DPipeline build() {
            B3DPipeline pipeline = new B3DPipeline(name);
            for (String uniform : uniforms) {
                pipeline.addUniform(uniform);
            }
            for (var entry : uniformBlocks.entrySet()) {
                pipeline.addUniformBlock(entry.getKey(), entry.getValue());
            }
            return pipeline;
        }
    }
}
