package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice.MappedView;

import net.minecraft.client.renderer.MappableRingBuffer;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;

public class B3DUBODataBuffer extends AbstractUBODataBuffer<B3DUBODataBuffer, GpuBuffer> {

    private MappableRingBuffer buffer;
    private MappedView view;
    private boolean active = false;

    B3DUBODataBuffer(String name, int size) {
        super(name, size);
        this.buffer = new MappableRingBuffer(() -> name, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, size);
    }
    
    public MappableRingBuffer getUBO() {
        return buffer;
    }

    private MappedView getMappedView() {
        if (view == null || !active) {
            view = buffer.currentBuffer().map(/*read=*/false, /*write=*/true);
            active = true;
        }
        return view;
    }

    private Std140Builder getCurrent() {
        return Std140Builder.intoBuffer(getMappedView().data());
    }

    public B3DUBODataBuffer putFloat(float v) {
        getCurrent().putFloat(v);
        return this;
    }

    public B3DUBODataBuffer putInt(int v) {
        getCurrent().putInt(v);
        return this;
    }

    public B3DUBODataBuffer putVec2(float x, float y) {
        getCurrent().putVec2(x, y);
        return this;
    }

    public B3DUBODataBuffer putVec3(float x, float y, float z) {
        getCurrent().putVec3(x, y, z);
        return this;
    }

    public B3DUBODataBuffer putVec4(float x, float y, float z, float w) {
        getCurrent().putVec4(x, y, z, w);
        return this;
    }

    public B3DUBODataBuffer putIVec4(int x, int y, int z, int w) {
        getCurrent().putIVec4(x, y, z, w);
        return this;
    }

    public B3DUBODataBuffer putMat4(Matrix4f mat) {
        getCurrent().putMat4f(mat);
        return this;
    }

    private void clean() {
        if (!active) return;
        active = false;
        view.close();
    }

    @Override
    public GpuBuffer build() {
        clean();
        return buffer.currentBuffer();
    }

    @Override
    public void reset() {
        clean();
        buffer.rotate();
    }

    @Override
    public void close() {
        super.close();
        clean();
        buffer.close();
    }
}
