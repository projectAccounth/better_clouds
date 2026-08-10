package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
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

public class PresetEditorScreen {
    private PresetEditorScreen() {}
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    @SuppressWarnings("unused")
    private static String editingPresetId;
    @SuppressWarnings("unused")
    private static String editingPresetCategory;

    private static String editingDisplayName;
    private static String editingDescription;

    public static Screen createEditorScreen(PresetController.PresetCategory category, String presetId, Screen backScreen) {
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
            () -> ClientHelper.setScreen(backScreen)
        ));

        screenBuilder.category(categoryBuilder);
        return screenBuilder.build(backScreen);
    }
    
    public static Screen createMetadataEditScreen(PresetController.PresetCategory category, String presetId, Screen backScreen) {
        editingPresetId = presetId;
        editingPresetCategory = category.name();
        
        switch (category) {
            case COLORS:
                CloudColorPreset colorPreset = PresetController.getColorPreset(presetId);
                if (colorPreset != null) {
                    editingDisplayName = colorPreset.displayName;
                    editingDescription = colorPreset.description;
                }
                break;
            case LIGHTING:
                LightingPreset lightingPreset = PresetController.getLightingPreset(presetId);
                if (lightingPreset != null) {
                    editingDisplayName = lightingPreset.displayName;
                    editingDescription = lightingPreset.description;
                }
                break;
            case LIGHT_SOURCES:
                LightSourcesPreset lightSourcesPreset = PresetController.getLightSourcesPreset(presetId);
                if (lightSourcesPreset != null) {
                    editingDisplayName = lightSourcesPreset.displayName;
                    editingDescription = lightSourcesPreset.description;
                }
                break;
        }

        var backend = BackendHolder.getBackend();

        var screenBuilder = backend.createScreen(formatCategoryName(category), () -> {
            PresetController.updatePresetMetadata(category, presetId, editingDisplayName, editingDescription);
            CloudsConfiguration.save();
        });

        var infoCategory = backend.createCategory("cloudtweaks.presets.information", null);
        
        infoCategory
            .option(backend.stringOption(
                "cloudtweaks.presets.name",
                null,
                () -> editingDisplayName,
                v -> editingDisplayName = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc",
                null,
                () -> editingDescription,
                v -> editingDescription = v
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.back",
                "cloudtweaks.presets.back.tooltip",
                () -> ClientHelper.setScreen(backScreen)
            ));
        
        screenBuilder.category(infoCategory);
        return screenBuilder.build(backScreen);
    }

    public static Screen createPresetEditingScreen(PresetController.PresetCategory category, String presetId, Screen backScreen) {
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
                            Screen refreshedScreen = createPresetEditingScreen(category, presetId, backScreen);
                            if (refreshedScreen != null) {
                                ClientHelper.setScreen(refreshedScreen);
                            }
                            else {
                                ClientHelper.setScreen(backScreen);
                            }
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
                                    preset.colors.remove(kp);
                                    CloudsConfiguration.save();
                                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_keypoint"));
                                    Screen refreshedScreen = createPresetEditingScreen(category, presetId, backScreen);
                                    if (refreshedScreen != null) {
                                        ClientHelper.setScreen(refreshedScreen);
                                    }
                                    else {
                                        ClientHelper.setScreen(backScreen);
                                    }
                                }
                            }
                        ));
                    
                    dataCategory.group(group);
                }

                dataCategory.group(colorGroup);

                screenBuilder.category(dataCategory);
                return screenBuilder.build(backScreen);
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
                    // .option(backend.floatOption(
                    //     "cloudtweaks.option.max_shading",
                    //     null,
                    //     () -> preset.maxShadingStrength,
                    //     v -> preset.maxShadingStrength = v
                    // ).range(0.0f, 1.0f).slider()
                    //     .step(0.05f))
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
                return screenBuilder.build(backScreen);
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
                            Screen refreshedScreen = createPresetEditingScreen(category, presetId, backScreen);
                            if (refreshedScreen != null) {
                                ClientHelper.setScreen(refreshedScreen);
                            }
                            else {
                                ClientHelper.setScreen(backScreen);
                            }
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
                                    preset.lights.remove(light);
                                    CloudsConfiguration.save();
                                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_light"));
                                    Screen refreshedScreen = createPresetEditingScreen(category, presetId, backScreen);
                                    if (refreshedScreen != null) {
                                        ClientHelper.setScreen(refreshedScreen);
                                    }
                                    else {
                                        ClientHelper.setScreen(backScreen);
                                    }
                                }
                            }
                        ));

                    dataCategory.group(group);
                }

                screenBuilder.category(dataCategory);
                return screenBuilder.build(backScreen);
            }
        }
        return null;
    }
    
    private static void addColorPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, CloudColorPreset preset) {
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

        var modeGroup = backend.createGroup("cloudtweaks.presets.color_mode")
            .description("cloudtweaks.presets.color_mode_desc");

        modeGroup.option(backend.stringOption(
                "cloudtweaks.option.cloud_mode",
                null,
                () -> preset.colorMode.name(),
                v -> {}
            ));

        categoryBuilder.group(modeGroup);

        var colorsGroup = backend.createGroup("cloudtweaks.presets.color_keypoints")
            .description("cloudtweaks.presets.color_keypoints_desc");

        if (preset.colors == null || preset.colors.isEmpty()) {
            colorsGroup.option(backend.stringOption(
                "cloudtweaks.presets.no_color_keypoints",
                null,
                () -> "No colors configured",
                v -> {}
            ));
        } else {
            for (int i = 0; i < preset.colors.size(); i++) {
                CloudsConfiguration.SkyColorKeypoint keypoint = preset.colors.get(i);
                colorsGroup.option(backend.stringOption(
                    "cloudtweaks.raw.keypoint",
                    null,
                    () -> formatColorKeypoint(keypoint),
                    v -> {}
                ));
            }
        }

        categoryBuilder.group(colorsGroup);
    }
    
    private static void addLightingPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightingPreset preset) {
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
            ));

        categoryBuilder.group(infoGroup);

        var lightsGroup = backend.createGroup("cloudtweaks.presets.lighting_parameters")
            .description("cloudtweaks.presets.configuration_settings");
        
        lightsGroup.option(backend.floatOption(
                "cloudtweaks.option.ambient_strength",
                null,
                () -> preset.ambientStrength,
                v -> {}
            ).range(0.0f, 2.0f).step(0.1f).slider())
            .option(backend.floatOption(
                "cloudtweaks.option.max_shading",
                null,
                () -> preset.maxShadingStrength,
                v -> {}
            ).range(0.0f, 2.0f).step(0.1f).slider())
            .option(backend.stringOption(
                "cloudtweaks.option.shading_mode",
                null,
                () -> preset.shadingMode.name(),
                v -> {}
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.lighting_type",
                null,
                () -> preset.lightingType.name(),
                v -> {}
            ));

        categoryBuilder.group(lightsGroup);

        var dayGroup = backend.createGroup("cloudtweaks.presets.day_cycle")
            .description("cloudtweaks.presets.day_cycle_desc");
        
        dayGroup.option(backend.integerOption(
                "cloudtweaks.presets.day_start",
                null,
                () -> preset.dayStart,
                v -> {}
            ).range(0, 24000).step(100).slider())
            .option(backend.integerOption(
                "cloudtweaks.presets.day_noon",
                null,
                () -> preset.dayNoon,
                v -> {}
            ).range(0, 24000).step(100).slider())
            .option(backend.integerOption(
                "cloudtweaks.presets.day_end",
                null,
                () -> preset.dayEnd,
                v -> {}
            ).range(0, 24000).step(100).slider());

        categoryBuilder.group(dayGroup);
    }
    
    private static void addLightSourcesPresetDataGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightSourcesPreset preset) {
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
            .option(backend.integerOption(
                "cloudtweaks.presets.light_source_count",
                null,
                () -> preset.lights.size(),
                v -> {}
            ).range(0, ConfigConstants.MAX_LIGHT_COUNT).field());

        categoryBuilder.group(infoGroup);

        var lightsGroup = backend.createGroup("cloudtweaks.option.light_sources")
            .description("cloudtweaks.presets.light_sources_desc");

        if (preset.lights == null || preset.lights.isEmpty()) {
            lightsGroup.option(backend.stringOption(
                "cloudtweaks.presets.no_light_sources",
                null,
                () -> "No light sources configured",
                v -> {}
            ));
        } else {
            for (DiffuseLight light : preset.lights) {
                lightsGroup.option(backend.stringOption(
                    "cloudtweaks.entry.light",
                    null,
                    () -> formatDiffuseLight(light),
                    v -> {}
                ));
            }
        }

        categoryBuilder.group(lightsGroup);
    }
    
    private static String formatColorKeypoint(CloudsConfiguration.SkyColorKeypoint keypoint) {
        return "Time=" + keypoint.time + ", Color=#" + String.format("%06X", keypoint.color);
    }
    
    private static String formatDiffuseLight(DiffuseLight light) {
        return String.format("Direction=(%.2f, %.2f, %.2f), Intensity=%.2f",
            light.getXDirection(), light.getYDirection(), light.getZDirection(), light.intensity());
    }
    
    private static String formatCategoryName(PresetController.PresetCategory category) {
        return switch (category) {
            case COLORS -> "Cloud Colors";
            case LIGHTING -> "Lighting";
            case LIGHT_SOURCES -> "Light Sources";
        };
    }
}
