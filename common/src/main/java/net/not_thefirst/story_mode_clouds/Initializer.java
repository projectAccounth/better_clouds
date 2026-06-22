package net.not_thefirst.story_mode_clouds;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.pipelines.gl.GLPipelines;

public class Initializer {
    public static final String MOD_ID = "cloud_tweaks";
    
    private Initializer() {}

    public static void initialize() {
        CloudsConfiguration.load();
        GLPipelines.init();
    }

    public static String j(String s) {
        return "j" + s;
    }
}