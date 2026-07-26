package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.RenderTargetFactory;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import net.not_thefirst.lib.gl_render_system.alt.RenderTargetDescriptor;

public class GLRenderTargetFactory implements RenderTargetFactory<GLFramebuffer> {

    @Override
    public GLFramebuffer create(String name, RenderTargetDescriptor descriptor, boolean useDepth, boolean useColor) {
        if (!useDepth) {
            if (useColor) {
                return GLFramebuffer.createColorRenderTarget(descriptor.getWidth(), descriptor.getHeight(), GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            }
        }
        else {
            if (useColor) {
                return GLFramebuffer.createColorDepthFramebuffer(descriptor.getWidth(), descriptor.getHeight(), GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            }
            else {
                return GLFramebuffer.createDepthOnlyFramebuffer(descriptor.getWidth(), descriptor.getHeight());
            }
        }
        return null;
    }

    @Override
    public GLFramebuffer create(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, true, true);
    }

    @Override
    public GLFramebuffer createDepth(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, true, false);
    }

    @Override
    public GLFramebuffer createColor(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, false, true);
    }

    @Override
    public GLFramebuffer createDefault(String name, RenderTargetDescriptor descriptor) {
        return GLFramebuffer.createDefaultFramebuffer(descriptor.getWidth(), descriptor.getHeight());
    }
    
}
