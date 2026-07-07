package net.not_thefirst.story_mode_clouds;

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