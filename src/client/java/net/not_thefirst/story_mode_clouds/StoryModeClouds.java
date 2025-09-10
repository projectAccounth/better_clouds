package net.not_thefirst.story_mode_clouds;

import net.fabricmc.api.ClientModInitializer;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ModKeybinds;
public class StoryModeClouds implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CloudsConfiguration.load();
        ModKeybinds.initialize();
    }
}
