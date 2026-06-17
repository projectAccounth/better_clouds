package net.not_thefirst.story_mode_clouds.utils.rendering.opengl;

import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractPipeline;

public class GLPipeline extends AbstractPipeline {

    public GLPipeline(String name) {
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
 
    public static class Builder extends AbstractPipeline.Builder<GLPipeline> {
        public Builder(String name) {
            super(name);
        }

        @Override
        public GLPipeline build() {
            GLPipeline pipeline = new GLPipeline(name);
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
