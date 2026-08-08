package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import org.joml.Vector4f;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderTarget;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.RenderTargetDescriptor;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

public class B3DRenderTarget extends AbstractRenderTarget {

    private RenderTarget internal;
    private static AbstractStaticMesh<?> quad;

    B3DRenderTarget(RenderTargetDescriptor descriptor, boolean useDepth, boolean useColor) {
        super(descriptor, useDepth, useColor);
    }

    public static B3DRenderTarget createDefaultTarget(int width, int height) {
        B3DRenderTarget target = new B3DRenderTarget(new RenderTargetDescriptor(width, height), true, true);
        target.internal = ClientHelper.GameRendererHelper.getRenderer().mainRenderTarget();
        return target;
    }

    public static B3DRenderTarget createColorDepthTarget(String name, int width, int height) {
        B3DRenderTarget target = new B3DRenderTarget(new RenderTargetDescriptor(width, height), true, true);

        target.internal = new TextureTarget(name, width, height, true, GpuFormat.RGBA8_UNORM);

        return target;
    }

    public static B3DRenderTarget createDepthTarget(String name, int width, int height) {
        B3DRenderTarget target = new B3DRenderTarget(new RenderTargetDescriptor(width, height), true, false);

        target.internal = new TextureTarget(name, width, height, true, GpuFormat.RGBA8_UNORM);

        return target;
    }

    public static B3DRenderTarget createColorTarget(String name, int width, int height) {
        B3DRenderTarget target = new B3DRenderTarget(new RenderTargetDescriptor(width, height), false, true);

        target.internal = new TextureTarget(name, width, height, false, GpuFormat.RGBA8_UNORM);

        return target;
    }

    public RenderTarget getInternal() {
        return internal;
    }

    @Override
    public void clearColorAttachment(float r, float g, float b, float a) {
        super.clearColorAttachment(r, g, b, a);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(internal.getColorTexture(), new Vector4f(r, g, b, a));
    }

    @Override
    public void clearDepthAttachment(float depth) {
        super.clearDepthAttachment(depth);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(internal.getDepthTexture(), depth);
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        this.internal.resize(w, h);
    }

    @Override
    public void renderTo(AbstractPipeline pipeline, AbstractRenderTarget other) {
        if (quad == null) {
            quad = new B3DMesh.Builder(VertexFormat.POSITION_TEX, GLPrimitive.TRIANGLES)
                .addVertex(0, 0, 0).setUv(0, 0)
                .addVertex(0, 0, 0).setUv(0, 0)
                .addVertex(0, 0, 0).setUv(0, 0)
                .build();
        }

        if (!(pipeline instanceof B3DPipeline pip)) throw new IllegalArgumentException("Incompatible type passed in");
        if (!(other instanceof B3DRenderTarget tar)) throw new IllegalArgumentException("Incompatible type passed in");

        try (B3DRenderPass pass = new B3DRenderPass("Blit", pip)) {
            pass.setRenderTarget(tar);
            pass.setMesh(quad, quad.getIndexCount());
 
            pass.setup();

            pass.getPass().bindTexture("colorTex1", this.internal.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.getPass().bindTexture("depthTex1", this.internal.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

            pass.getPass().bindTexture("colorTex0", tar.internal.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.getPass().bindTexture("depthTex0", tar.internal.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            
            pass.render();
            pass.cleanup();
        }
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException("Unsupported method 'bind'");
    }

    @Override
    public void unbind() {
        throw new UnsupportedOperationException("Unsupported method 'unbind'");
    }

    @Override
    public void close() {
        this.internal.destroyBuffers();
    }
    
}
