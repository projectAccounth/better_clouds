package net.not_thefirst.story_mode_clouds.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.not_thefirst.story_mode_clouds.config.screens.YACLConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YACLConfigScreen::createConfigScreen;
    }
}