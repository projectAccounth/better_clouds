package net.not_thefirst.story_mode_clouds.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.api.Environment;

@Environment(net.fabricmc.api.EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CloudsConfiguration::createConfigScreen;
    }
}