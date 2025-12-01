package net.not_thefirst.story_mode_clouds;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.InteractionResult;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.config.ClothConfigClass;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ModKeybinds;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;

public class StoryModeClouds implements ClientModInitializer {
    public static final String MOD_ID = "cloud_tweaks";
    @Override
    public void onInitializeClient() {
        CloudsConfiguration.load();
        ModKeybinds.initialize();
        ModRenderPipelines.registerCloudPipelines();
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
    }
}
