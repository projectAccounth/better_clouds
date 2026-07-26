package net.not_thefirst.story_mode_clouds.renderer.platform.gl;


import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.TextureFactory;

public class GLTextureFactory implements TextureFactory<GLTexture2D> {

    @Override
    public GLTexture2D create(String name, int target, int width, int height) {
        return new GLTexture2D(width, height, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
    }
    
}
