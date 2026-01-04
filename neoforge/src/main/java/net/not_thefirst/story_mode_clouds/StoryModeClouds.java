package net.not_thefirst.story_mode_clouds;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds() {
        ModLoadingContext ctx = ModLoadingContext.get();
        StoryModeClouds.initialize();
        
        ctx.getActiveContainer().registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> {
            return CloudsConfiguration.createConfigScreen(parent);
        });
    }

    public static void initialize() {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();
    }
}
