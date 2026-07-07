package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.factories;

import net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d.B3DUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.DataBufferFactory;

import com.mojang.blaze3d.buffers.GpuBuffer;

public class B3DUBODataBufferFactory implements DataBufferFactory<B3DUBODataBuffer, GpuBuffer> {
    @Override
    public B3DUBODataBuffer create(String name, int size) {
        return new B3DUBODataBuffer(name, size);
    }
}
