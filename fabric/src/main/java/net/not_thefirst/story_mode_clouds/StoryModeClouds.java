package net.not_thefirst.story_mode_clouds;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.InteractionResult;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.FabricModChecker;
import net.not_thefirst.story_mode_clouds.config.ClothConfigClass;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class StoryModeClouds implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Compat.init(new FabricModChecker());
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
    }
}
