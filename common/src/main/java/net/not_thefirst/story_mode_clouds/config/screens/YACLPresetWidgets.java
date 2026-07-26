package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

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

        /**
         * qas
         */
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
                            ComponentWrapper.translatable("cloudtweaks.message.saved_color_preset_prefix")
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
                            ComponentWrapper.translatable("cloudtweaks.message.saved_lighting_preset_prefix")
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
                            ComponentWrapper.translatable("cloudtweaks.message.saved_light_sources_preset_prefix")
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
                            ComponentWrapper.translatable("cloudtweaks.presets.reset_to_default")
                        );
                    }
                })
                .build());

        builder.group(quickActionsBuilder.build());

        /**
         * color presets
         **/
        var colorPresetsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.color_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.color_presets.tooltip")));
        
        colorPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.my_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.color_presets.tooltip")))
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
        
        lightingPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.lighting_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.lighting_presets.tooltip")))
            .action((yacl, btn) -> {
                if (parentScreen != null) {
                    ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                        PresetController.PresetCategory.LIGHTING, parentScreen
                    ));
                }
            })
            .build());
        
        builder.group(lightingPresetsBuilder.build());


        /**
         * light sources
         */
        var lightSourcesPresetsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets.tooltip")));
        
        lightSourcesPresetsBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.light_sources_presets.tooltip")))
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
