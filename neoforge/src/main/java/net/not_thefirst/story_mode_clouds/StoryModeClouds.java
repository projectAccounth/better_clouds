package net.not_thefirst.story_mode_clouds;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.compat.ForgeModChecker;
import net.not_thefirst.story_mode_clouds.config.ClothConfigClass;
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
