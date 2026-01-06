package net.not_thefirst.story_mode_clouds.renderer.shader;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.mojang.math.Matrix4f;

public final class Std140BufferBuilder {

    private final ByteBuffer buffer;

    public Std140BufferBuilder(int size) {
        buffer = BufferUtils.createByteBuffer(size);
    }

    private void align(int align) {
        int pos = buffer.position();
        int aligned = (pos + align - 1) & ~(align - 1);
        buffer.position(aligned);
    }

    public Std140BufferBuilder putFloat(float v) {
        align(4);
        buffer.putFloat(v);
        return this;
    }

    public Std140BufferBuilder putInt(int v) {
        align(4);
        buffer.putInt(v);
        return this;
    }

    public Std140BufferBuilder putVec2(float x, float y) {
        align(8);
        buffer.putFloat(x);
        buffer.putFloat(y);
        return this;
    }

    public Std140BufferBuilder putVec3(float x, float y, float z) {
        align(16);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(0.0f); // padding
        return this;
    }

    public Std140BufferBuilder putVec4(float x, float y, float z, float w) {
        align(16);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(w);
        return this;
    }

    public Std140BufferBuilder putIVec4(int x, int y, int z, int w) {
        align(16);
        buffer.putInt(x);
        buffer.putInt(y);
        buffer.putInt(z);
        buffer.putInt(w);
        return this;
    }

    public Std140BufferBuilder putMat4(Matrix4f mat) {
        align(16);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        mat.store(fb);
        fb.flip();

        while (fb.hasRemaining()) {
            buffer.putFloat(fb.get());
        }
        return this;
    }

    public ByteBuffer build() {
        buffer.flip();
        return buffer;
    }
}
