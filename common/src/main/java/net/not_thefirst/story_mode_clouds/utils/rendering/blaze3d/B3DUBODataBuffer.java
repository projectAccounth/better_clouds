package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import org.joml.Matrix4f;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;

public class B3DUBODataBuffer extends AbstractUBODataBuffer<B3DUBODataBuffer, Object> {

    public B3DUBODataBuffer(String name, int size) {
        super(name, size);
    }

    public B3DUBODataBuffer putFloat(float v) {
        return this;
    }

    public B3DUBODataBuffer putInt(int v) {
        return this;
    }

    public B3DUBODataBuffer putVec2(float x, float y) {
        return this;
    }

    public B3DUBODataBuffer putVec3(float x, float y, float z) {
        return this;
    }

    public B3DUBODataBuffer putVec4(float x, float y, float z, float w) {
        return this;
    }

    public B3DUBODataBuffer putIVec4(int x, int y, int z, int w) {
        return this;
    }

    public B3DUBODataBuffer putMat4(Matrix4f mat) {
        return this;
    }

    @Override
    public Object build() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void close() {
    }
}
