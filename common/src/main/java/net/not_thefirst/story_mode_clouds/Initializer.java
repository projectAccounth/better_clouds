package net.not_thefirst.story_mode_clouds;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class Initializer {
    public static final String MOD_ID = "cloud_tweaks";
    
    private Initializer() {}

    public static void initialize() {
        CloudsConfiguration.load();
    }

    public static String j(String s) {
        return "j" + s;
    }
}