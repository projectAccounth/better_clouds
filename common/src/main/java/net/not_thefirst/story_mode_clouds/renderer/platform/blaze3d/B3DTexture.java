package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import net.not_thefirst.lib.gl_render_system.alt.AbstractTexture;

public class B3DTexture extends AbstractTexture {

    private GpuTexture backend;

    B3DTexture(String name, int width, int height, int usage, GpuFormat format) {
        super(GL11.GL_TEXTURE_2D, width, height);

        GpuDevice device = RenderSystem.getDevice();
        this.backend = device.createTexture(name, usage, format, width, height, 0, 0);
    }

    @Override
    public void bind(int unit) {
        super.bind(unit);
    }

    @Override
    public void unbind(int unit) {
        super.unbind(unit);
    }

    public GpuTexture getBackend() {
        return backend;
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        bind(0);

        unbind(0);
    }

    @Override
    public void close() {
        backend.close();
    }

    @Override
    public void data(int level, int xoffset, int yoffset, int width, int height, int format, int type,
            ByteBuffer data) {
        throw new UnsupportedOperationException("Unimplemented method 'data'");
    }
}
