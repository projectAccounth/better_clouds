package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.Starter;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

public class YACLConfigScreen {
    private YACLConfigScreen() {}

    private static ConfigCategory buildPresetsSettings() {
        return YACLPresetWidgets.createPresetsCategory();
    }

    public static Screen createConfigScreen(Screen parent) {
        var config = CloudsConfiguration.getInstance();
        PresetController.initialize();
        LayerPresets.initialize();
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
            .category(YACLDataSettings.buildNetherLayersSettings(config))
            .category(YACLDataSettings.buildEndLayersSettings(config))
            
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory buildGlobalSettings(CloudsConfiguration config) {
        return ConfigCategory.createBuilder()
        
            .name(ComponentWrapper.translatable("cloudtweaks.category.global"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.global"))
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.clouds_mod_enabled"))
                .binding(config.CLOUDS_RENDERED, () -> config.CLOUDS_RENDERED, v -> config.CLOUDS_RENDERED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.cloud_distance"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.cloud_distance.tooltip")))
                .binding(config.getCloudDistanceChunks(), config::getCloudDistanceChunks, v -> config.CLOUD_DISTANCE_CHUNKS = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_CLOUD_DISTANCE_CHUNKS, ConfigConstants.MAX_CLOUD_DISTANCE_CHUNKS).step(ConfigConstants.CLOUD_DISTANCE_STEP))
                .build())

            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.rebuild_clouds"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.rebuild_clouds")))
                .action((yacl, btn) -> {
                    CloudsConfiguration.save();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.clouds_rebuilt"));
                    ClientHelper.setScreen(null);
                })
                .build())

            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.recompile_scripts"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.recompile_scripts")))
                .action((yacl, btn) -> {
                    CloudsConfiguration.save();
                    Starter.reloadScript();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.reloaded_scripts"));
                    ClientHelper.setScreen(null);
                })
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
                .controller(ColorControllerBuilder::create)
                .build())
            
            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.thunder_color"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.thunder_color")))
                .binding(
                    new Color(weather.thunderColor | 0xFF000000),
                    () -> new Color(weather.thunderColor | 0xFF000000),
                    v -> weather.thunderColor = v.getRGB() & 0xFFFFFF
                )
                .controller(ColorControllerBuilder::create)
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
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Save as Lighting Preset"))
                .description(OptionDescription.of(ComponentWrapper.literal("Save current lighting settings as a preset")))
                .action((yacl, btn) -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved from Lighting settings", config)) {
                        CloudsConfiguration.save();
                        RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("Saved lighting preset: " + presetName)
                        );
                    }
                })
                .build());
        
        return builder.build();
    }
    
    private static String formatTimestamp(long millis) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }
}
