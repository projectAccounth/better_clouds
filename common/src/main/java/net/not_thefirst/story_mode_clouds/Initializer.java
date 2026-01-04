package net.not_thefirst.story_mode_clouds;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;

public class Initializer {
    public static final String MOD_ID = "cloud_tweaks";

    public static void initialize() {
        CloudsConfiguration.load();
        ModRenderPipelines.registerCloudPipelines();
    }

    public static String j() {
        return "j";
    }
};