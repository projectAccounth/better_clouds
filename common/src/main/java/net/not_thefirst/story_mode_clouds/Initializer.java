package net.not_thefirst.story_mode_clouds;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class Initializer {
    public static void initialize() {
        CloudsConfiguration.load();
    }
};