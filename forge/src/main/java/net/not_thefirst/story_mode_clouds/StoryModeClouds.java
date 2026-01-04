package net.not_thefirst.story_mode_clouds;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds(FMLJavaModLoadingContext ctx) {
        if (FMLEnvironment.dist.isClient()) {
            StoryModeClouds.initialize(ctx);
        }
    }

    private static void initialize(FMLJavaModLoadingContext ctx) {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();

        ctx.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, prevScreen) -> CloudsConfiguration.createConfigScreen(prevScreen))
        );
    }
}
