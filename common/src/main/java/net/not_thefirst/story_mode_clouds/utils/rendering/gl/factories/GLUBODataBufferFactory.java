package net.not_thefirst.story_mode_clouds.utils.rendering.gl.factories;

import net.not_thefirst.story_mode_clouds.utils.rendering.gl.GLUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.DataBufferFactory;

import java.nio.ByteBuffer;

public class GLUBODataBufferFactory implements DataBufferFactory<GLUBODataBuffer, ByteBuffer> {
    @Override
    public GLUBODataBuffer create(String name, int size) {
        return new GLUBODataBuffer(name, size);
    }
}
