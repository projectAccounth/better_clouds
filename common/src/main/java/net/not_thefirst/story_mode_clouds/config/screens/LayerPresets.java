package net.not_thefirst.story_mode_clouds.config.screens;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

public class LayerPresets {
    private LayerPresets() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File LAYER_PRESETS_DIR = new File("config/cloud_presets/layers");
    private static final File LAYER_PRESETS_INDEX = new File(LAYER_PRESETS_DIR, "presets.json");
    
    private static final Map<String, LayerPresetMetadata> PRESETS = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Metadata about a layer preset.
     */
    public static class LayerPresetMetadata {
        public String id;
        public String displayName;
        public String description;
        public long lastModified;
        public String configFile;
        public String dimension; // OVERWORLD, NETHER, END

        public LayerPresetMetadata(String id, String displayName, String description, String dimension) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.dimension = dimension;
            this.lastModified = System.currentTimeMillis();
            this.configFile = id + ".json";
        }
    }

    /**
     * Initialize the layer presets system.
     */
    public static void initialize() {
        if (initialized) return;
        LAYER_PRESETS_DIR.mkdirs();
        loadPresetsIndex();
        initialized = true;
    }

    /**
     * Load presets index from disk.
     */
    private static void loadPresetsIndex() {
        if (!LAYER_PRESETS_INDEX.exists()) {
            LoggerProvider.get().info("No layer presets index found, creating fresh");
            return;
        }

        try (FileReader reader = new FileReader(LAYER_PRESETS_INDEX)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("presets")) {
                LayerPresetMetadata[] presets = GSON.fromJson(
                    json.getAsJsonArray("presets"),
                    LayerPresetMetadata[].class
                );
                for (LayerPresetMetadata preset : presets) {
                    PRESETS.put(preset.id, preset);
                }
                LoggerProvider.get().info("Loaded {} layer presets", PRESETS.size());
            }
        } catch (IOException e) {
            LoggerProvider.get().warn("Failed to load layer presets index: {}", e.getMessage());
        }
    }

    /**
     * Save presets index to disk.
     */
    private static void savePresetsIndex() {
        try {
            LAYER_PRESETS_DIR.mkdirs();
            try (FileWriter writer = new FileWriter(LAYER_PRESETS_INDEX)) {
                JsonObject json = new JsonObject();
                json.add("presets", GSON.toJsonTree(PRESETS.values().toArray(new LayerPresetMetadata[0])));
                writer.write(GSON.toJson(json));
            }
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save layer presets index: {}", e.getMessage());
        }
    }

    /**
     * Save a layer configuration as a preset.
     *
     * @param presetId Unique identifier for the preset
     * @param displayName User-friendly name
     * @param description Short description
     * @param layer The layer configuration to save
     * @param dimension The dimension this preset is for
     * @return true if successful
     */
    public static boolean saveLayerPreset(String presetId, String displayName, String description,
                                          LayerConfiguration layer, String dimension) {
        initialize();
        try {
            LayerPresetMetadata metadata = new LayerPresetMetadata(presetId, displayName, description, dimension);
            File presetFile = new File(LAYER_PRESETS_DIR, metadata.configFile);

            try (FileWriter writer = new FileWriter(presetFile)) {
                String json = GSON.toJson(layer);
                writer.write(json);
            }

            PRESETS.put(presetId, metadata);
            savePresetsIndex();
            LoggerProvider.get().info("Saved layer preset: {} ({})", presetId, dimension);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save layer preset: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Load a layer preset.
     *
     * @param presetId The preset to load
     * @return The layer configuration, or null if not found
     */
    public static LayerConfiguration loadLayerPreset(String presetId) {
        initialize();
        LayerPresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            LoggerProvider.get().warn("Layer preset not found: {}", presetId);
            return null;
        }

        File presetFile = new File(LAYER_PRESETS_DIR, metadata.configFile);
        if (!presetFile.exists()) {
            LoggerProvider.get().warn("Layer preset file missing: {}", presetFile);
            return null;
        }

        try (FileReader reader = new FileReader(presetFile)) {
            LayerConfiguration layer = GSON.fromJson(reader, LayerConfiguration.class);
            if (layer != null) {
                LoggerProvider.get().info("Loaded layer preset: {}", presetId);
                return layer;
            }
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to load layer preset: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Export a layer preset as Base64 string for sharing.
     *
     * @param presetId The preset to export
     * @return Base64 encoded string, or null if not found
     */
    public static String exportLayerPresetAsBase64(String presetId) {
        initialize();
        LayerConfiguration layer = loadLayerPreset(presetId);
        if (layer == null) return null;

        try {
            String json = GSON.toJson(layer);
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to export layer preset as Base64: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Import a layer preset from Base64 string.
     *
     * @param presetId Unique identifier for the new preset
     * @param displayName User-friendly name
     * @param description Short description
     * @param base64Data Base64 encoded layer configuration
     * @param dimension The dimension this preset is for
     * @return true if successful
     */
    public static boolean importLayerPresetFromBase64(String presetId, String displayName, String description,
                                                       String base64Data, String dimension) {
        initialize();
        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
            String json = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            LayerConfiguration layer = GSON.fromJson(json, LayerConfiguration.class);

            if (layer != null) {
                return saveLayerPreset(presetId, displayName, description, layer, dimension);
            }
        } catch (IllegalArgumentException e) {
            LoggerProvider.get().error("Invalid Base64 data for layer preset import: {}", e.getMessage());
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import layer preset from Base64: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Delete a layer preset.
     *
     * @param presetId The preset to delete
     * @return true if successful
     */
    public static boolean deleteLayerPreset(String presetId) {
        initialize();
        LayerPresetMetadata metadata = PRESETS.get(presetId);
        if (metadata == null) {
            LoggerProvider.get().warn("Layer preset not found: {}", presetId);
            return false;
        }

        File presetFile = new File(LAYER_PRESETS_DIR, metadata.configFile);
        try {
            Files.delete(Path.of(LAYER_PRESETS_DIR.getPath(), metadata.configFile));
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to delete layer preset file: {}, error: {}", presetFile, e.getMessage());
            return false;
        }

        PRESETS.remove(presetId);
        savePresetsIndex();
        LoggerProvider.get().info("Deleted layer preset: {}", presetId);
        return true;
    }

    /**
     * Get all available layer presets.
     *
     * @return Map of preset ID to metadata
     */
    public static Map<String, LayerPresetMetadata> getAllPresets() {
        initialize();
        return new HashMap<>(PRESETS);
    }

    /**
     * Get presets for a specific dimension.
     *
     * @param dimension The dimension to filter by
     * @return List of preset metadata for that dimension
     */
    public static List<LayerPresetMetadata> getPresetsForDimension(String dimension) {
        initialize();
        List<LayerPresetMetadata> result = new ArrayList<>();
        for (LayerPresetMetadata metadata : PRESETS.values()) {
            if (dimension.equals(metadata.dimension)) {
                result.add(metadata);
            }
        }
        return result;
    }

    /**
     * Get a specific preset metadata.
     *
     * @param presetId The preset ID
     * @return The metadata, or null if not found
     */
    public static LayerPresetMetadata getPresetMetadata(String presetId) {
        initialize();
        return PRESETS.get(presetId);
    }
}
