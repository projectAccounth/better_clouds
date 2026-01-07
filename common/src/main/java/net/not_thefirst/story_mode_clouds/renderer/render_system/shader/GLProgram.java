package net.not_thefirst.story_mode_clouds.renderer.render_system.shader;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import com.mojang.math.Matrix4f;

public final class GLProgram implements AutoCloseable {

    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private boolean inUse = false;

    public GLProgram(int programId) {
        this.programId = programId;
    }

    public void use() {
        if (!inUse) {
            GL20.glUseProgram(programId);
            inUse = true;
        }
    }

    public void stop() {
        if (inUse) {
            GL20.glUseProgram(0);
            inUse = false;
        }
    }

    @Override
    public void close() {
        stop();
        GL20.glDeleteProgram(programId);
    }

    public int getId() {
        return programId;
    }

    @VisibleForTesting
    public int uniform(String name) {
        Integer cached = uniformLocations.get(name);
        if (cached != null) {
            return cached;
        }

        int loc = GL20.glGetUniformLocation(programId, name);
        uniformLocations.put(name, loc);
        return loc;
    }

    public void setBool(String name, boolean v) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform1i(loc, v ? 1 : 0);
        }
    }

    public void setInt(String name, int v) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform1i(loc, v);
        }
    }

    public void setFloat(String name, float v) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform1f(loc, v);
        }
    }

    public void setVec2(String name, float x, float y) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform2f(loc, x, y);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform3f(loc, x, y, z);
        }
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform4f(loc, x, y, z, w);
        }
    }

    public void setMat4(String name, Matrix4f mat) {
        int loc = uniform(name);
        if (loc >= 0) {
            FloatBuffer fb = BufferUtils.createFloatBuffer(16);
            mat.store(fb);
            // fb.flip();
            GL20.glUniformMatrix4fv(loc, false, fb);
        }
    }

    public void setSampler(String name, int textureUnit) {
        int loc = uniform(name);
        if (loc >= 0) {
            GL20.glUniform1i(loc, textureUnit);
        }
    }

    public void validate() {
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(
                "Program validation failed:\n" +
                GL20.glGetProgramInfoLog(programId)
            );
        }
    }

    public void bindUniformBlock(String name, int binding) {
        int index = GL31.glGetUniformBlockIndex(programId, name);
        if (index >= 0) {
            GL31.glUniformBlockBinding(programId, index, binding);
        }
    }

    public void requireUniformBlock(String name) {
        int index = GL31.glGetUniformBlockIndex(programId, name);
        if (index < 0) {
            throw new IllegalStateException(
                "Required uniform block missing: " + name
            );
        }
    }

    public GLProgram useScoped() {
        use();
        return this;
    }
}
