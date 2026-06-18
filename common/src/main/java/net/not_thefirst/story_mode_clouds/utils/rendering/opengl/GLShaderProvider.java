package net.not_thefirst.story_mode_clouds.utils.rendering.opengl;

import java.io.InputStream;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.lib.gl_render_system.shader.ToStreamProvider;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;

class GLShaderProvider implements ToStreamProvider {
    private ResourceManager manager;

    public GLShaderProvider(ResourceManager manager) {
        this.manager = manager;
    }

    @Override
    public InputStream toStream(String path) {
        IdentifierWrapper loc = IdentifierWrapper.tryParse(path);
        try {
            return manager.getResource(loc.getDelegate()).orElseThrow(() -> 
                new IllegalStateException("Failed to find shader resource: " + loc)
            ).open();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
}
