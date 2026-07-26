package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;


import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.TextureFactory;

public class B3DTextureFactory implements TextureFactory<B3DTexture> {

    @Override
    public B3DTexture create(String name, int target, int width, int height) {
        return new B3DTexture(name, width, height, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC, GpuFormat.RGBA8_UNORM);
    }
    
}
