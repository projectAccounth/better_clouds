package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

import net.not_thefirst.lib.gl_render_system.alt.AbstractPipeline;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderTarget;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.lib.gl_render_system.alt.RenderTargetDescriptor;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

public final class GLFramebuffer extends AbstractRenderTarget {

    private final int fboId;
    private final boolean defaultFramebuffer;
    private final GLTexture2D colorAttachment;
    private final GLTexture2D depthAttachment;
    private static AbstractStaticMesh<?> quad;

    private GLFramebuffer(
        RenderTargetDescriptor descriptor,
        boolean useDepth,
        boolean useColor,
        boolean defaultFramebuffer,
        int fboId,
        GLTexture2D colorAttachment,
        GLTexture2D depthAttachment
    ) {
        super(descriptor, useDepth, useColor);
        this.defaultFramebuffer = defaultFramebuffer;
        this.fboId = fboId;
        this.colorAttachment = colorAttachment;
        this.depthAttachment = depthAttachment;
    }

    public static GLFramebuffer createDefaultFramebuffer(int width, int height) {
        return new GLFramebuffer(
            new RenderTargetDescriptor(width, height),
            false,
            true,
            true,
            0,
            null,
            null
        );
    }

    @Override
    public void clearColorAttachment(float r, float g, float b, float a) {
        if (closed) return;

        if (!isUseColor()) {
            throw new IllegalStateException("Render target was not created with color support");
        }
        bind();
        glClearBufferfv(GL_COLOR, 0, new float[]{r, g, b, a});
        unbind();
    }

    @Override
    public void clearDepthAttachment(float depth) {
        if (closed) return;
        
        if (!isUseDepth()) {
            throw new IllegalStateException("Render target was not created with depth support");
        }
        bind();
        glClearBufferfv(GL_DEPTH, 0, new float[]{depth});
        unbind();
    }

    public static GLFramebuffer createDepthOnlyFramebuffer(int width, int height) {
        int fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        GLTexture2D depthTexture = new GLTexture2D(width, height, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT);
        glBindTexture(GL_TEXTURE_2D, depthTexture.getId());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture.getId(), 0);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            depthTexture.close();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            throw new IllegalStateException("Framebuffer initialization failed and is incomplete.");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLFramebuffer target = new GLFramebuffer(
            new RenderTargetDescriptor(width, height),
            true,
            false,
            false,
            fboId,
            null,
            depthTexture
        );

        target.attachDepthTexture(depthTexture);
        return target;
    }

    public static GLFramebuffer createColorRenderTarget(int width, int height, int internalFormat, int format, int type) {
        int fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        GLTexture2D colorTexture = new GLTexture2D(width, height, internalFormat, format, type);
        glBindTexture(GL_TEXTURE_2D, colorTexture.getId());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture.getId(), 0);

        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glReadBuffer(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            colorTexture.close();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            throw new IllegalStateException("Framebuffer initialization failed and is incomplete.");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLFramebuffer target = new GLFramebuffer(
            new RenderTargetDescriptor(width, height),
            false,
            true,
            false,
            fboId,
            colorTexture,
            null
        );

        target.attachColorTexture(colorTexture);
        return target;
    }

    public static GLFramebuffer createColorDepthFramebuffer(int width, int height, int internalFormat, int format, int type) {
        int fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        GLTexture2D colorTexture = new GLTexture2D(width, height, internalFormat, format, type);
        glBindTexture(GL_TEXTURE_2D, colorTexture.getId());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture.getId(), 0);

        GLTexture2D depthTexture = new GLTexture2D(width, height, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT);
        glBindTexture(GL_TEXTURE_2D, depthTexture.getId());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture.getId(), 0);

        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glReadBuffer(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            colorTexture.close();
            depthTexture.close();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            throw new IllegalStateException("Framebuffer initialization failed and is incomplete.");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLFramebuffer target = new GLFramebuffer(
            new RenderTargetDescriptor(width, height),
            true,
            true,
            false,
            fboId,
            colorTexture,
            depthTexture
        );

        target.attachColorTexture(colorTexture);
        target.attachDepthTexture(depthTexture);
        return target;
    }

    @Override
    public void bind() {
        if (closed) return;
        if (defaultFramebuffer) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        }
        glViewport(0, 0, getWidth(), getHeight());
    }

    @Override
    public void unbind() {
        if (closed) return;
        if (!defaultFramebuffer) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    @Override
    public void copyColorFrom(AbstractRenderTarget other) {
        if (closed) return;

        super.copyColorFrom(other);

        if (!(other instanceof GLFramebuffer)) {
            throw new IllegalArgumentException("Can only combine with another GLFramebufferRenderTarget");
        }

        GLFramebuffer otherGL = (GLFramebuffer) other;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, otherGL.fboId);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.defaultFramebuffer ? 0 : this.fboId);

        glBlitFramebuffer(
            0, 0, getWidth(), getHeight(),
            0, 0, otherGL.getWidth(), otherGL.getHeight(),
            GL_COLOR_BUFFER_BIT,
            GL_NEAREST
        );

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }

    @Override
    public void copyDepthFrom(AbstractRenderTarget other) {
        if (closed) return;

        super.copyDepthFrom(other);

        if (!(other instanceof GLFramebuffer)) {
            throw new IllegalArgumentException("Can only combine with another GLFramebufferRenderTarget");
        }

        GLFramebuffer otherGL = (GLFramebuffer) other;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, otherGL.fboId);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.defaultFramebuffer ? 0 : this.fboId);

        glBlitFramebuffer(
            0, 0, getWidth(), getHeight(),
            0, 0, otherGL.getWidth(), otherGL.getHeight(),
            GL_DEPTH_BUFFER_BIT,
            GL_NEAREST
        );

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }

    @Override
    public void copyFrom(AbstractRenderTarget other) {
        if (closed) return;

        super.copyFrom(other);

        if (!(other instanceof GLFramebuffer)) {
            throw new IllegalArgumentException("Can only combine with another GLFramebufferRenderTarget");
        }

        GLFramebuffer otherGL = (GLFramebuffer) other;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, otherGL.fboId);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.defaultFramebuffer ? 0 : this.fboId);

        glBlitFramebuffer(
            0, 0, getWidth(), getHeight(),
            0, 0, otherGL.getWidth(), otherGL.getHeight(),
            GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT,
            GL_NEAREST
        );

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }

    @Override
    public void close() {
        if (closed) return;
        unbind();
        closed = true;
        if (!defaultFramebuffer) {
            glDeleteFramebuffers(fboId);
            if (colorAttachment != null) {
                colorAttachment.close();
            }
            if (depthAttachment != null) {
                depthAttachment.close();
            }
        }
    }

    @Override
    public GLTexture2D getColorAttachment() {
        return colorAttachment;
    }

    @Override
    public GLTexture2D getDepthAttachment() {
        return depthAttachment;
    }

    @Override
    public void resize(int w, int h) {
        if (closed) return;
        super.resize(w, h);

        if (colorAttachment != null)
            colorAttachment.resize(w, h);
        if (depthAttachment != null)
            depthAttachment.resize(w, h);

        if (colorAttachment != null || depthAttachment != null) {
            glBindFramebuffer(GL_FRAMEBUFFER, this.fboId);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("FBO failed completeness check after resize!");
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        glViewport(0, 0, getWidth(), getHeight());
    }

    @Override
    public void renderTo(AbstractPipeline pipeline, AbstractRenderTarget other) {
        if (quad == null) {
            quad = new GLMesh.Builder(VertexFormat.POSITION_TEX, GLPrimitive.TRIANGLES)
                .addVertex(0, 0, 0).setUv(0, 0)
                .addVertex(0, 0, 0).setUv(0, 0)
                .addVertex(0, 0, 0).setUv(0, 0)
                .build();
        }

        if (closed) return;

        if (!(other instanceof GLFramebuffer))
            throw new IllegalArgumentException();

        GLFramebuffer targ = (GLFramebuffer) other;

        try (AbstractRenderPass<?> pass = PipelineManager.getInstance().createRenderPass("Blit", new AbstractPipeline[]{pipeline})) {
            pass.setRenderTarget(targ);
            pass.setMesh(quad, quad.getIndexCount());

            pass.bindTexture("overrideColorTex", this.getColorAttachment(), 0);
            pass.bindTexture("overrideDepthTex", this.getDepthAttachment(), 1);
            pass.bindTexture("baseColorTex", targ.getColorAttachment(), 2);
            pass.bindTexture("baseDepthTex", targ.getDepthAttachment(), 3);

            pass.setup();
            pass.render();
        }
    }
}