package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.not_thefirst.story_mode_clouds.config.presets.controllers.PresetController;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PresetEditorScreenManager extends ScreenManager {
    private static final Map<String, PresetEditorScreenManager> instances = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private final PresetController.PresetCategory category;
    private final String presetId;

    private PresetEditorScreenManager(PresetController.PresetCategory category, String presetId) {
        super();
        this.category = category;
        this.presetId = presetId;
    }

    private static String getKey(PresetController.PresetCategory category, String presetId) {
        return category.name() + "_" + presetId;
    }

    public static PresetEditorScreenManager getInstance(PresetController.PresetCategory category, String presetId) {
        return instances.computeIfAbsent(getKey(category, presetId), k -> new PresetEditorScreenManager(category, presetId));
    }

    public static void clearInstance(PresetController.PresetCategory category, String presetId) {
        instances.remove(getKey(category, presetId));
    }

    public static void clearAll() {
        instances.clear();
    }

    @Override
    public Screen buildScreen() {
        return buildEditorScreen();
    }

    public Screen buildEditorScreen() {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen(formatCategoryName(category), CloudsConfiguration::save);
        var categoryBuilder = backend.createCategory("cloudtweaks.presets.data", null);

        switch (category) {
            case COLORS:
                CloudColorPreset colorPreset = PresetController.getColorPreset(presetId);
                if (colorPreset != null) {
                    addColorPresetDataGroup(categoryBuilder, colorPreset);
                }
                break;
            case LIGHTING:
                LightingPreset lightingPreset = PresetController.getLightingPreset(presetId);
                if (lightingPreset != null) {
                    addLightingPresetDataGroup(categoryBuilder, lightingPreset);
                }
                break;
            case LIGHT_SOURCES:
                LightSourcesPreset lightSourcesPreset = PresetController.getLightSourcesPreset(presetId);
                if (lightSourcesPreset != null) {
                    addLightSourcesPresetDataGroup(categoryBuilder, lightSourcesPreset);
                }
                break;
        }

        categoryBuilder.action(backend.createAction(
            "cloudtweaks.presets.back",
            "cloudtweaks.presets.back.tooltip",
            () -> ClientHelper.setScreen(parentScreen)
        ));

        screenBuilder.category(categoryBuilder);
        currentScreen = screenBuilder.build(parentScreen);
        return currentScreen;
    }

    public Screen buildMetadataEditScreen() {
        switch (category) {
            case COLORS -> {
                CloudColorPreset preset = PresetController.getColorPreset(presetId);
                if (preset != null) {
                    metadataName = preset.displayName;
                    metadataDescription = preset.description;
                }
            }
            case LIGHTING -> {
                LightingPreset preset = PresetController.getLightingPreset(presetId);
                if (preset != null) {
                    metadataName = preset.displayName;
                    metadataDescription = preset.description;
                }
            }
            case LIGHT_SOURCES -> {
                LightSourcesPreset preset = PresetController.getLightSourcesPreset(presetId);
                if (preset != null) {
                    metadataName = preset.displayName;
                    metadataDescription = preset.description;
                }
            }
        }

        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen(formatCategoryName(category), () -> {
            PresetController.updatePresetMetadata(category, presetId, metadataName, metadataDescription);
            CloudsConfiguration.save();
        });
        var infoCategory = backend.createCategory("cloudtweaks.presets.information", null)
            .option(backend.stringOption(
                "cloudtweaks.presets.name", null,
                () -> metadataName,
                v -> metadataName = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc", null,
                () -> metadataDescription,
                v -> metadataDescription = v
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.back",
                "cloudtweaks.presets.back.tooltip",
                () -> ClientHelper.setScreen(parentScreen)
            ));

        screenBuilder.category(infoCategory);
        currentScreen = screenBuilder.build(parentScreen);
        return currentScreen;
    }
    private String metadataName = "";
    private String metadataDescription = "";

    public Screen buildEditingScreen() {
        var backend = BackendHolder.getBackend();

        switch (category) {
            case COLORS: {
                CloudColorPreset preset = PresetController.getColorPreset(presetId);
                if (preset == null) return null;

                var screenBuilder = backend.createScreen("cloudtweaks.presets.edit_color_prefix", () -> {
                    PresetController.saveColorPresetDirect(preset);
                    CloudsConfiguration.save();
                });

                var dataCategory = backend.createCategory("cloudtweaks.presets.data", null);
                var infoGroup = backend.createGroup("cloudtweaks.presets.information");

                infoGroup
                    .option(backend.stringOption(
                        "cloudtweaks.presets.name",
                        null,
                        () -> preset.displayName,
                        v -> preset.displayName = v
                    ))
                    .option(backend.stringOption(
                        "cloudtweaks.option.preset_desc",
                        null,
                        () -> preset.description,
                        v -> preset.description = v
                    ));

                dataCategory.group(infoGroup);

                var colorGroup = backend.createGroup("cloudtweaks.presets.color_presets");

                if (preset.colors == null) preset.colors = new ArrayList<>();
                
                colorGroup
                    .action(backend.createAction(
                        "cloudtweaks.presets.add_keypoint",
                        "cloudtweaks.presets.add_keypoint.tooltip",
                        () -> {
                            CloudsConfiguration.SkyColorKeypoint newKp = new CloudsConfiguration.SkyColorKeypoint();
                            newKp.time = preset.colors.isEmpty() ? 0 : preset.colors.get(preset.colors.size() - 1).time + 1000;
                            newKp.color = 0xFFFFFF;
                            preset.colors.add(newKp);
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.added_color_keypoint"));
                            ClientHelper.setScreen(buildEditingScreen());
                        }
                    ));
                    
                for (int i = 0; i < preset.colors.size(); i++) {
                    CloudsConfiguration.SkyColorKeypoint kp = preset.colors.get(i);

                    var group = backend.createGroup("cloudtweaks.raw.keypoint")
                        .option(backend.integerOption(
                            "cloudtweaks.raw.time",
                            null,
                            () -> kp.time,
                            v -> kp.time = v
                        ).range(0, 24000))
                        .option(backend.colorOption(
                            "cloudtweaks.raw.color",
                            null,
                            () -> new Color(kp.color),
                            v -> kp.color = v.getRGB()
                        ))
                        .action(backend.createAction(
                            "cloudtweaks.raw.remove",
                            "cloudtweaks.raw.remove.tooltip",
                            () -> {
                                if (preset.colors.size() > 0) {
                                    ClientHelper.setScreen(ScreenManager.buildDeleteConfirmationScreen(
                                        ComponentWrapper.translatable("cloudtweaks.raw.keypoint").getString(),
                                        currentScreen,
                                        () -> {
                                            preset.colors.remove(kp);
                                            CloudsConfiguration.save();
                                            ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_keypoint"));
                                            ClientHelper.setScreen(buildEditingScreen());
                                        }
                                    ));
                                }
                            }
                        ));
                    
                    dataCategory.group(group);
                }

                dataCategory.group(colorGroup);

                screenBuilder.category(dataCategory);
                currentScreen = screenBuilder.build(parentScreen);
                return currentScreen;
            }
            case LIGHTING: {
                LightingPreset preset = PresetController.getLightingPreset(presetId);
                if (preset == null) return null;

                var screenBuilder = backend.createScreen("cloudtweaks.presets.edit_lighting_prefix", () -> {
                    PresetController.saveLightingPresetDirect(preset);
                    CloudsConfiguration.save();
                });

                var dataCategory = backend.createCategory("cloudtweaks.presets.lighting_data", null);

                dataCategory
                    .option(backend.stringOption(
                        "cloudtweaks.presets.name",
                        null,
                        () -> preset.displayName,
                        v -> preset.displayName = v
                    ))
                    .option(backend.stringOption(
                        "cloudtweaks.option.preset_desc",
                        null,
                        () -> preset.description,
                        v -> preset.description = v
                    ))
                    .option(backend.floatOption(
                        "cloudtweaks.option.ambient_strength",
                        null,
                        () -> preset.ambientStrength,
                        v -> preset.ambientStrength = v
                    ).range(0.0f, 1.0f).slider()
                        .step(0.05f))
                    .option(backend.enumOption(
                        "cloudtweaks.option.shading_mode",
                        null,
                        () -> preset.shadingMode,
                        v -> preset.shadingMode = v
                    ).enumClass(CloudsConfiguration.ShadingMode.class))
                    .option(backend.enumOption(
                        "cloudtweaks.option.lighting_type",
                        null,
                        () -> preset.lightingType,
                        v -> preset.lightingType = v
                    ).enumClass(CloudsConfiguration.LightingType.class))
                    .option(backend.integerOption(
                        "cloudtweaks.presets.day_start",
                        null,
                        () -> preset.dayStart,
                        v -> preset.dayStart = v
                    ).range(0, 24000))
                    .option(backend.integerOption(
                        "cloudtweaks.presets.day_noon",
                        null,
                        () -> preset.dayNoon,
                        v -> preset.dayNoon = v
                    ).range(0, 24000))
                    .option(backend.integerOption(
                        "cloudtweaks.presets.day_end",
                        null,
                        () -> preset.dayEnd,
                        v -> preset.dayEnd = v
                    ).range(0, 24000));

                screenBuilder.category(dataCategory);
                currentScreen = screenBuilder.build(parentScreen);
                return currentScreen;
            }
            case LIGHT_SOURCES: {
                LightSourcesPreset preset = PresetController.getLightSourcesPreset(presetId);
                if (preset == null) return null;

                var screenBuilder = backend.createScreen("cloudtweaks.presets.edit_light_sources_prefix", () -> {
                    PresetController.saveLightSourcesPresetDirect(preset);
                    CloudsConfiguration.save();
                });

                var dataCategory = backend.createCategory("cloudtweaks.presets.light_sources", null);

                if (preset.lights == null) preset.lights = new ArrayList<>();

                dataCategory
                    .action(backend.createAction(
                        "cloudtweaks.presets.add_light", 
                        "cloudtweaks.presets.add_light.tooltip", 
                        () -> {
                            preset.lights.add(new DiffuseLight());
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.added_light"));
                            ClientHelper.setScreen(buildEditingScreen());
                        }
                    ));

                for (int i = 0; i < preset.lights.size(); i++) {
                    DiffuseLight light = preset.lights.get(i);

                    var group = backend.createGroup("cloudtweaks.entry.light");

                    group
                        .option(backend.floatOption(
                            "cloudtweaks.option.direction_x",
                            null,
                            () -> light.direction().x(),
                            v -> light.setDirection(v, light.direction().y(), light.direction().z())
                        ).range(-1.0f, 1.0f).slider()
                            .step(0.01f))
                        .option(backend.floatOption(
                            "cloudtweaks.option.direction_y",
                            null,
                            () -> light.direction().y(),
                            v -> light.setDirection(light.direction().x(), v, light.direction().z())
                        ).range(-1.0f, 1.0f).slider()
                            .step(0.01f))
                        .option(backend.floatOption(
                            "cloudtweaks.option.direction_z",
                            null,
                            () -> light.direction().z(),
                            v -> light.setDirection(light.direction().x(), light.direction().y(), v)
                        ).range(-1.0f, 1.0f).slider()
                            .step(0.01f))
                        .option(backend.floatOption(
                            "cloudtweaks.option.intensity",
                            null,
                            () -> light.intensity(),
                            v -> light.setIntensity(v)
                        ).range(0.0f, 1.0f).slider()
                            .step(0.01f))
                        .action(backend.createAction(
                            "cloudtweaks.raw.remove",
                            "cloudtweaks.raw.remove.tooltip",
                            () -> {
                                if (preset.lights.size() > 0) {
                                    ClientHelper.setScreen(ScreenManager.buildDeleteConfirmationScreen(
                                        ComponentWrapper.translatable("cloudtweaks.entry.light").getString(),
                                        currentScreen,
                                        () -> {
                                            preset.lights.remove(light);
                                            CloudsConfiguration.save();
                                            ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_light"));
                                            ClientHelper.setScreen(buildEditingScreen());
                                        }
                                    ));
                                }
                            }
                        ));

                    dataCategory.group(group);
                }

                screenBuilder.category(dataCategory);
                currentScreen = screenBuilder.build(parentScreen);
                return currentScreen;
            }
        }
        return null;
    }

    private void addColorPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, CloudColorPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        var backend = BackendHolder.getBackend();

        var infoGroup = backend.createGroup("cloudtweaks.presets.information")
            .description("cloudtweaks.presets.metadata_about");

        infoGroup.option(backend.stringOption(
                "cloudtweaks.presets.name",
                null,
                () -> preset.displayName,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc",
                null,
                () -> preset.description,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.last_modified",
                null,
                () -> lastMod,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.color_count",
                null,
                () -> String.valueOf(preset.colors.size()),
                v -> {}
            ));

        categoryBuilder.group(infoGroup);
    }

    private void addLightingPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightingPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        var backend = BackendHolder.getBackend();

        var infoGroup = backend.createGroup("cloudtweaks.presets.information")
            .description("cloudtweaks.presets.metadata_about");

        infoGroup
            .option(backend.stringOption(
                "cloudtweaks.presets.name",
                null,
                () -> preset.displayName,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc",
                null,
                () -> preset.description,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.last_modified",
                null,
                () -> lastMod,
                v -> {}
            ));

        categoryBuilder.group(infoGroup);
    }

    private void addLightSourcesPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightSourcesPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        var backend = BackendHolder.getBackend();

        var infoGroup = backend.createGroup("cloudtweaks.presets.information")
            .description("cloudtweaks.presets.metadata_about");

        infoGroup
            .option(backend.stringOption(
                "cloudtweaks.presets.name",
                null,
                () -> preset.displayName,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc",
                null,
                () -> preset.description,
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.last_modified",
                null,
                () -> lastMod,
                v -> {}
            ));

        categoryBuilder.group(infoGroup);
    }

    private String formatCategoryName(PresetController.PresetCategory category) {
        return switch (category) {
            case COLORS -> "cloudtweaks.presets.color_presets";
            case LIGHTING -> "cloudtweaks.presets.lighting_presets";
            case LIGHT_SOURCES -> "cloudtweaks.presets.light_sources_presets";
        };
    }
}
