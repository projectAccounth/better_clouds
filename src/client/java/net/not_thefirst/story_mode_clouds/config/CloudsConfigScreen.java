package net.not_thefirst.story_mode_clouds.config;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.api.SimpleConfigScreen;

public class CloudsConfigScreen extends SimpleConfigScreen {
    public CloudsConfigScreen(Screen parent) {
        super(parent, "cloud_tweaks.cloud_config");
    }

    @Override
    protected void init() {
        super.init();

        var cfg = CloudsConfiguration.INSTANCE;

        addCategory("cloud_tweaks.main_settings_category", HorizontalAlignment.CENTER);
        
        addToggle("cloud_tweaks.is_enabled", cfg.IS_ENABLED, (v) -> cfg.IS_ENABLED = v);
        addToggle("cloud_tweaks.is_shaded", cfg.APPEARS_SHADED, (v) -> cfg.APPEARS_SHADED = v);
        addToggle("cloud_tweaks.custom_alpha", cfg.USES_CUSTOM_ALPHA, (v) -> cfg.USES_CUSTOM_ALPHA = v);
        addToggle("cloud_tweaks.custom_brightness", cfg.CUSTOM_BRIGHTNESS, (v) -> cfg.CUSTOM_BRIGHTNESS = v);
        addToggle("cloud_tweaks.custom_color", cfg.USES_CUSTOM_COLOR, (v) -> cfg.USES_CUSTOM_COLOR = v);
        addToggle("cloud_tweaks.debug_v0", cfg.DEBUG_v0, (v) -> cfg.DEBUG_v0 = v);
        addToggle("cloud_tweaks.randomized_y", cfg.RANDOMIZED_Y, (v) -> cfg.RANDOMIZED_Y = v);

        addCategory("cloud_tweaks.cloud_shape_settings", HorizontalAlignment.CENTER);

        addSliderWithBox("cloud_tweaks.fade_alpha", cfg.FADE_ALPHA, 0.0, 1.0, 0.01, (v) -> cfg.FADE_ALPHA = v.floatValue());
        addSliderWithBox("cloud_tweaks.y_scale", cfg.CLOUD_Y_SCALE, 0.5, 10.0, 0.1, (v) -> cfg.CLOUD_Y_SCALE = v.floatValue());

        addCategory("cloud_tweaks.cloud_color_settings", HorizontalAlignment.CENTER);

        addSliderWithBox("cloud_tweaks.base_alpha", cfg.BASE_ALPHA, 0.0, 1.0, 0.05, (v) -> cfg.BASE_ALPHA = v.floatValue());
        addSliderWithBox("cloud_tweaks.brightness", cfg.BRIGHTNESS, 0.0, 1.0, 0.01, (v) -> cfg.BRIGHTNESS = v.floatValue());
        addColorPicker("cloud_tweaks.bg_color", cfg.CLOUD_COLOR, color -> {
            cfg.CLOUD_COLOR = color;
        });

        onCloseSave(CloudsConfiguration::save);
    }
}