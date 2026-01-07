package net.not_thefirst.story_mode_clouds.renderer.render_system.shader;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

// Testbed for whatever I'm doing
public final class UniformBufferObject implements AutoCloseable {

    private final int bufferId;
    private final int binding;
    private final int size;

    private boolean closed = false;

    public UniformBufferObject(int binding, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("UBO size must be > 0");
        }
        if ((size & 0xF) != 0) {
            throw new IllegalArgumentException(
                "UBO size must be 16-byte aligned for std140 (got " + size + ")"
            );
        }

        this.binding = binding;
        this.size = size;

        bufferId = GL15.glGenBuffers();
        if (bufferId == 0) {
            throw new IllegalStateException("Failed to generate UBO");
        }

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bufferId);
        GL15.glBufferData(
            GL31.GL_UNIFORM_BUFFER,
            size,
            GL15.GL_DYNAMIC_DRAW
        );
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, bufferId);

        // debugiegng
        if (GL.getCapabilities().GL_KHR_debug) {
            GL43.glObjectLabel(
                GL43.GL_BUFFER,
                bufferId,
                "UBO(binding=" + binding + ", size=" + size + ")"
            );
        }
    }

    private void checkAlive() {
        if (closed) {
            throw new IllegalStateException("UBO already closed");
        }
    }

    public int binding() {
        return binding;
    }

    public int size() {
        return size;
    }

    /** 
     * Full update
     */
    public void update(ByteBuffer data) {
        update(0, data);
    }

    /** 
     * Partial update 
     */
    public void update(int offsetBytes, ByteBuffer data) {
        checkAlive();

        if (offsetBytes < 0) {
            throw new IllegalArgumentException("Negative offset");
        }

        int byteSize = data.remaining();
        int end = offsetBytes + byteSize;

        if (end > size) {
            throw new IllegalArgumentException(
                "UBO overflow: write " + end + " > capacity " + size
            );
        }

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bufferId);
        GL15.glBufferSubData(
            GL31.GL_UNIFORM_BUFFER,
            offsetBytes,
            data
        );
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    /** 
     * Explicit rebind 
     */
    public void bind() {
        checkAlive();
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, bufferId);
    }

    @Override
    public void close() {
        if (!closed) {
            GL15.glDeleteBuffers(bufferId);
            closed = true;
        }
    }
}


