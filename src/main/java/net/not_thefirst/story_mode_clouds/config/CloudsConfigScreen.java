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

        CloudsConfiguration cfg = CloudsConfiguration.INSTANCE;

        // === General ===
        addCategory("option.cloud_tweaks.category.general", HorizontalAlignment.CENTER);

        addToggle("option.cloud_tweaks.is_enabled", cfg.IS_ENABLED, (v) -> cfg.IS_ENABLED = v);
        addToggle("option.cloud_tweaks.debug_v0", cfg.DEBUG_v0, (v) -> cfg.DEBUG_v0 = v);

        // === Appearance ===
        addCategory("option.cloud_tweaks.category.appearance", HorizontalAlignment.CENTER);

        addToggle("option.cloud_tweaks.is_shaded", cfg.APPEARS_SHADED, (v) -> cfg.APPEARS_SHADED = v);
        addToggle("option.cloud_tweaks.fog_enabled", cfg.FOG_ENABLED, (v) -> cfg.FOG_ENABLED = v);
        addToggle("option.cloud_tweaks.fade_enabled", cfg.FADE_ENABLED, (v) -> cfg.FADE_ENABLED = v);
        addSliderWithBox("option.cloud_tweaks.fade_alpha", cfg.FADE_ALPHA, 0.0, 1.0, 0.01, (v) -> cfg.FADE_ALPHA = v.floatValue());

        addToggle("option.cloud_tweaks.custom_alpha", cfg.USES_CUSTOM_ALPHA, (v) -> cfg.USES_CUSTOM_ALPHA = v);
        addSliderWithBox("option.cloud_tweaks.base_alpha", cfg.BASE_ALPHA, 0.0, 1.0, 0.05, (v) -> cfg.BASE_ALPHA = v.floatValue());

        addToggle("option.cloud_tweaks.custom_brightness", cfg.CUSTOM_BRIGHTNESS, (v) -> cfg.CUSTOM_BRIGHTNESS = v);
        addSliderWithBox("option.cloud_tweaks.brightness", cfg.BRIGHTNESS, 0.0, 1.0, 0.01, (v) -> cfg.BRIGHTNESS = v.floatValue());

        addToggle("option.cloud_tweaks.custom_color", cfg.USES_CUSTOM_COLOR, (v) -> cfg.USES_CUSTOM_COLOR = v);

        // === Shape ===
        addCategory("option.cloud_tweaks.category.shape", HorizontalAlignment.CENTER);

        addSliderWithBox("option.cloud_tweaks.y_scale", cfg.CLOUD_Y_SCALE, 0.5, 10.0, 0.1, (v) -> cfg.CLOUD_Y_SCALE = v.floatValue());
        addToggle("option.cloud_tweaks.randomized_y", cfg.RANDOMIZED_Y, (v) -> cfg.RANDOMIZED_Y = v);

        // === Layers ===
        addCategory("option.cloud_tweaks.category.layers", HorizontalAlignment.CENTER);

        addSliderWithBox("option.cloud_tweaks.cloud_layers", cfg.CLOUD_LAYERS, 1, 10, 1, (v) -> cfg.CLOUD_LAYERS = v.intValue());
        addSliderWithBox("option.cloud_tweaks.cloud_layers_spacing", cfg.CLOUD_LAYERS_SPACING, 1, 512, 0.1, (v) -> cfg.CLOUD_LAYERS_SPACING = v.floatValue());
        for (int i = 0; i < CloudsConfiguration.MAX_LAYER_COUNT; i++) {
            final int idx = i;
            addColorPicker("option.cloud_tweaks.layer_color." + (i + 1),
                    cfg.CLOUD_COLORS[idx],
                    (v) -> cfg.CLOUD_COLORS[idx] = v);
        }

        onCloseSave(CloudsConfiguration::save);
    }
}