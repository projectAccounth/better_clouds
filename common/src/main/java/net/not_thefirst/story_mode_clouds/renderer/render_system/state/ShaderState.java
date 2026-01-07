package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.GLProgram;

public final class ShaderState implements RenderState {

    private final GLProgram program;

    public ShaderState(GLProgram program) {
        this.program = program;
    }

    @Override
    public void apply() {
        program.use();
    }

    @Override
    public void clear() {
        program.stop();
    }

    public GLProgram program() {
        return program;
    }
}
