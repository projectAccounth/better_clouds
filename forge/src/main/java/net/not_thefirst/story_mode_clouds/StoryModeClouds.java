package net.not_thefirst.story_mode_clouds;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds() {
        if (FMLEnvironment.dist.isClient()) {
            StoryModeClouds.initialize(ModLoadingContext.get());
        }
    }

    private static void initialize(ModLoadingContext ctx) {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();

        ctx.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, prevScreen) -> CloudsConfiguration.createConfigScreen(prevScreen))
        );
    }
}
