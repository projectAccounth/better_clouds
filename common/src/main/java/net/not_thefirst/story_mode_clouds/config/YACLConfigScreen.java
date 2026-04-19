package net.not_thefirst.story_mode_clouds.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;

import java.awt.Color;
/**
 * Config migration
 */
public class YACLConfigScreen {

    private static ConfigCategory buildPresetsSettings() {
        return YACLPresetWidgets.createPresetsCategory();
    }

    public static Screen createConfigScreen(Screen parent) {
        var config = CloudsConfiguration.getInstance();
        ConfigPresets.initialize();
        YACLPresetWidgets.setParentScreen(parent);
        
        return YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.title"))
            .save(CloudsConfiguration::save)
            
            .category(buildGlobalSettings(config))
            .category(buildPresetsSettings())
            .category(buildLightingSettings(config))
            .category(YACLDataSettings.buildSkyColorSettings(config))
            .category(YACLDataSettings.buildLightSourcesSettings(config))
            .category(YACLDataSettings.buildLayersSettings(config))
            
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory buildGlobalSettings(CloudsConfiguration config) {
        return ConfigCategory.createBuilder()
        
            .name(ComponentWrapper.translatable("cloudtweaks.category.global"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.global"))
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.clouds_rendered"))
                .binding(config.CLOUDS_RENDERED, () -> config.CLOUDS_RENDERED, v -> config.CLOUDS_RENDERED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.grid_size"))
                .binding(config.CLOUD_GRID_SIZE, () -> config.CLOUD_GRID_SIZE, v -> config.CLOUD_GRID_SIZE = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_GRID_SIZE, ConfigConstants.MAX_GRID_SIZE).step(ConfigConstants.GRID_SIZE_STEP))
                .build())
            
            .build();
    }

    private static ConfigCategory buildLightingSettings(CloudsConfiguration config) {
        LightingParameters lighting = config.LIGHTING;
        WeatherColorConfig weather = config.getWeather();
        
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.lighting"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.lighting"))
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.ambient_strength"))
                .binding(lighting.AMBIENT_LIGHTING_STRENGTH, () -> lighting.AMBIENT_LIGHTING_STRENGTH, v -> lighting.AMBIENT_LIGHTING_STRENGTH = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS).step(ConfigConstants.BRIGHTNESS_STEP))
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.max_shading"))
                .binding(lighting.MAX_LIGHTING_SHADING, () -> lighting.MAX_LIGHTING_SHADING, v -> lighting.MAX_LIGHTING_SHADING = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS).step(ConfigConstants.BRIGHTNESS_STEP))
                .build())
            
            .option(Option.<CloudColorProviderMode>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.cloud_provider"))
                .binding(config.COLOR_MODE, () -> config.COLOR_MODE, v -> config.COLOR_MODE = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(CloudColorProviderMode.class))
                .build())
            
            .option(Option.<LightingType>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.lighting_type"))
                .binding(lighting.LIGHTING_TYPE, () -> lighting.LIGHTING_TYPE, v -> lighting.LIGHTING_TYPE = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(LightingType.class))
                .build())
            
            .option(Option.<ShadingMode>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.shading_mode"))
                .binding(lighting.SHADING_MODE, () -> lighting.SHADING_MODE, v -> lighting.SHADING_MODE = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ShadingMode.class))
                .build());
        
        builder
            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.rain_color"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.rain_color")))
                .binding(
                    new Color(weather.rainColor | 0xFF000000),
                    () -> new Color(weather.rainColor | 0xFF000000),
                    v -> weather.rainColor = v.getRGB() & 0xFFFFFF
                )
                .controller(opt -> ColorControllerBuilder.create(opt))
                .build())
            
            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.thunder_color"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.thunder_color")))
                .binding(
                    new Color(weather.thunderColor | 0xFF000000),
                    () -> new Color(weather.thunderColor | 0xFF000000),
                    v -> weather.thunderColor = v.getRGB() & 0xFFFFFF
                )
                .controller(opt -> ColorControllerBuilder.create(opt))
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.rain_strength"))
                .binding(weather.rainStrength, () -> weather.rainStrength, v -> weather.rainStrength = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.thunder_strength"))
                .binding(weather.thunderStrength, () -> weather.thunderStrength, v -> weather.thunderStrength = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                .build());
        
        return builder.build();
    }
}
