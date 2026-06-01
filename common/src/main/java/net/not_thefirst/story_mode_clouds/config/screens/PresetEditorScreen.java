package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.config.presets.*;

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
            .title(ComponentWrapper.literal(formatCategoryName(category) + " - View Preset"))
            .save(CloudsConfiguration::save);
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.literal("Preset Data"));
        
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
            .name(ComponentWrapper.literal("Back"))
            .description(OptionDescription.of(ComponentWrapper.literal("Return to preset browser")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) mc.setScreen(backScreen);
            })
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
            .title(ComponentWrapper.literal(formatCategoryName(category) + " - Edit Metadata"))
            .save(() -> {
                PresetController.updatePresetMetadata(category, presetId, editingDisplayName, editingDescription);
                CloudsConfiguration.save();
            });
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.literal("Preset Information"));
        
        categoryBuilder.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Display Name"))
            .description(OptionDescription.of(ComponentWrapper.literal("The name shown in menus")))
            .binding(editingDisplayName, () -> editingDisplayName, v -> editingDisplayName = v)
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Description"))
            .description(OptionDescription.of(ComponentWrapper.literal("A brief description of this preset")))
            .binding(editingDescription, () -> editingDescription, v -> editingDescription = v)
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Back"))
            .description(OptionDescription.of(ComponentWrapper.literal("Return to preset browser (changes not saved)")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) mc.setScreen(backScreen);
            })
            .build());
        
        builder.category(categoryBuilder.build());
        return new YACLScreen(builder.build(), backScreen);
    }
    
    private static void addColorPresetDataGroup(ConfigCategory.Builder categoryBuilder, CloudColorPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Information"))
            .description(OptionDescription.of(ComponentWrapper.literal("Metadata about this preset")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Display Name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Description"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Last Modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.literal("Color Count"))
            .binding(preset.colors.size(), () -> preset.colors.size(), v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24))
            .build());
        
        categoryBuilder.group(infoGroup.build());
        
        // Color mode
        var modeGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Color Mode"))
            .description(OptionDescription.of(ComponentWrapper.literal("How colors are applied")));
        
        modeGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Mode"))
            .binding(preset.colorMode.name(), () -> preset.colorMode.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(modeGroup.build());
    }
    
    private static void addLightingPresetDataGroup(ConfigCategory.Builder categoryBuilder, LightingPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Information"))
            .description(OptionDescription.of(ComponentWrapper.literal("Metadata about this preset")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Display Name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Description"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Last Modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(infoGroup.build());
        
        // Lighting parameters
        var lightsGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Lighting Parameters"))
            .description(OptionDescription.of(ComponentWrapper.literal("Configuration settings")));
        
        lightsGroup.option(Option.<Float>createBuilder()
            .name(ComponentWrapper.literal("Ambient Strength"))
            .binding(preset.ambientStrength, () -> preset.ambientStrength, v -> {})
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.1f))
            .build());
        
        lightsGroup.option(Option.<Float>createBuilder()
            .name(ComponentWrapper.literal("Max Shading Strength"))
            .binding(preset.maxShadingStrength, () -> preset.maxShadingStrength, v -> {})
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.1f))
            .build());
        
        lightsGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Shading Mode"))
            .binding(preset.shadingMode.name(), () -> preset.shadingMode.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        lightsGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Lighting Type"))
            .binding(preset.lightingType.name(), () -> preset.lightingType.name(), v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        categoryBuilder.group(lightsGroup.build());
        
        // Day cycle
        var dayGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Day Cycle"))
            .description(OptionDescription.of(ComponentWrapper.literal("Time-based lighting")));
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.literal("Day Start"))
            .binding(preset.dayStart, () -> preset.dayStart, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.literal("Day Noon"))
            .binding(preset.dayNoon, () -> preset.dayNoon, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        dayGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.literal("Day End"))
            .binding(preset.dayEnd, () -> preset.dayEnd, v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
            .build());
        
        categoryBuilder.group(dayGroup.build());
    }
    
    private static void addLightSourcesPresetDataGroup(ConfigCategory.Builder categoryBuilder, LightSourcesPreset preset) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var infoGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal("Information"))
            .description(OptionDescription.of(ComponentWrapper.literal("Metadata about this preset")));
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Display Name"))
            .binding(preset.displayName, () -> preset.displayName, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Description"))
            .binding(preset.description, () -> preset.description, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.literal("Last Modified"))
            .binding(lastMod, () -> lastMod, v -> {})
            .controller(StringControllerBuilder::create)
            .build());
        
        infoGroup.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.literal("Light Source Count"))
            .binding(preset.lights.size(), () -> preset.lights.size(), v -> {})
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 16).step(1))
            .build());
        
        categoryBuilder.group(infoGroup.build());
    }
    
    private static String formatCategoryName(PresetController.PresetCategory category) {
        return switch (category) {
            case COLORS -> "Cloud Colors";
            case LIGHTING -> "Lighting";
            case LIGHT_SOURCES -> "Light Sources";
        };
    }
}
