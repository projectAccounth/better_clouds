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
                        CloudsConfiguration.INSTANCE.IS_ENABLED)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.IS_ENABLED = v)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.debug_v0"),
                        CloudsConfiguration.INSTANCE.DEBUG_v0)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.DEBUG_v0 = v)
                .build());

        // === Appearance category ===
        ConfigCategory appearance = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.appearance"));

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.is_shaded"),
                        CloudsConfiguration.INSTANCE.APPEARS_SHADED)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.APPEARS_SHADED = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.fade_enabled"),
                        CloudsConfiguration.INSTANCE.FADE_ENABLED)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.FADE_ENABLED = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.fade_alpha"),
                        CloudsConfiguration.INSTANCE.FADE_ALPHA)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(0.5f)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.FADE_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_alpha"),
                        CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.base_alpha"),
                        CloudsConfiguration.INSTANCE.BASE_ALPHA)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(0.8f)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.BASE_ALPHA = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_brightness"),
                        CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS)
                .setDefaultValue(true)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.CUSTOM_BRIGHTNESS = v)
                .build());

        appearance.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.brightness"),
                        CloudsConfiguration.INSTANCE.BRIGHTNESS)
                .setMin(0.0f).setMax(1.0f)
                .setDefaultValue(1.0f)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.BRIGHTNESS = v)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.custom_color"),
                        CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.USES_CUSTOM_COLOR = v)
                .build());

        appearance.addEntry(entryBuilder
                .startColorField(Component.translatable("option.cloud_tweaks.color"),
                        CloudsConfiguration.INSTANCE.CLOUD_COLOR)
                .setDefaultValue(0xFFFFFF)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.CLOUD_COLOR = v)
                .build());

        // === Shape category ===
        ConfigCategory shape = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.shape"));

        shape.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.y_scale"),
                        CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE)
                .setMin(0.5f).setMax(10.0f)
                .setDefaultValue(1.5f)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE = v)
                .build());

        shape.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.randomized_y"),
                        CloudsConfiguration.INSTANCE.RANDOMIZED_Y)
                .setDefaultValue(false)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.RANDOMIZED_Y = v)
                .build());

        // === Layers category ===
        ConfigCategory layers = builder.getOrCreateCategory(Component.translatable("option.cloud_tweaks.category.layers"));

        layers.addEntry(entryBuilder
                .startIntField(Component.translatable("option.cloud_tweaks.cloud_layers"),
                        CloudsConfiguration.INSTANCE.CLOUD_LAYERS)
                .setMin(1).setMax(10)
                .setDefaultValue(1)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.CLOUD_LAYERS = v)
                .build());

        layers.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.cloud_layers_spacing"),
                        CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING)
                .setMin(1.0f).setMax(20.0f)
                .setDefaultValue(2.0f)
                .setSaveConsumer(v -> CloudsConfiguration.INSTANCE.CLOUD_LAYERS_SPACING = v)
                .build());

        // Hook up saving
        builder.setSavingRunnable(CloudsConfiguration::save);

        return builder.build();
    }
}
