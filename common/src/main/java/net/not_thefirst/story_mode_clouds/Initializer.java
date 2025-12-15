package net.not_thefirst.story_mode_clouds;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class Initializer {
    public static final String MOD_ID = "cloud_tweaks";

    public static void initialize() {
        CloudsConfiguration.load();
    }
};