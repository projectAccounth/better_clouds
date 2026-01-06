package net.not_thefirst.story_mode_clouds.renderer.shader;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

// Testbed for whatever I'm doing
public final class UniformBlock implements AutoCloseable {

    private final int bufferId;
    private final int binding;
    private final int size;

    public UniformBlock(int binding, int size) {
        this.binding = binding;
        this.size = size;

        bufferId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bufferId);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, bufferId);
    }

    public void update(FloatBuffer data) {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bufferId);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void close() {
        GL15.glDeleteBuffers(bufferId);
    }
}

