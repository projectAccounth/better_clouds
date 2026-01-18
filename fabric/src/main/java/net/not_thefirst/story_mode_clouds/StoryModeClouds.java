package net.not_thefirst.story_mode_clouds;

import net.fabricmc.api.ClientModInitializer;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.FabricModChecker;

public class StoryModeClouds implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Compat.init(new FabricModChecker());
        Initializer.initialize();
    }
}
