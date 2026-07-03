package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

import java.awt.Color;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

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
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal(formatCategoryName(category)))
            .save(CloudsConfiguration::save);
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.data"));
        
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
        
        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.back"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.back.tooltip")))
            .action((yacl, btn) -> ClientHelper.setScreen(backScreen))
            .build());
        
        builder.category(categoryBuilder.build());
        return new YACLScreen(builder.build(), backScreen);
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
        
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal(formatCategoryName(category)))
            .save(() -> {
                PresetController.updatePresetMetadata(category, presetId, editingDisplayName, editingDescription);
                CloudsConfiguration.save();
            });
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.information"));
        
        categoryBuilder.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.preset_name")))
            .binding(editingDisplayName, () -> editingDisplayName, v -> editingDisplayName = v)
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.preset_desc")))
            .binding(editingDescription, () -> editingDescription, v -> editingDescription = v)
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.back"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.back.tooltip")))
            .action((yacl, btn) -> ClientHelper.setScreen(backScreen))
            .build());
        
        builder.category(categoryBuilder.build());
        return new YACLScreen(builder.build(), backScreen);
    }

    public static Screen createPresetEditingScreen(PresetController.PresetCategory category, String presetId, Screen backScreen) {
        switch (category) {
            case COLORS: {
                CloudColorPreset preset = PresetController.getColorPreset(presetId);
                if (preset == null) return null;

                // Local editable object is the preset itself
                var builder = YetAnotherConfigLib.createBuilder()
                    .title(ComponentWrapper.translatable("cloudtweaks.presets.edit_color_prefix"))
                    .save(() -> {
                        PresetController.saveColorPresetDirect(preset);
                        CloudsConfiguration.save();
                    });

                var categoryBuilder = ConfigCategory.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.data"));

                // Metadata group
                var infoGroup = OptionGroup.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.information"));

                infoGroup.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
                    .binding(preset.displayName, () -> preset.displayName, v -> preset.displayName = v)
                    .controller(StringControllerBuilder::create)
                    .build());

                infoGroup.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
                    .binding(preset.description, () -> preset.description, v -> preset.description = v)
                    .controller(StringControllerBuilder::create)
                    .build());

                categoryBuilder.group(infoGroup.build());

                // Color keypoints editable
                var colorsGroup = OptionGroup.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.color_presets"));

                if (preset.colors == null) preset.colors = new java.util.ArrayList<>();

                colorsGroup.option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.add_keypoint"))
                    .action((yacl, btn) -> {
                        CloudsConfiguration.SkyColorKeypoint newKp = new CloudsConfiguration.SkyColorKeypoint();
                        newKp.time = preset.colors.isEmpty() ? 0 : preset.colors.get(preset.colors.size() - 1).time + 1000;
                        newKp.color = 0xFFFFFF;
                        preset.colors.add(newKp);
                        CloudsConfiguration.save();
                        ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.added_color_keypoint"));
                        ClientHelper.setScreen(backScreen);
                    })
                    .build());

                for (int i = 0; i < preset.colors.size(); i++) {
                    CloudsConfiguration.SkyColorKeypoint kp = preset.colors.get(i);
                    var groupBuilder = OptionGroup.createBuilder()
                        .name(ComponentWrapper.translatable("cloudtweaks.raw.keypoint"))
                        .option(Option.<Integer>createBuilder()
                            .name(ComponentWrapper.translatable("cloudtweaks.raw.time"))
                            .binding(kp.time, () -> kp.time, v -> kp.time = v)
                            .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 24000))
                            .build())
                            .option(Option.<Color>createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.raw.color"))
                            .binding(new Color(kp.color), () -> new Color(kp.color), v -> kp.color = v.getRGB())
                            .controller(ColorControllerBuilder::create)
                            .build())
                            .option(ButtonOption.createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.raw.remove"))
                            .action((yacl, btn) -> {
                                if (preset.colors.size() > 0) {
                                    preset.colors.remove(kp);
                                    CloudsConfiguration.save();
                                        ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_keypoint"));
                                    ClientHelper.setScreen(backScreen);
                                }
                            })
                            .build());

                    categoryBuilder.group(groupBuilder.build());
                }

                categoryBuilder.group(colorsGroup.build());

                builder.category(categoryBuilder.build());
                return new YACLScreen(builder.build(), backScreen);
            }
            case LIGHTING: {
                LightingPreset preset = PresetController.getLightingPreset(presetId);
                if (preset == null) return null;

                var builder = YetAnotherConfigLib.createBuilder()
                    .title(ComponentWrapper.translatable("cloudtweaks.presets.edit_lighting_prefix"))
                    .save(() -> {
                        PresetController.saveLightingPresetDirect(preset);
                        CloudsConfiguration.save();
                    });

                var categoryBuilder = ConfigCategory.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.lighting_data"));

                categoryBuilder.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.literal("Display Name"))
                    .binding(preset.displayName, () -> preset.displayName, v -> preset.displayName = v)
                    .controller(StringControllerBuilder::create)
                    .build());

                categoryBuilder.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.literal("Description"))
                    .binding(preset.description, () -> preset.description, v -> preset.description = v)
                    .controller(StringControllerBuilder::create)
                    .build());

                categoryBuilder.option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.ambient_strength"))
                    .binding(preset.ambientStrength, () -> preset.ambientStrength, v -> preset.ambientStrength = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                    .build());

                categoryBuilder.option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.max_shading"))
                    .binding(preset.maxShadingStrength, () -> preset.maxShadingStrength, v -> preset.maxShadingStrength = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                    .build());

                categoryBuilder.option(Option.<CloudsConfiguration.ShadingMode>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.shading_mode"))
                    .binding(preset.shadingMode, () -> preset.shadingMode, v -> preset.shadingMode = v)
                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(CloudsConfiguration.ShadingMode.class))
                    .build());

                categoryBuilder.option(Option.<CloudsConfiguration.LightingType>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.lighting_type"))
                    .binding(preset.lightingType, () -> preset.lightingType, v -> preset.lightingType = v)
                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(CloudsConfiguration.LightingType.class))
                    .build());

                categoryBuilder.option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.day_start"))
                    .binding(preset.dayStart, () -> preset.dayStart, v -> preset.dayStart = v)
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 24000))
                    .build());

                categoryBuilder.option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.literal("Day Noon"))
                    .binding(preset.dayNoon, () -> preset.dayNoon, v -> preset.dayNoon = v)
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 24000))
                    .build());

                categoryBuilder.option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.literal("Day End"))
                    .binding(preset.dayEnd, () -> preset.dayEnd, v -> preset.dayEnd = v)
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 24000))
                    .build());

                builder.category(categoryBuilder.build());
                return new YACLScreen(builder.build(), backScreen);
            }
            case LIGHT_SOURCES: {
                LightSourcesPreset preset = PresetController.getLightSourcesPreset(presetId);
                if (preset == null) return null;

                var builder = YetAnotherConfigLib.createBuilder()
                    .title(ComponentWrapper.translatable("cloudtweaks.presets.edit_light_sources_prefix"))
                    .save(() -> {
                        PresetController.saveLightSourcesPresetDirect(preset);
                        CloudsConfiguration.save();
                    });

                var categoryBuilder = ConfigCategory.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.light_sources"));

                if (preset.lights == null) preset.lights = new java.util.ArrayList<>();

                categoryBuilder.option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.add_light"))
                    .action((yacl, btn) -> {
                        preset.lights.add(new DiffuseLight());
                        CloudsConfiguration.save();
                        ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.added_light"));
                        ClientHelper.setScreen(backScreen);
                    })
                    .build());

                for (int i = 0; i < preset.lights.size(); i++) {
                    DiffuseLight light = preset.lights.get(i);
                    var groupBuilder = OptionGroup.createBuilder()
                        .name(ComponentWrapper.translatable("cloudtweaks.entry.light"))
                        .option(Option.<Float>createBuilder()
                            .name(ComponentWrapper.translatable("cloudtweaks.option.direction_x"))
                            .binding(light.getXDirection(), light::getXDirection, light::setXDirection)
                            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                            .build())
                            .option(Option.<Float>createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.option.direction_y"))
                            .binding(light.getYDirection(), light::getYDirection, light::setYDirection)
                            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                            .build())
                            .option(Option.<Float>createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.option.direction_z"))
                            .binding(light.getZDirection(), light::getZDirection, light::setZDirection)
                            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                            .build())
                            .option(Option.<Float>createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.option.intensity"))
                            .binding(light.intensity(), light::intensity, light::setIntensity)
                            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                            .build())
                            .option(ButtonOption.createBuilder()
                                .name(ComponentWrapper.translatable("cloudtweaks.raw.remove"))
                            .action((yacl, btn) -> {
                                if (preset.lights.size() > 0) {
                                    preset.lights.remove(light);
                                    CloudsConfiguration.save();
                                        ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.presets.removed_light"));
                                    ClientHelper.setScreen(backScreen);
                                }
                            })
                            .build());

                    categoryBuilder.group(groupBuilder.build());
                }

                builder.category(categoryBuilder.build());
                return new YACLScreen(builder.build(), backScreen);
            }
        }
        return null;
    }
    
    private static void addColorPresetDataGroup(ConfigCategory.Builder categoryBuilder, CloudColorPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
                var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.information"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.metadata_about")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.last_modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.color_count"))
            .binding(preset.colors.size(), () -> preset.colors.size(), v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24))
            .build());
        
        categoryBuilder.group(infoGroup.build());
        
        var modeGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.color_mode"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.color_mode_desc")));
        
        modeGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.cloud_mode"))
            .binding(preset.colorMode.name(), () -> preset.colorMode.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(modeGroup.build());
        
        var colorsGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.color_keypoints"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.color_keypoints_desc")));
        
        if (preset.colors == null || preset.colors.isEmpty()) {
            colorsGroup.option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.no_color_keypoints"))
                .binding("No colors configured", () -> "No colors configured", v -> {})
                .controller(StringControllerBuilder::create)
                .build());
        } else {
            for (int i = 0; i < preset.colors.size(); i++) {
                int index = i;
                CloudsConfiguration.SkyColorKeypoint keypoint = preset.colors.get(index);
                colorsGroup.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.keypoint"))
                    .binding(formatColorKeypoint(keypoint), () -> formatColorKeypoint(keypoint), v -> {})
                    .controller(StringControllerBuilder::create)
                    .build());
            }
        }
        
        categoryBuilder.group(colorsGroup.build());
    }
    
    private static void addLightingPresetDataGroup(ConfigCategory.Builder categoryBuilder, LightingPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.information"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.metadata_about")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.last_modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(infoGroup.build());
        
        // Lighting parameters
        var lightsGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.lighting_parameters"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.configuration_settings")));
        
        lightsGroup.option(Option.<Float>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.ambient_strength"))
            .binding(preset.ambientStrength, () -> preset.ambientStrength, v -> {})
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.1f))
            .build());
        
        lightsGroup.option(Option.<Float>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.max_shading"))
            .binding(preset.maxShadingStrength, () -> preset.maxShadingStrength, v -> {})
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.1f))
            .build());
        
        lightsGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.shading_mode"))
            .binding(preset.shadingMode.name(), () -> preset.shadingMode.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        lightsGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.lighting_type"))
            .binding(preset.lightingType.name(), () -> preset.lightingType.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(lightsGroup.build());
        
        // Day cycle
        var dayGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.day_cycle"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.day_cycle_desc")));
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.day_start"))
            .binding(preset.dayStart, () -> preset.dayStart, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.day_noon"))
            .binding(preset.dayNoon, () -> preset.dayNoon, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.day_end"))
            .binding(preset.dayEnd, () -> preset.dayEnd, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        categoryBuilder.group(dayGroup.build());
    }
    
    private static void addLightSourcesPresetDataGroup(ConfigCategory.Builder categoryBuilder, LightSourcesPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.information"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.metadata_about")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.last_modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.light_source_count"))
            .binding(preset.lights.size(), () -> preset.lights.size(), v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, ConfigConstants.MAX_LIGHT_COUNT).step(1))
            .build());
        
        categoryBuilder.group(infoGroup.build());
        
        var lightsGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.light_sources"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_desc")));
        
        if (preset.lights == null || preset.lights.isEmpty()) {
            lightsGroup.option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.no_light_sources"))
                .binding("No light sources configured", () -> "No light sources configured", v -> {})
                .controller(StringControllerBuilder::create)
                .build());
        } else {
            for (int i = 0; i < preset.lights.size(); i++) {
                int index = i;
                DiffuseLight light = preset.lights.get(index);
                lightsGroup.option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.entry.light"))
                    .binding(formatDiffuseLight(light), () -> formatDiffuseLight(light), v -> {})
                    .controller(StringControllerBuilder::create)
                    .build());
            }
        }
        
        categoryBuilder.group(lightsGroup.build());
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
