package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import java.io.InputStream;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.lib.gl_render_system.shader.ToStreamProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

class GLShaderProvider implements ToStreamProvider {
    private ResourceManager manager;

    GLShaderProvider(ResourceManager manager) {
        this.manager = manager;
    }

    @Override
    public InputStream toStream(String path) {
        try {
            return manager.open(IdentifierWrapper.tryParse(path).getDelegate());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
}
