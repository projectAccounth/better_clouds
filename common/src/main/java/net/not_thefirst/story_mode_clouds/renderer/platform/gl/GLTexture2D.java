package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;

import net.not_thefirst.lib.gl_render_system.alt.AbstractTexture;

final class GLTexture2D extends AbstractTexture {

    private final int textureId;
    private final int internalFormat;
    private final int format;
    private final int type;

    GLTexture2D(int width, int height, int internalFormat, int format, int type) {
        super(GL_TEXTURE_2D, width, height);
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
        this.textureId = glGenTextures();

        bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        unbind(0);
    }

    @Override
    public void bind(int unit) {
        super.bind(unit);
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(getTarget(), textureId);
    }

    @Override
    public void unbind(int unit) {
        super.unbind(unit);
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(getTarget(), 0);
    }

    public int getId() {
        return textureId;
    }

    public int getInternalFormat() {
        return internalFormat;
    }

    public int getFormat() {
        return format;
    }

    public int getType() {
        return type;
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, w, h, 0, format, type, (ByteBuffer) null);
        unbind(0);
    }

    @Override
    public void close() {
        bound.stream().forEach((i) -> {
            unbind(i);
        });
        glDeleteTextures(textureId);
    }

    @Override
    public void data(int level, int xoffset, int yoffset, int width, int height, int format, int type,
            ByteBuffer data) {
        // partially implemented
        if (!data.isDirect())
            throw new IllegalArgumentException("Attempted to use a heap-allocated buffer for GPU data");
        bind(0);
        glTexSubImage2D(getTarget(), level, xoffset, yoffset, width, height, format, type, data);
        unbind(0);
    }
}
