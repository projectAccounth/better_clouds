package net.not_thefirst.story_mode_clouds;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.ClothConfigClass;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

        if (Compat.hasClothConfig()) {
            ConfigHolder<ClothConfigClass> holder = AutoConfig.register(ClothConfigClass.class, GsonConfigSerializer::new);
            
            holder.registerSaveListener((manager, data) -> {
                CloudsConfiguration.INSTANCE.applyFromCloth(data);
                CloudsConfiguration.save();
                return InteractionResult.SUCCESS;
            });

            holder.registerLoadListener((manager, data) -> {
                data.applyFromMain(CloudsConfiguration.INSTANCE);
                return InteractionResult.SUCCESS;
            });
        }

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, prevScreen) -> CloudsConfiguration.createConfigScreen(prevScreen))
        );
    }
}
