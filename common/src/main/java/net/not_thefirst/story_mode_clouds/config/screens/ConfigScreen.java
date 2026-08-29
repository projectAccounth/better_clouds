package net.not_thefirst.story_mode_clouds.config.screens;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.Starter;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

public class ConfigScreen {
    private ConfigScreen() {}

    public static Screen createConfigScreen(Screen parent) {
        var config = CloudsConfiguration.getInstance();
        PresetController.initialize();
        LayerPresets.initialize();
        PresetWidgets.setParentScreen(parent);

        var backend = BackendHolder.getBackend();
        var builder = backend.createScreen("cloudtweaks.title", CloudsConfiguration::save);

        builder.category(buildGlobalSettings(config));
        builder.category(PresetWidgets.createPresetsCategory());
        builder.category(buildLightingSettings(config));
        builder.category(buildLayerSettings(config, parent));
        builder.category(DataSettings.buildSkyColorSettings(config));
        builder.category(DataSettings.buildLightSourcesSettings(config));

        return builder.build(parent);
    }

    private static ConfigBackend.ConfigCategoryBuilder buildGlobalSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();
        return backend.createCategory("cloudtweaks.category.global", "cloudtweaks.desc.global")
            .option(backend.booleanOption(
                "cloudtweaks.option.clouds_mod_enabled",
                null,
                () -> config.CLOUDS_RENDERED,
                v -> config.CLOUDS_RENDERED = v
            ))
            .option(backend.integerOption(
                "cloudtweaks.option.cloud_distance",
                "cloudtweaks.option.cloud_distance.tooltip",
                config::getCloudDistanceChunks,
                v -> config.CLOUD_DISTANCE_CHUNKS = v
            ).range(ConfigConstants.MIN_CLOUD_DISTANCE_CHUNKS, ConfigConstants.MAX_CLOUD_DISTANCE_CHUNKS)
                .step(ConfigConstants.CLOUD_DISTANCE_STEP)
                .slider())
            .action(backend.createAction(
                "cloudtweaks.option.rebuild_clouds",
                "cloudtweaks.desc.rebuild_clouds",
                () -> {
                    CloudsConfiguration.save();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.clouds_rebuilt"));
                    ClientHelper.setScreen(null);
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.option.recompile_scripts",
                "cloudtweaks.desc.recompile_scripts",
                () -> {
                    CloudsConfiguration.save();
                    Starter.reloadScript();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.reloaded_scripts"));
                    ClientHelper.setScreen(null);
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.option.refresh_config", 
                "cloudtweaks.desc.refresh_config", 
                () -> {
                    CloudsConfiguration.save();
                    CloudsConfiguration.refreshConfig();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.refreshed_config"));
                    ClientHelper.setScreen(null);
                }));
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLightingSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();
        LightingParameters lighting = config.LIGHTING;

        return backend.createCategory("cloudtweaks.category.lighting", "cloudtweaks.desc.lighting")
            .option(backend.floatOption(
                "cloudtweaks.option.ambient_strength",
                null,
                () -> lighting.AMBIENT_LIGHTING_STRENGTH,
                v -> lighting.AMBIENT_LIGHTING_STRENGTH = v
            ).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS)
                .step(ConfigConstants.BRIGHTNESS_STEP)
                .slider())
            // .option(backend.floatOption(
            //     "cloudtweaks.option.max_shading",
            //     null,
            //     () -> lighting.MAX_LIGHTING_SHADING,
            //     v -> lighting.MAX_LIGHTING_SHADING = v
            // ).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS)
            //     .step(ConfigConstants.BRIGHTNESS_STEP)
            //     .slider())
            .option(backend.enumOption(
                "cloudtweaks.option.cloud_provider",
                null,
                () -> config.COLOR_MODE,
                v -> config.COLOR_MODE = v
            ).enumClass(CloudColorProviderMode.class))
            .option(backend.enumOption(
                "cloudtweaks.option.lighting_type",
                null,
                () -> lighting.LIGHTING_TYPE,
                v -> lighting.LIGHTING_TYPE = v
            ).enumClass(LightingType.class))
            .option(backend.enumOption(
                "cloudtweaks.option.shading_mode",
                null,
                () -> lighting.SHADING_MODE,
                v -> lighting.SHADING_MODE = v
            ).enumClass(ShadingMode.class))
            
            .option(backend.colorOption(
                "cloudtweaks.option.rain_color",
                null,
                () -> new java.awt.Color(config.WEATHER_COLOR.rainColor | 0xFF000000),
                v -> config.WEATHER_COLOR.rainColor = v.getRGB() & 0xFFFFFF
            ))
            .option(backend.colorOption(
                "cloudtweaks.option.thunder_color",
                null,
                () -> new java.awt.Color(config.WEATHER_COLOR.thunderColor | 0xFF000000),
                v -> config.WEATHER_COLOR.thunderColor = v.getRGB() & 0xFFFFFF
            ))
            .option(backend.floatOption(
                "cloudtweaks.option.rain_strength",
                null,
                () -> config.WEATHER_COLOR.rainStrength,
                v -> config.WEATHER_COLOR.rainStrength = v
            ).range(0.0f, 1.0f)
                .step(0.01f))
            .option(backend.floatOption(
                "cloudtweaks.option.thunder_strength",
                null,
                () -> config.WEATHER_COLOR.thunderStrength,
                v -> config.WEATHER_COLOR.thunderStrength = v
            ).range(0.0f, 1.0f)
                .step(0.01f))
            .action(backend.createAction(
                "cloudtweaks.option.save_lighting_preset",
                "cloudtweaks.desc.save_lighting_preset",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved from Lighting settings", config)) {
                        CloudsConfiguration.save();
                        RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("Saved lighting preset: " + presetName)
                        );
                    }
                }
            ));
    }

    private static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date(timestamp));
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerSettings(CloudsConfiguration config, Screen parent) {
        var backend = BackendHolder.getBackend();

        var builder = backend.createCategory("cloudtweaks.category.layers", "cloudtweaks.desc.layers");

        for (CloudsConfiguration.Dimension dimension : CloudsConfiguration.Dimension.values()) {
            builder.action(backend.createAction(
                ComponentWrapper.translatable("cloudtweaks.raw.edit").getString() + " " + dimension.getId(),
                ComponentWrapper.translatable("cloudtweaks.desc.edit_dimension").getString() + " " + dimension.getId(),
                () -> {
                    // current screen is YACL's screen or whatever config screen we're using
                    ClientHelper.setScreen(DataSettings.createLayerSettingsScreenForDimension(config, dimension, ClientHelper.getCurrentScreen()));
                }
            ));
        }

        return builder;
    }
}
