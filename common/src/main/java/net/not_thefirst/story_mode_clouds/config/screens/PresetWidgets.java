package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
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
public class PresetWidgets {
    private PresetWidgets() {}

    private static Screen parentScreen = null;

    public static void setParentScreen(Screen screen) {
        parentScreen = screen;
    }

    public static ConfigBackend.ConfigCategoryBuilder createPresetsCategory() {
        var backend = BackendHolder.getBackend();
        var builder = backend.createCategory("cloudtweaks.category.presets", "cloudtweaks.category.presets");

        CloudsConfiguration config = CloudsConfiguration.getInstance();

        var quickActions = backend.createGroup("cloudtweaks.presets.quick_actions");

        quickActions
            .action(backend.createAction(
                "cloudtweaks.presets.save_color_preset",
                "cloudtweaks.presets.save_color_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "CloudPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveColorPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_color_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.save_lighting_preset",
                "cloudtweaks.presets.save_lighting_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_lighting_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.save_light_sources_preset",
                "cloudtweaks.presets.save_light_sources_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightSourcesPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightSourcesPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_light_sources_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.reset_to_default",
                "cloudtweaks.presets.reset_to_default.tooltip",
                () -> {
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
                }
            ));

        builder.group(quickActions);

        /**
         * color presets
         **/

        var colorPresets = backend.createGroup("cloudtweaks.presets.color_presets");

        colorPresets
            .action(backend.createAction(
                "cloudtweaks.presets.color_presets",
                "cloudtweaks.presets.color_presets.tooltip",
                () -> {
                    if (parentScreen != null) {
                        ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                            PresetController.PresetCategory.COLORS, parentScreen
                        ));
                    }
                }
            ));
        
        builder.group(colorPresets);

        var lightingPresets = backend.createGroup("cloudtweaks.presets.lighting_presets");

        lightingPresets
            .action(backend.createAction(
                "cloudtweaks.presets.lighting_presets",
                "cloudtweaks.presets.lighting_presets.tooltip",
                () -> {
                    if (parentScreen != null) {
                        ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                            PresetController.PresetCategory.LIGHTING, parentScreen
                        ));
                    }
                }
            ));
        
        builder.group(lightingPresets);

        /**
         * light sources
         */
        var lightSourcesPresets = backend.createGroup("cloudtweaks.presets.light_sources_presets");

        lightSourcesPresets
            .action(backend.createAction(
                "cloudtweaks.presets.light_sources_presets",
                "cloudtweaks.presets.light_sources_presets.tooltip",
                () -> {
                    if (parentScreen != null) {
                        ClientHelper.setScreen(PresetBrowserScreen.createBrowserScreen(
                            PresetController.PresetCategory.LIGHT_SOURCES, parentScreen
                        ));
                    }
                }
            ));
        
        builder.group(lightSourcesPresets);

        return builder;
    }
    
    private static String formatTimestamp(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }
}
