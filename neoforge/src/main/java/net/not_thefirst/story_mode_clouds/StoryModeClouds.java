package net.not_thefirst.story_mode_clouds;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.YACLConfigScreen;
import net.neoforged.neoforge.client.ConfigScreenHandler;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds() {
        StoryModeClouds.initialize();
        
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, prevScreen) -> YACLConfigScreen.createConfigScreen(prevScreen))
        );
    }

    public static void initialize() {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();
    }
}
