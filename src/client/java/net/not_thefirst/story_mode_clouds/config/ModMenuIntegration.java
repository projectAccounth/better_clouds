package net.not_thefirst.story_mode_clouds.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.api.ClothConfigScreen;
import net.not_thefirst.story_mode_clouds.compat.Compat;

public class ModMenuIntegration implements ModMenuApi {

    public static Screen createConfigScreen(Screen parent) {
        if (Compat.hasClothConfig()) {
            return ClothConfigScreen.create(parent);
        } else {
            return new CloudsConfigScreen(parent);
        }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }
}