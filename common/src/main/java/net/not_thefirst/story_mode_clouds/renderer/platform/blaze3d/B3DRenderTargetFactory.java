package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import net.not_thefirst.lib.gl_render_system.alt.PipelineManager.RenderTargetFactory;
import net.not_thefirst.lib.gl_render_system.alt.RenderTargetDescriptor;

public class B3DRenderTargetFactory implements RenderTargetFactory<B3DRenderTarget> {

    @Override
    public B3DRenderTarget create(String name, RenderTargetDescriptor descriptor, boolean useDepth, boolean useColor) {
        if (!useDepth) {
            if (useColor) {
                return B3DRenderTarget.createColorTarget(name, descriptor.getWidth(), descriptor.getHeight());
            }
        }
        else {
            if (useColor) {
                return B3DRenderTarget.createColorDepthTarget(name, descriptor.getWidth(), descriptor.getHeight());
            }
            else {
                return B3DRenderTarget.createDepthTarget(name, descriptor.getWidth(), descriptor.getHeight());
            }
        }
        return null;
    }

    @Override
    public B3DRenderTarget create(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, true, true);
    }

    @Override
    public B3DRenderTarget createDepth(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, true, false);
    }

    @Override
    public B3DRenderTarget createColor(String name, RenderTargetDescriptor descriptor) {
        return create(name, descriptor, false, true);
    }

    @Override
    public B3DRenderTarget createDefault(String name, RenderTargetDescriptor descriptor) {
        return B3DRenderTarget.createDefaultTarget(descriptor.getWidth(), descriptor.getHeight());
    }
    
}
