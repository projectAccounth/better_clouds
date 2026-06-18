package net.not_thefirst.story_mode_clouds.utils.rendering.opengl;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.lib.gl_render_system.shader.ProgramManager;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;

public class GLResourceHandler {
    private GLResourceHandler() {}

    private static ProgramManager<IdentifierWrapper> programManager;

    public static void init(ResourceManager manager) {
        if (programManager != null) {
            throw new IllegalStateException("GLResourceHandler has already been initialized!");
        }

        programManager = new ProgramManager<>(new GLShaderProvider(manager));
    }

    public static ProgramManager<IdentifierWrapper> getProgramManager() {
        if (programManager == null) {
            throw new IllegalStateException("GLResourceHandler has not been initialized yet!");
        }
        return programManager;
    }
}
