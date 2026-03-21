package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * Manages configuration presets for easy switching between different cloud setups.
 * Presets are stored in JSON format and can be loaded/saved independently.
 */
public class ConfigPresets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PRESETS_DIR = new File("config/cloud_presets");
    private static final File PRESETS_INDEX = new File(PRESETS_DIR, "presets.json");
    private static final File BACKUPS_DIR = new File(PRESETS_DIR, "backups");
    private static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

    private static final Map<String, PresetMetadata> PRESETS = new HashMap<>();
    
    private static final Map<String, java.io.File> RESOURCEPACK_PRESET_FILES = new HashMap<>();
    private static boolean INITIALIZED = false;
    private static int IMPORTED_COUNT = 0;
    
    private static final Map<String, Long> LAST_BACKUP_TIME = new HashMap<>();
    private static final long BACKUP_THROTTLE_MS = 5000; // Minimum 5s
    
    // Base64 caching to prevent expensive serialization on every frame
    private static String cachedBase64 = null;
    private static long lastConfigModifiedTime = 0;

    /**
     * Metadata about a preset configuration.
     */
    public static class PresetMetadata {
        public String id;
        public String displayName;
        public String description;
        public long lastModified;
        public String configFile;
        public boolean fromResourcePack = false;
        public String resourcePackName = null;
        public String resourcePath = null;

        public PresetMetadata(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.lastModified = System.currentTimeMillis();
            this.configFile = id + ".json";
        }
    }

    /**
     * Initialize the presets system and load all available presets.
     */
    public static void initialize() {
        if (INITIALIZED) return;

        PRESETS_DIR.mkdirs();
        BACKUPS_DIR.mkdirs();
        loadPresetsIndex();
        scanResourcePacks();
        createDefaultPreset();
        INITIALIZED = true;
    }

    /**
     * Load presets from active resource packs using ResourceManager.
     * This should be called from the config screen context where ResourceManager is available.
     */
    public static void loadResourcePackPresetsFromManager(ResourceManager resourceManager) {
        if (resourceManager == null) return;
        LoggerProvider.get().info("Scanning resource packs for cloud presets...");
        
        // Try to load presets from common modded namespaces
        String[] commonNamespaces = {"cloud_tweaks", "minecraft", "modname"};
        for (String namespace : commonNamespaces) {
            loadPresetsFromNamespace(resourceManager, namespace);
        }
    }

    /**
     * Load presets from a specific namespace using ResourceManager and IdentifierWrapper.
     * Looks for JSON files in assets/<namespace>/cloud_presets/
     */
    public static void loadPresetsFromNamespace(ResourceManager resourceManager, String namespace) {
        try {
            // Try common preset file names or enumerate if possible
            String[] presetNames = {"basic", "fantasy", "stormy", "sunset", "night", "vanilla"};
            for (String presetName : presetNames) {
                IdentifierWrapper presetId = IdentifierWrapper.of(
                    namespace,
                    "cloud_presets/" + presetName + ".json"
                );
                try {
                    CloudsConfiguration config = ResourcePackPresetLoader.loadPresetFromResources(
                        resourceManager,
                        presetId
                    );
                    if (config != null) {
                        String rpPresetId = "rp:" + namespace + ":" + presetName;
                        if (!PRESETS.containsKey(rpPresetId)) {
                            PresetMetadata metadata = new PresetMetadata(
                                rpPresetId,
                                presetName,
                                "From resource pack: " + namespace
                            );
                            metadata.fromResourcePack = true;
                            metadata.resourcePackName = namespace;
                            PRESETS.put(rpPresetId, metadata);
                            LoggerProvider.get().info("Loaded resource pack preset: " + rpPresetId);
                        }
                    }
                } catch (Exception ignored) {
                    // Preset doesn't exist in this namespace, skip
                }
            }
        } catch (Exception e) {
            LoggerProvider.get().warn("Error loading presets from namespace " + namespace + ": " + e.getMessage());
        }
    }

    /**
     * Load the presets index from disk.
     */
    private static void loadPresetsIndex() {
        if (!PRESETS_INDEX.exists()) {
            LoggerProvider.get().info("No presets index found, creating fresh");
            return;
        }

        try (FileReader reader = new FileReader(PRESETS_INDEX)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("presets")) {
                PresetMetadata[] presets = GSON.fromJson(
                        json.getAsJsonArray("presets"),
                        PresetMetadata[].class
                );
                for (PresetMetadata preset : presets) {
                    PRESETS.put(preset.id, preset);
                }
                LoggerProvider.get().info("Loaded " + PRESETS.size() + " presets");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the presets index to disk using atomic write.
     */
    private static void savePresetsIndex() {
        try {
            safeWriteFile(PRESETS_INDEX, () -> {
                JsonObject json = new JsonObject();
                json.add("presets", GSON.toJsonTree(PRESETS.values().toArray(new PresetMetadata[0])));
                return GSON.toJson(json);
            });
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save presets index");
            e.printStackTrace();
        }
    }

    /**
     * Scan resourcepack folder for presets
     * Builds RESOURCEPACK_PRESET_FILES and adds metadata entries (not persisted).
     */
    private static void scanResourcePacks() {
        if (true) {
            // no-op, tryna figure out how to safely access the resourcepack folder
            return; 
        }
        RESOURCEPACK_PRESET_FILES.clear();
        File rpRoot = new File("saiuhdoawijfoasdj/resourcepacks");
        if (!rpRoot.exists() || !rpRoot.isDirectory()) return;

        File[] packs = rpRoot.listFiles(File::isDirectory);
        if (packs == null) return;

        for (File pack : packs) {
            File assets = new File(pack, "assets");
            if (!assets.exists() || !assets.isDirectory()) continue;
            File[] namespaces = assets.listFiles(File::isDirectory);
            if (namespaces == null) continue;
            for (File ns : namespaces) {
                File presetsDir = new File(ns, "cloud_presets");
                if (!presetsDir.exists() || !presetsDir.isDirectory()) continue;
                File[] presetFiles = presetsDir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
                if (presetFiles == null) continue;
                for (File f : presetFiles) {
                    String name = f.getName();
                    String base = name.substring(0, name.length() - 5);
                    String id = "rp:" + ns.getName() + ":" + base;
                    RESOURCEPACK_PRESET_FILES.put(id, f);
                }
            }
        }
    }

    /**
     * Create and save a new preset from the current configuration.
     *
     * @param presetId Unique identifier for the preset
     * @param displayName User-friendly name
     * @param description Short description of the preset
     * @return true if successful, false otherwise
     */
    public static boolean savePreset(String presetId, String displayName, String description) {
        initialize();
        PresetMetadata metadata = new PresetMetadata(presetId, displayName, description);
        File configFile = new File(PRESETS_DIR, metadata.configFile);

        try (FileWriter writer = new FileWriter(configFile)) {
            String json = ConfigSerializer.toJson(CloudsConfiguration.getInstance());
            writer.write(json);
            PRESETS.put(presetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Saved preset: " + presetId);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a preset and apply it as the current configuration (with backup of current).
     *
     * @param presetId The preset to load
     * @return true if successful, false otherwise
     */
    public static boolean loadPreset(String presetId) {
        initialize();
        
        // Create backup of current config before loading new one
        try {
            backupCurrentConfig("before_load_" + presetId);
        } catch (IOException e) {
            LoggerProvider.get().warn("Could not create backup before loading preset");
        }
        
        // Support resourcepack presets prefixed with rp:namespace:name
        if (presetId != null && presetId.startsWith("rp:")) {
            java.io.File f = RESOURCEPACK_PRESET_FILES.get(presetId);
            if (f == null) {
                // attempt to scan and retry
                scanResourcePacks();
                f = RESOURCEPACK_PRESET_FILES.get(presetId);
                if (f == null) {
                    LoggerProvider.get().warn("Resourcepack preset not found: " + presetId);
                    return false;
                }
            }
            try (FileReader reader = new FileReader(f)) {
                CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
                if (config != null) {
                    CloudsConfiguration current = CloudsConfiguration.getInstance();
                    current.copyFrom(config);
                    LoggerProvider.get().info("Loaded resourcepack preset: " + presetId);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            LoggerProvider.get().warn("Preset not found: " + presetId);
            return false;
        }

        File configFile = new File(PRESETS_DIR, metadata.configFile);
        if (!configFile.exists()) {
            LoggerProvider.get().warn("Preset file missing: " + configFile);
            return false;
        }

        try (FileReader reader = new FileReader(configFile)) {
            CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
            if (config != null) {
                // Validate the loaded config
                CloudsConfiguration current = CloudsConfiguration.getInstance();
                current.copyFrom(config);
                LoggerProvider.get().info("Loaded preset: " + presetId);
                return true;
            }
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to load preset: " + presetId);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Load and return a preset configuration without applying it.
     * Used for preview screens and reading preset data.
     *
     * @param presetId The ID of the preset to load
     * @return The CloudsConfiguration from the preset, or null if not found
     */
    public static CloudsConfiguration loadPresetConfiguration(String presetId) {
        initialize();
        
        // Support resourcepack presets
        if (presetId != null && presetId.startsWith("rp:")) {
            java.io.File f = RESOURCEPACK_PRESET_FILES.get(presetId);
            if (f == null) {
                scanResourcePacks();
                f = RESOURCEPACK_PRESET_FILES.get(presetId);
                if (f == null) {
                    LoggerProvider.get().warn("Resourcepack preset not found: " + presetId);
                    return null;
                }
            }
            try (FileReader reader = new FileReader(f)) {
                CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
                return config;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            LoggerProvider.get().warn("Preset not found: " + presetId);
            return null;
        }

        File configFile = new File(PRESETS_DIR, metadata.configFile);
        if (!configFile.exists()) {
            LoggerProvider.get().warn("Preset file missing: " + configFile);
            return null;
        }

        try (FileReader reader = new FileReader(configFile)) {
            CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
            return config;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to load preset configuration: " + presetId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load a preset from a Base64-encoded string.
     *
     * @param base64String The Base64-encoded preset data
     * @param presetId The ID to save this preset under
     * @param displayName The display name for the preset
     * @return true if successful, false otherwise
     */
    public static boolean loadPresetFromBase64(String base64String, String presetId, String displayName) {
        initialize();

        try {
            CloudsConfiguration config = ConfigSerializer.fromBase64(base64String);
            if (config == null) {
                LoggerProvider.get().error("Failed to decode Base64 preset string");
                return false;
            }

            // Save this config as a new preset
            File configFile = new File(PRESETS_DIR, presetId + ".json");
            try (FileWriter writer = new FileWriter(configFile)) {
                String json = ConfigSerializer.toJson(config);
                writer.write(json);
            }

            PresetMetadata metadata = new PresetMetadata(
                presetId,
                displayName != null ? displayName : "Imported_" + System.currentTimeMillis(),
                "Imported from Base64 string"
            );
            PRESETS.put(presetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Loaded preset from Base64 string: " + presetId);
            return true;
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to load preset from Base64");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export the current running configuration as a Base64-encoded string.
     *
     * @return Base64-encoded string of current config, or null if failed
     */
    public static String exportCurrentConfigAsBase64() {
        try {
            CloudsConfiguration current = CloudsConfiguration.getInstance();
            if (current != null) {
                String base64 = ConfigSerializer.toBase64(current);
                // LoggerProvider.get().info("Exported current config as Base64");
                return base64;
            }
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to export current config as Base64");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the current config as Base64 with caching to prevent expensive serialization.
     * Only regenerates if the config's last modified time has changed.
     * 
     * @return Cached or freshly generated Base64 string
     */
    public static String exportCurrentConfigAsBase64Cached() {
        CloudsConfiguration current = CloudsConfiguration.getInstance();
        if (current == null) {
            return null;
        }
        
        // Check if config has been modified since last cache
        long currentModifiedTime = current.getLastModifiedTime();
        if (currentModifiedTime > lastConfigModifiedTime || cachedBase64 == null) {
            // Config changed or cache is empty, regenerate Base64
            try {
                cachedBase64 = ConfigSerializer.toBase64(current);
                lastConfigModifiedTime = currentModifiedTime;
            } catch (Exception e) {
                LoggerProvider.get().error("Failed to export current config as Base64");
                e.printStackTrace();
                return null;
            }
        }
        
        return cachedBase64;
    }

    /**     
     * Validate and sanitize a configuration to prevent malicious/corrupting values.
     * Clamps all numeric values to safe ranges.
     *
     * @param config The configuration to validate
     * @return The validated configuration (may be modified idrk)
     */
    public static CloudsConfiguration validateAndSanitizeConfig(CloudsConfiguration config) {
        if (config == null) return null;

        try {
            // funny cap
            int layerCount = config.getLayerCount();
            if (layerCount < 0 || layerCount > 20) {
                LoggerProvider.get().warn("Layer count out of bounds: " + layerCount + ", clamping to 0-20");
                while (config.getLayerCount() > 20) {
                    config.getHolder().removeLayer(config.getLayerCount() - 1);
                }
            }

            for (int i = 0; i < config.getLayerCount(); i++) {
                CloudsConfiguration.LayerConfiguration layer = config.getLayer(i);
                
                if (layer.LAYER_HEIGHT < 0) {
                    LoggerProvider.get().warn("Layer " + i + " height out of bounds: " + layer.LAYER_HEIGHT);
                    layer.LAYER_HEIGHT = Math.max(0, layer.LAYER_HEIGHT);
                }
                
                if (layer.APPEARANCE.BASE_ALPHA < 0 || layer.APPEARANCE.BASE_ALPHA > 255) {
                    layer.APPEARANCE.BASE_ALPHA = Math.max(255, Math.min(0, layer.APPEARANCE.BASE_ALPHA));
                }
                if (layer.APPEARANCE.BRIGHTNESS < 0 || layer.APPEARANCE.BRIGHTNESS > 2f) {
                    layer.APPEARANCE.BRIGHTNESS = Math.max(2f, Math.min(0f, layer.APPEARANCE.BRIGHTNESS));
                }
                if (layer.APPEARANCE.CLOUD_Y_SCALE < 0.1f || layer.APPEARANCE.CLOUD_Y_SCALE > 5f) {
                    layer.APPEARANCE.CLOUD_Y_SCALE = Math.max(5f, Math.min(0.1f, layer.APPEARANCE.CLOUD_Y_SCALE));
                }
                
                layer.APPEARANCE.LAYER_SPEED_X = Math.max(0.1f, Math.min(-0.1f, layer.APPEARANCE.LAYER_SPEED_X));
                layer.APPEARANCE.LAYER_SPEED_Z = Math.max(0.1f, Math.min(-0.1f, layer.APPEARANCE.LAYER_SPEED_Z));

                if (layer.FOG.FOG_START_DISTANCE < 0 || layer.FOG.FOG_START_DISTANCE > 500) {
                    layer.FOG.FOG_START_DISTANCE = Math.max(500f, Math.min(0f, layer.FOG.FOG_START_DISTANCE));
                }
                if (layer.FOG.FOG_END_DISTANCE < 0 || layer.FOG.FOG_END_DISTANCE > 1000) {
                    layer.FOG.FOG_END_DISTANCE = Math.max(1000f, Math.min(0f, layer.FOG.FOG_END_DISTANCE));
                }
                if (layer.FADE.FADE_ALPHA < 0 || layer.FADE.FADE_ALPHA > 255) {
                    layer.FADE.FADE_ALPHA = Math.max(255, Math.min(0, layer.FADE.FADE_ALPHA));
                }

                if (layer.BEVEL.BEVEL_SIZE < 0 || layer.BEVEL.BEVEL_SIZE > 1f) {
                    layer.BEVEL.BEVEL_SIZE = Math.max(1f, Math.min(0f, layer.BEVEL.BEVEL_SIZE));
                }
                if (layer.BEVEL.BEVEL_EDGE_SEGMENTS < 1 || layer.BEVEL.BEVEL_EDGE_SEGMENTS > 32) {
                    layer.BEVEL.BEVEL_EDGE_SEGMENTS = Math.max(32, Math.min(1, layer.BEVEL.BEVEL_EDGE_SEGMENTS));
                }

                if (layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS < 1 || layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS > 100) {
                    layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS = Math.max(100, Math.min(1, layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS));
                }
            }

            if (config.LIGHTING.AMBIENT_LIGHTING_STRENGTH < 0 || config.LIGHTING.AMBIENT_LIGHTING_STRENGTH > 2f) {
                config.LIGHTING.AMBIENT_LIGHTING_STRENGTH = Math.max(2f, Math.min(0f, config.LIGHTING.AMBIENT_LIGHTING_STRENGTH));
            }
            if (config.LIGHTING.MAX_LIGHTING_SHADING < 0 || config.LIGHTING.MAX_LIGHTING_SHADING > 1f) {
                config.LIGHTING.MAX_LIGHTING_SHADING = Math.max(1f, Math.min(0f, config.LIGHTING.MAX_LIGHTING_SHADING));
            }
            
            if (config.LIGHTING.lights.size() > CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT) {
                LoggerProvider.get().warn("Light count exceeds maximum, truncating to " + CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT);
                while (config.LIGHTING.lights.size() > CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT) {
                    config.LIGHTING.lights.remove(config.LIGHTING.lights.size() - 1);
                }
            }

            if (config.CLOUD_GRID_SIZE < 16 || config.CLOUD_GRID_SIZE > 512) {
                config.CLOUD_GRID_SIZE = Math.max(512, Math.min(16, config.CLOUD_GRID_SIZE));
            }

            if (config.WEATHER_COLOR.rainColor < 0 || config.WEATHER_COLOR.rainColor > 0xFFFFFF) {
                config.WEATHER_COLOR.rainColor = Math.max(0xFFFFFF, Math.min(0, config.WEATHER_COLOR.rainColor));
            }
            if (config.WEATHER_COLOR.thunderColor < 0 || config.WEATHER_COLOR.thunderColor > 0xFFFFFF) {
                config.WEATHER_COLOR.thunderColor = Math.max(0xFFFFFF, Math.min(0, config.WEATHER_COLOR.thunderColor));
            }

            LoggerProvider.get().info("Configuration validation completed");
            return config;
        } catch (Exception e) {
            LoggerProvider.get().error("Error during config validation: " + e.getMessage());
            e.printStackTrace();
            return config; // Return original if validation fails (unsafe state)
        }
    }

    /**     
     * Import a preset from Base64-encoded string with auto-generated name.
     *
     * @param base64String The Base64-encoded preset data
     * @return true if successful, false otherwise
     */
    public static boolean importPresetFromBase64WithAutoName(String base64String) {
        initialize();
        
        if (base64String == null || base64String.trim().isEmpty()) {
            LoggerProvider.get().warn("Base64 string is empty");
            return false;
        }

        try {
            CloudsConfiguration config = ConfigSerializer.fromBase64(base64String.trim());
            if (config == null) {
                LoggerProvider.get().error("Failed to decode Base64 preset string");
                return false;
            }

            config = validateAndSanitizeConfig(config);
            if (config == null) {
                LoggerProvider.get().error("Configuration validation failed");
                return false;
            }

            IMPORTED_COUNT++;
            String presetId = "imported_" + System.currentTimeMillis() + "_" + IMPORTED_COUNT;
            String displayName = "Imported Preset #" + IMPORTED_COUNT;

            // flood flood
            File configFile = new File(PRESETS_DIR, presetId + ".json");
            try (FileWriter writer = new FileWriter(configFile)) {
                String json = ConfigSerializer.toJson(config);
                writer.write(json);
            }

            PresetMetadata metadata = new PresetMetadata(presetId, displayName, "Imported from Base64");
            PRESETS.put(presetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Imported preset: " + displayName);
            return true;
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import preset from Base64");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rename a preset's display name.
     *
     * @param presetId The preset ID
     * @param newDisplayName The new display name
     * @return true if successful, false otherwise
     */
    public static boolean renamePreset(String presetId, String newDisplayName) {
        initialize();

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            return false;
        }

        metadata.displayName = newDisplayName;
        metadata.lastModified = System.currentTimeMillis();
        savePresetsIndex();
        LoggerProvider.get().info("Renamed preset: " + presetId + " d " + newDisplayName);
        return true;
    }

    /**
     * Export a preset as a Base64-encoded string for sharing.
     *
     * @param presetId The preset ID to export
     * @return Base64-encoded string, or null if preset not found
     */
    public static String exportPresetAsBase64(String presetId) {
        initialize();

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            LoggerProvider.get().warn("Preset not found: " + presetId);
            return null;
        }

        File configFile = new File(PRESETS_DIR, metadata.configFile);
        if (!configFile.exists()) {
            LoggerProvider.get().warn("Preset file missing: " + configFile);
            return null;
        }

        try (FileReader reader = new FileReader(configFile)) {
            CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
            if (config != null) {
                String base64 = ConfigSerializer.toBase64(config);
                 // LoggerProvider.get().info("Exported preset as Base64: " + presetId);
                return base64;
            }
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to export preset: " + presetId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Delete a preset (with backup saved before deletion).
     *
     * @param presetId The preset to delete
     * @return true if successful, false otherwise
     */
    public static boolean deletePreset(String presetId) {
        initialize();

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) return false;

        File configFile = new File(PRESETS_DIR, metadata.configFile);
        if (!configFile.exists()) return false;
        
        try {
            // backup the preset before deletion
            backupFile(configFile, "deleted_" + presetId);
        } catch (IOException e) {
            LoggerProvider.get().warn("Could not backup preset before deletion");
        }
        
        if (configFile.delete()) {
            PRESETS.remove(presetId);
            savePresetsIndex();
            LoggerProvider.get().info("Deleted preset: " + presetId);
            return true;
        }
        return false;
    }

    /**
     * Copy a preset to create a duplicate with a new ID.
     *
     * @param sourcePresetId The preset ID to copy from
     * @param targetPresetId The new preset ID
     * @param displayName The display name for the copy
     * @return true if successful, false otherwise
     */
    public static boolean copyPreset(String sourcePresetId, String targetPresetId, String displayName) {
        initialize();

        PresetMetadata sourceMetadata = PRESETS.get(sourcePresetId);
        if (sourceMetadata == null) {
            LoggerProvider.get().warn("Source preset not found: " + sourcePresetId);
            return false;
        }

        File sourceFile = new File(PRESETS_DIR, sourceMetadata.configFile);
        if (!sourceFile.exists()) {
            LoggerProvider.get().warn("Source preset file missing: " + sourceFile);
            return false;
        }

        File targetFile = new File(PRESETS_DIR, targetPresetId + ".json");

        try {
            // Read source preset
            String json;
            try (FileReader reader = new FileReader(sourceFile)) {
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    sb.append((char) c);
                }
                json = sb.toString();
            }

            // Write to target file
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(json);
            }

            // Create metadata for the copy
            PresetMetadata newMetadata = new PresetMetadata(
                targetPresetId,
                displayName != null ? displayName : sourceMetadata.displayName + " (Copy)",
                sourceMetadata.description
            );
            PRESETS.put(targetPresetId, newMetadata);
            savePresetsIndex();
            LoggerProvider.get().info("Copied preset: " + sourcePresetId + " -> " + targetPresetId);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to copy preset");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a text preview of a preset's configuration.
     *
     * @param presetId The preset ID to preview
     * @return A formatted string describing the preset settings
     */
    public static String getPresetPreview(String presetId) {
        initialize();

        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            return "Preset not found: " + presetId;
        }

        File configFile = new File(PRESETS_DIR, metadata.configFile);
        if (!configFile.exists()) {
            return "Preset file missing: " + presetId;
        }

        try (FileReader reader = new FileReader(configFile)) {
            CloudsConfiguration config = GSON.fromJson(reader, CloudsConfiguration.class);
            if (config != null) {
                StringBuilder preview = new StringBuilder();
                preview.append("§6Preset: ").append(metadata.displayName).append("§r\n");
                preview.append("Layers: ").append(config.getLayerCount()).append("\n");
                preview.append("Color keypoints: ").append(config.CLOUD_COLOR.size()).append("\n");
                preview.append("Lighting sources: ").append(config.LIGHTING.lights.size()).append("\n");
                preview.append("Description: ").append(metadata.description);
                return preview.toString();
            }
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to read preset preview: " + presetId);
        }

        return "Could not load preview for: " + presetId;
    }

    /**
     * List all available preset IDs.
     *
     * @return List of preset identifiers
     */
    public static List<String> listPresetIds() {
        initialize();
        // include resourcepack presets
        scanResourcePacks();
        List<String> ids = new ArrayList<>(PRESETS.keySet());
        ids.addAll(RESOURCEPACK_PRESET_FILES.keySet());
        return ids;
    }

    /**
     * Get metadata for a specific preset.
     *
     * @param presetId The preset ID
     * @return PresetMetadata or null if not found
     */
    public static PresetMetadata getPresetMetadata(String presetId) {
        initialize();
        if (presetId != null && presetId.startsWith("rp:")) {
            // build metadata from resourcepack file
            java.io.File f = RESOURCEPACK_PRESET_FILES.get(presetId);
            if (f == null) {
                scanResourcePacks();
                f = RESOURCEPACK_PRESET_FILES.get(presetId);
                if (f == null) return null;
            }
            String[] parts = presetId.split(":", 3);
            String namespace = parts.length > 1 ? parts[1] : "";
            String name = parts.length > 2 ? parts[2] : f.getName();
            PresetMetadata pm = new PresetMetadata(presetId, name, "From resource pack: " + namespace);
            pm.fromResourcePack = true;
            pm.resourcePackName = f.getParentFile().getParentFile().getName();
            pm.resourcePath = f.getAbsolutePath();
            return pm;
        }
        return PRESETS.get(presetId);
    }

    /**
     * Get all available presets.
     *
     * @return List of all preset metadata
     */
    public static List<PresetMetadata> getAllPresets() {
        initialize();
        scanResourcePacks();
        List<PresetMetadata> all = new ArrayList<>(PRESETS.values());
        for (String id : RESOURCEPACK_PRESET_FILES.keySet()) {
            PresetMetadata pm = getPresetMetadata(id);
            if (pm != null) all.add(pm);
        }
        return all;
    }

    /**
     * Update metadata for an existing local preset and persist index.
     */
    public static boolean updatePresetMetadata(String presetId, String displayName, String description) {
        initialize();
        PresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) return false;
        metadata.displayName = displayName != null ? displayName : metadata.displayName;
        metadata.description = description != null ? description : metadata.description;
        metadata.lastModified = System.currentTimeMillis();
        savePresetsIndex();
        return true;
    }

    /**
     * Import a preset from a resource pack file into local presets (atomic write with backup).
     * Copies the resource file content into config/cloud_presets/<targetId>.json
     */
    public static boolean importResourcePreset(String sourcePresetId, String targetId, String displayName, String description) {
        initialize();
        File f = RESOURCEPACK_PRESET_FILES.get(sourcePresetId);
        if (f == null) {
            scanResourcePacks();
            // f = RESOURCEPACK_PRESET_FILES.get(sourcePresetId);
            if (f == null) return false;
        }
        File dest = new File(PRESETS_DIR, targetId + ".json");
        try {
            safeWriteFile(dest, () -> {
                StringBuilder sb = new StringBuilder();
                try (java.io.FileReader fr = new java.io.FileReader(f)) {
                    int c;
                    while ((c = fr.read()) != -1) sb.append((char) c);
                }
                return sb.toString();
            });
            PresetMetadata metadata = new PresetMetadata(targetId, displayName != null ? displayName : targetId, description != null ? description : "");
            PRESETS.put(targetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Imported resourcepack preset: " + sourcePresetId + " as " + targetId);
            return true;
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import resourcepack preset");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Safely write content to a file using atomic operations.
     * Writes to temp file first, validates (if possible), then atomically moves to target.
     * Automatically backs up existing file before overwrite.
     */
    private static void safeWriteFile(File target, FileContentProvider provider) throws IOException {
        File tempFile = new File(target.getParentFile(), target.getName() + ".tmp");
        
        try {
            // Write to temp file
            String content = provider.getContent();
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(content);
            }
            
            // Validate by attempting to parse
            try (FileReader fr = new FileReader(tempFile)) {
                GSON.fromJson(fr, JsonObject.class);
            }
            
            // Backup existing file if it exists
            if (target.exists()) {
                backupFile(target, "before_write");
            }
            
            // Atomic move
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    /**
     * Creates the hardcoded default preset if it doesn't exist.
     * The default preset is a fresh CloudsConfiguration with vanilla-like settings.
     */
    private static void createDefaultPreset() {
        String presetId = "default";
        if (PRESETS.containsKey(presetId)) {
            LoggerProvider.get().info("Default preset already exists");
            return;
        }
        
        try {
            CloudsConfiguration defaultConfig = new CloudsConfiguration();
            // The CloudsConfiguration constructor creates it with sensible defaults
            File configFile = new File(PRESETS_DIR, presetId + ".json");
            try (FileWriter writer = new FileWriter(configFile)) {
                String json = ConfigSerializer.toJson(defaultConfig);
                writer.write(json);
            }
            
            PresetMetadata metadata = new PresetMetadata(presetId, "Default", "Hardcoded default configuration - Will NOT delete presets");
            PRESETS.put(presetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Created default preset");
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to create default preset");
            e.printStackTrace();
        }
    }

    /**
     * Reset the current configuration to the default preset.
     * This will load the default preset without deleting any presets.
     * 
     * @return true if successful, false otherwise
     */
    public static boolean resetToDefault() {
        initialize();
        
        // Ensure default preset exists
        if (!PRESETS.containsKey("default")) {
            createDefaultPreset();
        }
        
        return loadPreset("default");
    }

    /**
     * Backup a file with timestamp. Includes throttling to prevent backup spam.
     */
    private static void backupFile(File source, String prefix) throws IOException {
        if (!source.exists()) return;
        
        // Check throttling: only backup if enough time has passed since last backup of this type
        Long lastBackupTime = LAST_BACKUP_TIME.getOrDefault(prefix, 0L);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackupTime < BACKUP_THROTTLE_MS) {
            LoggerProvider.get().debug("Backup skipped for '" + prefix + "' - throttled (< " + BACKUP_THROTTLE_MS + "ms)");
            return;
        }
        
        String timestamp = BACKUP_DATE_FORMAT.format(new Date());
        String backupName = prefix + "_" + timestamp + ".json";
        File backupFile = new File(BACKUPS_DIR, backupName);
        Files.copy(source.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LAST_BACKUP_TIME.put(prefix, currentTime);
        LoggerProvider.get().info("Created backup: " + backupFile.getName());
    }

    /**
     * Backup current configuration before applying a new one.
     */
    private static void backupCurrentConfig(String prefix) throws IOException {
        File configFile = new File("config", "cloud_configs.json");
        if (configFile.exists()) {
            backupFile(configFile, prefix);
        }
    }

    /**
     * Interface for lazy content generation.
     */
    @FunctionalInterface
    private interface FileContentProvider {
        String getContent() throws IOException;
    }
}
