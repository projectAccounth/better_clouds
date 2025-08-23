package net.not_thefirst.story_mode_clouds.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CloudsConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.cloud_tweaks.clouds_config"));

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.cloud_tweaks.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Boolean toggles
        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.is_enabled"),
                        CloudsConfiguration.INSTANCE.IS_ENABLED)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.IS_ENABLED = newValue)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.appears_shaded"),
                        CloudsConfiguration.INSTANCE.APPEARS_SHADED)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.APPEARS_SHADED = newValue)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.uses_custom_alpha"),
                        CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.USES_CUSTOM_ALPHA = newValue)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.cloud_tweaks.fullbright"),
                        CloudsConfiguration.INSTANCE.FULLBRIGHT)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.FULLBRIGHT = newValue)
                .build());

        // Float sliders
        general.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.fade_range"),
                        CloudsConfiguration.INSTANCE.FADE_RANGE)
                .setDefaultValue(6.0f)
                .setMin(0.0f).setMax(64.0f)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.FADE_RANGE = newValue)
                .build());

        general.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.cloud_y_scale"),
                        CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE)
                .setDefaultValue(1.5f)
                .setMin(0.1f).setMax(10.0f)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.CLOUD_Y_SCALE = newValue)
                .build());

        general.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.brightness"),
                        CloudsConfiguration.INSTANCE.BRIGHTNESS)
                .setDefaultValue(1.0f)
                .setMin(0.0f).setMax(2.0f)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.BRIGHTNESS = newValue)
                .build());

        general.addEntry(entryBuilder
                .startFloatField(Component.translatable("option.cloud_tweaks.base_alpha"),
                        CloudsConfiguration.INSTANCE.BASE_ALPHA)
                .setDefaultValue(0.8f)
                .setMin(0.0f).setMax(1.0f)
                .setSaveConsumer(newValue -> CloudsConfiguration.INSTANCE.BASE_ALPHA = newValue)
                .build());

        // Hook up saving
        builder.setSavingRunnable(CloudsConfiguration::save);

        return builder.build();
    }
}