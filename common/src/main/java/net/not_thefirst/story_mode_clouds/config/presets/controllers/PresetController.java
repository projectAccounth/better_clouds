package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.util.List;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.CloudColorPreset;
import net.not_thefirst.story_mode_clouds.config.presets.LightSourcesPreset;
import net.not_thefirst.story_mode_clouds.config.presets.LightingPreset;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * For compat, soon to be removed
 */
public final class PresetController {
    private static final ColorPresetController COLORS = ColorPresetController.getInstance();
    private static final LightingPresetController LIGHTING = LightingPresetController.getInstance();
    private static final LightSourcesPresetController LIGHT_SOURCES = LightSourcesPresetController.getInstance();
    private static boolean initialized;

    private PresetController() {}

    public enum PresetCategory {
        COLORS("colors"),
        LIGHTING("lighting"),
        LIGHT_SOURCES("light_sources");

        public final String dirName;

        PresetCategory(String dirName) {
            this.dirName = dirName;
        }
    }

    public static synchronized void initialize() {
        if (initialized) return;
        COLORS.initialize();
        LIGHTING.initialize();
        LIGHT_SOURCES.initialize();
        createDefaultPresets();
        initialized = true;
        LoggerProvider.get().info("Preset controller initialized successfully");
    }

    public static boolean saveColorPreset(String id, String name, String description, CloudsConfiguration config) {
        initialize();
        return COLORS.savePreset(id, name, description, config);
    }

    public static boolean saveColorPresetDirect(CloudColorPreset preset) {
        initialize();
        if (preset.colors != null) preset.sortColorsByTime();
        preset.lastModified = System.currentTimeMillis();
        return COLORS.save(preset);
    }

    public static boolean loadColorPreset(String id, CloudsConfiguration config) {
        initialize();
        return COLORS.applyPreset(id, config);
    }

    public static CloudColorPreset getColorPreset(String id) { initialize(); return COLORS.getPreset(id); }
    public static List<CloudColorPreset> listColorPresets() { initialize(); return COLORS.listPresets(); }
    public static boolean deleteColorPreset(String id) { initialize(); return COLORS.deletePreset(id); }
    public static String exportColorPresetAsBase64(String id) { initialize(); return COLORS.exportPresetAsBase64(id); }
    public static boolean importColorPresetFromPayload(String id, String name, String description, String payload) {
        initialize();
        return COLORS.importPresetFromPayload(id, name, description, payload);
    }

    public static boolean saveLightingPreset(String id, String name, String description, CloudsConfiguration config) {
        initialize();
        return LIGHTING.savePreset(id, name, description, config);
    }

    public static boolean saveLightingPresetDirect(LightingPreset preset) {
        initialize();
        preset.lastModified = System.currentTimeMillis();
        return LIGHTING.save(preset);
    }

    public static boolean loadLightingPreset(String id, CloudsConfiguration config) {
        initialize();
        return LIGHTING.applyPreset(id, config);
    }

    public static LightingPreset getLightingPreset(String id) { initialize(); return LIGHTING.getPreset(id); }
    public static List<LightingPreset> listLightingPresets() { initialize(); return LIGHTING.listPresets(); }
    public static boolean deleteLightingPreset(String id) { initialize(); return LIGHTING.deletePreset(id); }
    public static String exportLightingPresetAsBase64(String id) { initialize(); return LIGHTING.exportPresetAsBase64(id); }
    public static boolean importLightingPresetFromPayload(String id, String name, String description, String payload) {
        initialize();
        return LIGHTING.importPresetFromPayload(id, name, description, payload);
    }

    public static boolean saveLightSourcesPreset(String id, String name, String description, CloudsConfiguration config) {
        initialize();
        return LIGHT_SOURCES.savePreset(id, name, description, config);
    }

    public static boolean saveLightSourcesPresetDirect(LightSourcesPreset preset) {
        initialize();
        preset.lastModified = System.currentTimeMillis();
        return LIGHT_SOURCES.save(preset);
    }

    public static boolean loadLightSourcesPreset(String id, CloudsConfiguration config) {
        initialize();
        return LIGHT_SOURCES.applyPreset(id, config);
    }

    public static LightSourcesPreset getLightSourcesPreset(String id) { initialize(); return LIGHT_SOURCES.getPreset(id); }
    public static List<LightSourcesPreset> listLightSourcesPresets() { initialize(); return LIGHT_SOURCES.listPresets(); }
    public static boolean deleteLightSourcesPreset(String id) { initialize(); return LIGHT_SOURCES.deletePreset(id); }
    public static String exportLightSourcesPresetAsBase64(String id) { initialize(); return LIGHT_SOURCES.exportPresetAsBase64(id); }
    public static boolean importLightSourcesPresetFromPayload(String id, String name, String description, String payload) {
        initialize();
        return LIGHT_SOURCES.importPresetFromPayload(id, name, description, payload);
    }

    public static int getTotalPresetCount() {
        initialize();
        return listColorPresets().size() + listLightingPresets().size() + listLightSourcesPresets().size();
    }

    public static boolean validatePresetPayload(PresetCategory category, String payload) {
        return switch (category) {
            case COLORS -> COLORS.validatePayload(payload);
            case LIGHTING -> LIGHTING.validatePayload(payload);
            case LIGHT_SOURCES -> LIGHT_SOURCES.validatePayload(payload);
        };
    }

    public static int getPresetCount(PresetCategory category) {
        return switch (category) {
            case COLORS -> listColorPresets().size();
            case LIGHTING -> listLightingPresets().size();
            case LIGHT_SOURCES -> listLightSourcesPresets().size();
        };
    }

    public static boolean updatePresetMetadata(PresetCategory category, String id, String name, String description) {
        return switch (category) {
            case COLORS -> COLORS.updateMetadata(id, name, description);
            case LIGHTING -> LIGHTING.updateMetadata(id, name, description);
            case LIGHT_SOURCES -> LIGHT_SOURCES.updateMetadata(id, name, description);
        };
    }

    private static void createDefaultPresets() {
        if (COLORS.getPreset("default") == null) {
            CloudColorPreset preset = new CloudColorPreset("default", "Default Colors", "Default cloud color progression");
            preset.colors = new java.util.ArrayList<>(CloudsConfiguration.DEFAULT_COLORS);
            preset.colorMode = CloudsConfiguration.CloudColorProviderMode.VANILLA;
            COLORS.save(preset);
        }
        if (LIGHTING.getPreset("default") == null) {
            LIGHTING.save(new LightingPreset("default", "Default Lighting", "Default lighting parameters"));
        }
        if (LIGHT_SOURCES.getPreset("default") == null) {
            LightSourcesPreset preset = new LightSourcesPreset("default", "Default Light Sources", "Default light source configuration");
            preset.lights = new java.util.ArrayList<>(CloudsConfiguration.LightingParameters.DEFAULT_LIGHTS);
            LIGHT_SOURCES.save(preset);
        }
    }
}
