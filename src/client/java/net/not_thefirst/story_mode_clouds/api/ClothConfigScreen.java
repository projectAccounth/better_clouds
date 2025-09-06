package net.not_thefirst.story_mode_clouds.api;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class ClothConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.cloud_tweaks.clouds_config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // === General category ===
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.general"));

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.is_enabled"),
                        CloudsConfiguration.get().IS_ENABLED)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.get().IS_ENABLED = v)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.clouds_rendered"),
                        CloudsConfiguration.get().CLOUDS_RENDERED)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.get().CLOUDS_RENDERED = v)
                .build());


        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.debug_v0"),
                        CloudsConfiguration.get().DEBUG_v0)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().DEBUG_v0 = v)
                .build());

        // === Appearance category ===
        ConfigCategory appearance = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.appearance"));

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.is_shaded"),
                        CloudsConfiguration.get().APPEARS_SHADED)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().APPEARS_SHADED = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.fog_enabled"),
                        CloudsConfiguration.get().APPEARS_SHADED)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().FOG_ENABLED = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.fade_enabled"),
                        CloudsConfiguration.get().FADE_ENABLED)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().FADE_ENABLED = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.fade_alpha"),
                        CloudsConfiguration.get().FADE_ALPHA)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(0.5f)
                .setSaveConsumer(v -> CloudsConfiguration.get().FADE_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_alpha"),
                        CloudsConfiguration.get().USES_CUSTOM_ALPHA)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.get().USES_CUSTOM_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.base_alpha"),
                        CloudsConfiguration.get().BASE_ALPHA)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(0.8f)
                .setSaveConsumer(v -> CloudsConfiguration.get().BASE_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_brightness"),
                        CloudsConfiguration.get().CUSTOM_BRIGHTNESS)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.get().CUSTOM_BRIGHTNESS = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.brightness"),
                        CloudsConfiguration.get().BRIGHTNESS)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(1.0f)
                .setSaveConsumer(v -> CloudsConfiguration.get().BRIGHTNESS = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_color"),
                        CloudsConfiguration.get().USES_CUSTOM_COLOR)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().USES_CUSTOM_COLOR = v)
                .build());

        // === Shape category ===
        ConfigCategory shape = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.shape"));

        shape.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.y_scale"),
                        CloudsConfiguration.get().CLOUD_Y_SCALE)
                .setMin(0.5f).setMax(10.0f)
                .setDefaultValue(1.5f)
                .setSaveConsumer(v -> CloudsConfiguration.get().CLOUD_Y_SCALE = v)
                .build());

        shape.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.randomized_y"),
                        CloudsConfiguration.get().RANDOMIZED_Y)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.get().RANDOMIZED_Y = v)
                .build());

        // === Layers category ===
        ConfigCategory layers = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.layers"));

        layers.addEntry(entryBuilder
                .startIntField(Component.translatable("option.cloud_tweaks.cloud_layers"),
                        CloudsConfiguration.get().CLOUD_LAYERS)
                .setMin(1).setMax(10)
                .setDefaultValue(1)
                .setSaveConsumer(v -> CloudsConfiguration.get().CLOUD_LAYERS = v)
                .build());

        layers.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.cloud_layers_spacing"),
                        CloudsConfiguration.get().CLOUD_LAYERS_SPACING)
                .setMin(1.0f).setMax(512.0f)
                .setDefaultValue(2.0f)
                .setSaveConsumer(v -> CloudsConfiguration.get().CLOUD_LAYERS_SPACING = v)
                .build());

        for (int i = 0; i < CloudsConfiguration.MAX_LAYER_COUNT; i++) {
            final int idx = i;
            layers.addEntry(entryBuilder
                .startColorField(
                        Component.translatable("option.cloud_tweaks.layer_color." + (i + 1)),
                        CloudsConfiguration.get().CLOUD_COLORS[idx])
                .setDefaultValue(0xFFFFFF)
                .setSaveConsumer(v -> CloudsConfiguration.get().CLOUD_COLORS[idx] = v)
                .build());
        }

        // Hook up saving
        builder.setSavingRunnable(CloudsConfiguration::save);

        return builder.build();
    }
}
