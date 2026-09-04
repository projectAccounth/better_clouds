package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.controllers.PresetController;
import net.not_thefirst.story_mode_clouds.config.presets.controllers.LayerPresets;

public class ConfigScreen {
    private ConfigScreen() {}

    public static Screen createConfigScreen(Screen parent) {
        var config = CloudsConfiguration.getInstance();
        PresetController.initialize();
        LayerPresets.initialize();
        final Screen[] self = new Screen[1];

        var backend = BackendHolder.getBackend();
        var builder = backend.createScreen("cloudtweaks.title", CloudsConfiguration::save);

        builder.category(DataSettings.buildGlobalSettings(config));
        builder.category(DataSettings.createPresetsCategory(self));
        builder.category(DataSettings.buildLightingSettings(config));
        builder.category(DataSettings.buildLightSourcesSettings(config));
        builder.category(DataSettings.buildSkyColorSettings(config));
        builder.category(DataSettings.buildLayerSettings(config, self));

        self[0] = builder.build(parent);
        return self[0];
    }
}
