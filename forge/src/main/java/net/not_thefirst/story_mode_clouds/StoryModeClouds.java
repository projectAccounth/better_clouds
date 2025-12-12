package net.not_thefirst.story_mode_clouds;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.ClothConfigClass;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("cloud_tweaks")
public class StoryModeClouds {
    public StoryModeClouds() {
        FMLJavaModLoadingContext.get()
            .getModEventBus()
            .addListener(StoryModeClouds::onFMLClientSetupEvent);
    }

    @SubscribeEvent
    public static void onFMLClientSetupEvent(FMLClientSetupEvent event) {
        Compat.init(new ForgeModChecker());
        Initializer.initialize();

        if (Compat.hasClothConfig()) {
            ConfigHolder<ClothConfigClass> holder = AutoConfig.register(ClothConfigClass.class, GsonConfigSerializer::new);
            
            holder.registerSaveListener((manager, data) -> {
                CloudsConfiguration.INSTANCE.applyFromCloth(data);
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
