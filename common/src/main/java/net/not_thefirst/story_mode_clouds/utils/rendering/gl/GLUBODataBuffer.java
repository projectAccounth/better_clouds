package net.not_thefirst.story_mode_clouds.utils.rendering.gl;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;

import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.shader.Std140BufferBuilder;

public class GLUBODataBuffer extends AbstractUBODataBuffer<GLUBODataBuffer, ByteBuffer> {

    private Std140BufferBuilder bufferBuilder;

    public GLUBODataBuffer(String name, int size) {
        super(name, size);
        this.bufferBuilder = new Std140BufferBuilder(size);
    }
    
    public Std140BufferBuilder getUBO() {
        return bufferBuilder;
    }

    public GLUBODataBuffer putFloat(float v) {
        bufferBuilder.putFloat(v);
        return this;
    }

    public GLUBODataBuffer putInt(int v) {
        bufferBuilder.putInt(v);
        return this;
    }

    public GLUBODataBuffer putVec2(float x, float y) {
        bufferBuilder.putVec2(x, y);
        return this;
    }

    public GLUBODataBuffer putVec3(float x, float y, float z) {
        bufferBuilder.putVec3(x, y, z);
        return this;
    }

    public GLUBODataBuffer putVec4(float x, float y, float z, float w) {
        bufferBuilder.putVec4(x, y, z, w);
        return this;
    }

    public GLUBODataBuffer putIVec4(int x, int y, int z, int w) {
        bufferBuilder.putIVec4(x, y, z, w);
        return this;
    }

    public GLUBODataBuffer putMat4(Matrix4f mat) {
        bufferBuilder.putMat4(mat);
        return this;
    }

    public void reset() {
        bufferBuilder.reset();
    }

    public ByteBuffer build() {
        return bufferBuilder.build();
    }
}
