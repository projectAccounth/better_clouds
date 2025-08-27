package net.not_thefirst.story_mode_clouds;

import net.fabricmc.api.ClientModInitializer;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ModKeybinds;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;

public class StoryModeClouds implements ClientModInitializer {
    public static final String MOD_ID = "cloud_tweaks";
    @Override
    public void onInitializeClient() {
        CloudsConfiguration.load();
        ModKeybinds.initialize();
        ModRenderPipelines.registerCloudPipelines();
    }
}
