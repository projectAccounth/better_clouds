package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice.MappedView;

import net.minecraft.client.renderer.MappableRingBuffer;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractUBODataBuffer;

public class B3DUBODataBuffer extends AbstractUBODataBuffer {

    private MappableRingBuffer buffer;

    public B3DUBODataBuffer(String name, int size) {
        super(name, size);
        this.buffer = new MappableRingBuffer(() -> name, 130, size);
    }
    
    public MappableRingBuffer getUBO() {
        return buffer;
    }

    private MappedView getMappedView() {
        return buffer.currentBuffer().map(/*read=*/false, /*write=*/true);
    }

    private Std140Builder getBuffer() {
        MappedView view = getMappedView();
        return Std140Builder.intoBuffer(view.data());
    }

    public B3DUBODataBuffer putFloat(float v) {
        getBuffer().putFloat(v);
        return this;
    }

    public B3DUBODataBuffer putInt(int v) {
        getBuffer().putInt(v);
        return this;
    }

    public B3DUBODataBuffer putVec2(float x, float y) {
        getBuffer().putVec2(x, y);
        return this;
    }

    public B3DUBODataBuffer putVec3(float x, float y, float z) {
        getBuffer().putVec3(x, y, z);
        return this;
    }

    public B3DUBODataBuffer putVec4(float x, float y, float z, float w) {
        getBuffer().putVec4(x, y, z, w);
        return this;
    }

    public B3DUBODataBuffer putIVec4(int x, int y, int z, int w) {
        getBuffer().putIVec4(x, y, z, w);
        return this;
    }

    public B3DUBODataBuffer putMat4(Matrix4f mat) {
        getBuffer().putMat4f(mat);
        return this;
    }

    public GpuBuffer build() {
        return buffer.currentBuffer();
    }
}
