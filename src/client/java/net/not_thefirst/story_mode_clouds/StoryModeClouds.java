package net.not_thefirst.story_mode_clouds;

import net.fabricmc.api.ClientModInitializer;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ModKeybinds;
import net.not_thefirst.story_mode_clouds.renderer.render_types.ModRenderTypes;

public class StoryModeClouds implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CloudsConfiguration.load();
        ModKeybinds.initialize();
        ModRenderTypes.initialize();
    }
}
