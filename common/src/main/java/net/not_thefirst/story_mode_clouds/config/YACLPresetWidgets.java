package net.not_thefirst.story_mode_clouds.config;

import dev.isxander.yacl3.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.config.screens.PresetBrowserScreen;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

/**
 * YACL Preset Widgets - Simplified UI with preset browser integration.
 * Replaces dropdowns with browse buttons for each preset category.
 */
public class YACLPresetWidgets {
    private YACLPresetWidgets() {}

    private static Screen parentScreen = null;

    public static void setParentScreen(Screen screen) {
        parentScreen = screen;
    }

    public static ConfigCategory createPresetsCategory() {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.presets"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.category.presets"));

        CloudsConfiguration config = CloudsConfiguration.getInstance();

        // Quick Actions section
        var quickActionsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.quick_actions"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.quick_actions.tooltip")))
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.save_color_preset"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_color_preset.tooltip")))
                .action((yacl, btn) -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "CloudPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveColorPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("§aSaved color preset: " + presetName)
                        );
                    }
                })
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.save_lighting_preset"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_lighting_preset.tooltip")))
                .action((yacl, btn) -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("§aSaved lighting preset: " + presetName)
                        );
                    }
                })
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.save_light_sources_preset"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_light_sources_preset.tooltip")))
                .action((yacl, btn) -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightSourcesPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightSourcesPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("§aSaved light sources preset: " + presetName)
                        );
                    }
                })
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.reset_to_default"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.reset_to_default.tooltip")))
                .action((yacl, btn) -> {
                    boolean success = true;
                    success &= PresetController.loadColorPreset("default", config);
                    success &= PresetController.loadLightingPreset("default", config);
                    success &= PresetController.loadLightSourcesPreset("default", config);
                    
                    if (success) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("§aReset to default presets")
                        );
                    }
                })
                .build());

        builder.group(quickActionsBuilder.build());

        // COLOR PRESETS SECTION
        var colorPresetsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.color_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.color_presets.tooltip")));
        
        int colorCount = PresetController.getPresetCount(PresetController.PresetCategory.COLORS);
        colorPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Browse Presets (" + colorCount + ")"))
            .description(OptionDescription.of(ComponentWrapper.literal("Open preset browser to manage, edit, and load color presets")))
            .action((yacl, btn) -> {
                if (parentScreen != null) {
                    ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                        PresetController.PresetCategory.COLORS, parentScreen
                    ));
                }
            })
            .build());
        
        builder.group(colorPresetsBuilder.build());

        var lightingPresetsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.lighting_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.lighting_presets.tooltip")));
        
        int lightingCount = PresetController.getPresetCount(PresetController.PresetCategory.LIGHTING);
        lightingPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Browse Presets (" + lightingCount + ")"))
            .description(OptionDescription.of(ComponentWrapper.literal("Open preset browser to manage, edit, and load lighting presets")))
            .action((yacl, btn) -> {
                if (parentScreen != null) {
                    ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                        PresetController.PresetCategory.LIGHTING, parentScreen
                    ));
                }
            })
            .build());
        
        builder.group(lightingPresetsBuilder.build());

        // LIGHT SOURCES PRESETS SECTION
        var lightSourcesPresetsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets.tooltip")));
        
        int lightSourcesCount = PresetController.getPresetCount(PresetController.PresetCategory.LIGHT_SOURCES);
        lightSourcesPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Browse Presets (" + lightSourcesCount + ")"))
            .description(OptionDescription.of(ComponentWrapper.literal("Open preset browser to manage, edit, and load light source presets")))
            .action((yacl, btn) -> {
                if (parentScreen != null) {
                    ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                        PresetController.PresetCategory.LIGHT_SOURCES, parentScreen
                    ));
                }
            })
            .build());
        
        builder.group(lightSourcesPresetsBuilder.build());

        return builder.build();
    }
    
    private static String formatTimestamp(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }
}
