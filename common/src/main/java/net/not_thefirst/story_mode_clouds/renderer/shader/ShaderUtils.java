package net.not_thefirst.story_mode_clouds.renderer.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ShaderUtils {

    private ShaderUtils() {}

    public static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed:\n" + log);
        }

        return shader;
    }

    public static GLProgram create(String vertexSrc, String fragmentSrc) {
        int vert = compile(GL20.GL_VERTEX_SHADER, vertexSrc);
        int frag = compile(GL20.GL_FRAGMENT_SHADER, fragmentSrc);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new RuntimeException("Program link failed:\n" + log);
        }

        return new GLProgram(program);
    }

    public static GLProgram create(
        ResourceManager rm,
        ResourceLocation vertex,
        ResourceLocation fragment) {

        String vertSrc = ShaderSourceLoader.load(rm, vertex);
        String fragSrc = ShaderSourceLoader.load(rm, fragment);

        return create(vertSrc, fragSrc);
    }

}
