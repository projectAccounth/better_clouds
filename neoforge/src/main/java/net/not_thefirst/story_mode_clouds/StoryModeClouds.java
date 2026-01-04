package net.not_thefirst.story_mode_clouds;

import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.neoforged.neoforge.client.ConfigScreenHandler;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds() {
        ModLoadingContext ctx = ModLoadingContext.get();
        StoryModeClouds.initialize();

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, prevScreen) -> CloudsConfiguration.createConfigScreen(prevScreen))
        );
    }

    public static void initialize() {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();
    }
}
