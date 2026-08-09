package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
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

        var backend = BackendHolder.getBackend();
        var builder = backend.createScreen("cloudtweaks.title", CloudsConfiguration::save);

        builder.category(buildGlobalSettingsBackend(config));
        builder.category(wrapCategory(buildPresetsSettings()));
        builder.category(buildLightingSettingsBackend(config));
        builder.category(wrapCategory(buildLayerSettings(config, parent)));
        builder.category(wrapCategory(YACLDataSettings.buildSkyColorSettings(config)));
        builder.category(wrapCategory(YACLDataSettings.buildLightSourcesSettings(config)));

        return builder.build(parent);
    }

    private static ConfigBackend.ConfigCategoryBuilder buildGlobalSettingsBackend(CloudsConfiguration config) {
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
            ));
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLightingSettingsBackend(CloudsConfiguration config) {
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
            .option(backend.floatOption(
                "cloudtweaks.option.max_shading",
                null,
                () -> lighting.MAX_LIGHTING_SHADING,
                v -> lighting.MAX_LIGHTING_SHADING = v
            ).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS)
                .step(ConfigConstants.BRIGHTNESS_STEP)
                .slider())
            .option(backend.enumOption(
                "cloudtweaks.option.cloud_provider",
                null,
                () -> config.COLOR_MODE,
                v -> config.COLOR_MODE = v
            ).enumClass(CloudColorProviderMode.class))
            .action(backend.createAction(
                "cloudtweaks.option.rebuild_clouds",
                "cloudtweaks.desc.rebuild_clouds",
                () -> {
                    CloudsConfiguration.save();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.clouds_rebuilt"));
                    ClientHelper.setScreen(null);
                }
            ));
    }

    private static ConfigBackend.ConfigCategoryBuilder wrapCategory(ConfigCategory category) {
        return new ConfigBackend.ConfigCategoryBuilder() {
            @Override
            public ConfigBackend.ConfigCategoryBuilder name(String name) {
                return this;
            }

            @Override
            public ConfigBackend.ConfigCategoryBuilder tooltip(String tooltip) {
                return this;
            }

            @Override
            public ConfigBackend.ConfigCategoryBuilder option(ConfigBackend.ConfigOptionBuilder<?> option) {
                return this;
            }

            @Override
            public ConfigBackend.ConfigCategoryBuilder group(ConfigBackend.ConfigGroupBuilder group) {
                return this;
            }

            @Override
            public ConfigBackend.ConfigCategoryBuilder action(ConfigBackend.ConfigActionBuilder action) {
                return this;
            }

            @Override
            public ConfigBackend.ConfigCategorySpec build() {
                return new ConfigBackend.ConfigCategorySpec("wrapped", null, category);
            }
        };
    }

    private static ConfigCategory buildLayerSettings(CloudsConfiguration config, Screen parent) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.layers"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.layers"));

        for (CloudsConfiguration.Dimension dimension : CloudsConfiguration.Dimension.values()) {
            builder.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal(dimension.getId()))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc." + dimension.getId() + "_layers")))
                .action((yacl, btn) -> {
                    ClientHelper.setScreen(YACLDataSettings.createLayerSettingsScreenForDimension(config, dimension, parent));
                })
                .build());
        }

        return builder.build();
    }
    
    private static String formatTimestamp(long millis) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }
}
