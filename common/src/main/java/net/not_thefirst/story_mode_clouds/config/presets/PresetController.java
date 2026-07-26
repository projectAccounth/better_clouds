package net.not_thefirst.story_mode_clouds.config.presets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.resources.ConfigSerializer;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

public class PresetController {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    
    @SuppressWarnings("unused")
    private static final long BACKUP_THROTTLE_MS = 5000;
    
    private static final File PRESETS_DIR = new File("config/cloud_presets");
    private static final File BACKUPS_DIR = new File(PRESETS_DIR, "backups");
    
    private static final File COLORS_DIR = new File(PRESETS_DIR, "colors");
    private static final File LIGHTING_DIR = new File(PRESETS_DIR, "lighting");
    private static final File LIGHT_SOURCES_DIR = new File(PRESETS_DIR, "light_sources");
    
    private static final File COLORS_INDEX = new File(COLORS_DIR, "presets.json");
    private static final File LIGHTING_INDEX = new File(LIGHTING_DIR, "presets.json");
    private static final File LIGHT_SOURCES_INDEX = new File(LIGHT_SOURCES_DIR, "presets.json");
    
    // In memory storage
    private static final Map<String, CloudColorPreset> COLOR_PRESETS = new HashMap<>();
    private static final Map<String, LightingPreset> LIGHTING_PRESETS = new HashMap<>();
    private static final Map<String, LightSourcesPreset> LIGHT_SOURCES_PRESETS = new HashMap<>();
    
    private static boolean initialized = false;

    @SuppressWarnings("unused")
    private static final Map<String, Long> LAST_BACKUP_TIME = new HashMap<>();
    
    public enum PresetCategory {
        COLORS("colors"),
        LIGHTING("lighting"),
        LIGHT_SOURCES("light_sources");
        
        public final String dirName;
        PresetCategory(String dirName) {
            this.dirName = dirName;
        }
    }

    /**
     * Initialize the preset controller and load all category presets
     */
    public static void initialize() {
        if (initialized) return;
        
        try {
            // Create all necessary directories
            PRESETS_DIR.mkdirs();
            BACKUPS_DIR.mkdirs();
            COLORS_DIR.mkdirs();
            LIGHTING_DIR.mkdirs();
            LIGHT_SOURCES_DIR.mkdirs();
            
            // Load presets for each category
            loadColorPresets();
            loadLightingPresets();
            loadLightSourcesPresets();
            
            initialized = true;
            
            createDefaultPresets();
            
            LoggerProvider.get().info("Preset controller initialized successfully");
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to initialize preset controller: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // colors
    
    public static boolean saveColorPreset(String presetId, String displayName, String description, CloudsConfiguration config) {
        ensure();
        CloudColorPreset preset = CloudColorPreset.fromConfiguration(presetId, displayName, description, config);
        return saveColorPresetDirect(preset);
    }

    public static boolean saveColorPresetDirect(CloudColorPreset preset) {
        ensure();
        if (preset.colors != null) {
            preset.sortColorsByTime();
        }
        preset.lastModified = System.currentTimeMillis();
        File presetFile = new File(COLORS_DIR, preset.id + ".json");
        
        try {
            safeWriteFile(presetFile, () -> GSON.toJson(preset));
            COLOR_PRESETS.put(preset.id, preset);
            saveColorPresetsIndex();
            LoggerProvider.get().info("Saved color preset: {}", preset.id);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save color preset {}: {}", preset.id, e.getMessage());
            return false;
        }
    }

    public static String exportColorPresetAsBase64(String presetId) {
        ensure();
        CloudColorPreset preset = COLOR_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Color preset not found for export: {}", presetId);
            return null;
        }

        try {
            String json = GSON.toJson(preset);
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to export color preset as Base64: {}", e.getMessage());
            return null;
        }
    }

    public static boolean importColorPresetFromPayload(String presetId, String displayName, String description, String payload) {
        ensure();
        try {
            if (!new CloudColorPreset().validate(payload)) {
                LoggerProvider.get().error("Color preset payload failed validation: {}", presetId);
                return false;
            }

            String json = decodePresetPayload(payload);
            if (json == null) return false;

            CloudColorPreset preset = GSON.fromJson(json, CloudColorPreset.class);
            if (preset == null) {
                LoggerProvider.get().error("Failed to parse color preset payload");
                return false;
            }

            preset.id = presetId;
            preset.displayName = displayName;
            preset.description = description;
            preset.lastModified = System.currentTimeMillis();
            preset.sortColorsByTime();

            if (!preset.isValid()) {
                LoggerProvider.get().error("Invalid color preset payload: {}", presetId);
                return false;
            }

            return saveColorPresetDirect(preset);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import color preset payload: {}", e.getMessage());
            return false;
        }
    }

    public static String exportLightingPresetAsBase64(String presetId) {
        ensure();
        LightingPreset preset = LIGHTING_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Lighting preset not found for export: {}", presetId);
            return null;
        }

        try {
            String json = GSON.toJson(preset);
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to export lighting preset as Base64: {}", e.getMessage());
            return null;
        }
    }

    public static boolean importLightingPresetFromPayload(String presetId, String displayName, String description, String payload) {
        ensure();
        try {
            if (!new LightingPreset().validate(payload)) {
                LoggerProvider.get().error("Lighting preset payload failed validation: {}", presetId);
                return false;
            }

            String json = decodePresetPayload(payload);
            if (json == null) return false;

            LightingPreset preset = GSON.fromJson(json, LightingPreset.class);
            if (preset == null) {
                LoggerProvider.get().error("Failed to parse lighting preset payload");
                return false;
            }

            preset.id = presetId;
            preset.displayName = displayName;
            preset.description = description;
            preset.lastModified = System.currentTimeMillis();

            if (!preset.isValid()) {
                LoggerProvider.get().error("Invalid lighting preset payload: {}", presetId);
                return false;
            }

            return saveLightingPresetDirect(preset);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import lighting preset payload: {}", e.getMessage());
            return false;
        }
    }

    public static String exportLightSourcesPresetAsBase64(String presetId) {
        ensure();
        LightSourcesPreset preset = LIGHT_SOURCES_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Light sources preset not found for export: {}", presetId);
            return null;
        }

        try {
            String json = GSON.toJson(preset);
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to export light sources preset as Base64: {}", e.getMessage());
            return null;
        }
    }

    public static boolean importLightSourcesPresetFromPayload(String presetId, String displayName, String description, String payload) {
        ensure();
        try {
            if (!new LightSourcesPreset().validate(payload)) {
                LoggerProvider.get().error("Light sources preset payload failed validation: {}", presetId);
                return false;
            }

            String json = decodePresetPayload(payload);
            if (json == null) return false;

            LightSourcesPreset preset = GSON.fromJson(json, LightSourcesPreset.class);
            if (preset == null) {
                LoggerProvider.get().error("Failed to parse light sources preset payload");
                return false;
            }

            preset.id = presetId;
            preset.displayName = displayName;
            preset.description = description;
            preset.lastModified = System.currentTimeMillis();

            if (!preset.isValid()) {
                LoggerProvider.get().error("Invalid light sources preset payload: {}", presetId);
                return false;
            }

            return saveLightSourcesPresetDirect(preset);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import light sources preset payload: {}", e.getMessage());
            return false;
        }
    }

    private static String decodePresetPayload(String payload) {
        if (payload == null) return null;
        String trimmed = payload.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            if (!isValidJson(trimmed)) {
                LoggerProvider.get().error("Invalid JSON preset payload");
                return null;
            }
            return trimmed;
        }

        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(trimmed);
            String decoded = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (!isValidJson(decoded)) {
                LoggerProvider.get().error("Decoded Base64 payload is not valid JSON");
                return null;
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            LoggerProvider.get().error("Failed to decode preset payload as Base64: {}", e.getMessage());
            return null;
        }
    }

    private static boolean isValidJson(String json) {
        try {
            var element = JsonParser.parseString(json);
            return element.isJsonObject();
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private static boolean validateColorPresetPayload(String json) {
        try {
            var obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("colors") || !obj.get("colors").isJsonArray()) return false;
            if (!obj.has("colorMode")) return false;
            return obj.getAsJsonArray("colors").size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateLightingPresetPayload(String json) {
        try {
            var obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("ambientStrength")
                && obj.has("maxShadingStrength")
                && obj.has("shadingMode")
                && obj.has("lightingType")
                && obj.has("dayStart")
                && obj.has("dayEnd")
                && obj.has("dayNoon");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateLightSourcesPresetPayload(String json) {
        try {
            var obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("lights") || !obj.get("lights").isJsonArray()) return false;
            return obj.getAsJsonArray("lights").size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean loadColorPreset(String presetId, CloudsConfiguration config) {
        ensure();
        CloudColorPreset preset = COLOR_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Color preset not found: {}", presetId);
            return false;
        }
        
        backupCurrentConfig("color_preset_load");
        preset.applyTo(config);
        return true;
    }

    public static CloudColorPreset getColorPreset(String presetId) {
        ensure();
        return COLOR_PRESETS.get(presetId);
    }

    public static List<CloudColorPreset> listColorPresets() {
        ensure();
        return new ArrayList<>(COLOR_PRESETS.values());
    }

    public static boolean deleteColorPreset(String presetId) {
        ensure();
        if (!COLOR_PRESETS.containsKey(presetId)) {
            return false;
        }
        
        File presetFile = new File(COLORS_DIR, presetId + ".json");
        try {
            backupFile(presetFile, "color_preset_delete");
            Files.deleteIfExists(presetFile.toPath());
            COLOR_PRESETS.remove(presetId);
            saveColorPresetsIndex();
            LoggerProvider.get().info("Deleted color preset: {}", presetId);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to delete color preset: {}", e.getMessage());
            return false;
        }
    }

    private static void loadColorPresets() throws IOException {
        COLOR_PRESETS.clear();
        
        if (!COLORS_INDEX.exists()) {
            LoggerProvider.get().info("No color presets index found");
            return;
        }
        
        try (FileReader reader = new FileReader(COLORS_INDEX)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("presets")) {
                CloudColorPreset[] presets = GSON.fromJson(
                    json.getAsJsonArray("presets"),
                    CloudColorPreset[].class
                );
                for (CloudColorPreset preset : presets) {
                    COLOR_PRESETS.put(preset.id, preset);
                }
                LoggerProvider.get().info("Loaded {} color presets", COLOR_PRESETS.size());
            }
        }
    }

    private static void saveColorPresetsIndex() {
        try {
            safeWriteFile(COLORS_INDEX, () -> {
                JsonObject json = new JsonObject();
                json.add("presets", GSON.toJsonTree(COLOR_PRESETS.values().toArray(new CloudColorPreset[0])));
                return GSON.toJson(json);
            });
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save color presets index: {}", e.getMessage());
        }
    }

    // lighitng settings

    public static boolean saveLightingPreset(String presetId, String displayName, String description, CloudsConfiguration config) {
        ensure();
        LightingPreset preset = LightingPreset.fromConfiguration(presetId, displayName, description, config);
        return saveLightingPresetDirect(preset);
    }

    public static boolean saveLightingPresetDirect(LightingPreset preset) {
        ensure();
        preset.lastModified = System.currentTimeMillis();
        File presetFile = new File(LIGHTING_DIR, preset.id + ".json");
        
        try {
            safeWriteFile(presetFile, () -> GSON.toJson(preset));
            LIGHTING_PRESETS.put(preset.id, preset);
            saveLightingPresetsIndex();
            LoggerProvider.get().info("Saved lighting preset: {}", preset.id);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save lighting preset {}: {}", preset.id, e.getMessage());
            return false;
        }
    }

    public static boolean loadLightingPreset(String presetId, CloudsConfiguration config) {
        ensure();
        LightingPreset preset = LIGHTING_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Lighting preset not found: {}", presetId);
            return false;
        }
        
        backupCurrentConfig("lighting_preset_load");
        preset.applyTo(config);
        return true;
    }

    public static LightingPreset getLightingPreset(String presetId) {
        ensure();
        return LIGHTING_PRESETS.get(presetId);
    }

    public static List<LightingPreset> listLightingPresets() {
        ensure();
        return new ArrayList<>(LIGHTING_PRESETS.values());
    }

    public static boolean deleteLightingPreset(String presetId) {
        ensure();
        if (!LIGHTING_PRESETS.containsKey(presetId)) {
            return false;
        }
        
        File presetFile = new File(LIGHTING_DIR, presetId + ".json");
        try {
            backupFile(presetFile, "lighting_preset_delete");
            Files.deleteIfExists(presetFile.toPath());
            LIGHTING_PRESETS.remove(presetId);
            saveLightingPresetsIndex();
            LoggerProvider.get().info("Deleted lighting preset: {}", presetId);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to delete lighting preset: {}", e.getMessage());
            return false;
        }
    }

    private static void loadLightingPresets() throws IOException {
        LIGHTING_PRESETS.clear();
        
        if (!LIGHTING_INDEX.exists()) {
            LoggerProvider.get().info("No lighting presets index found");
            return;
        }
        
        try (FileReader reader = new FileReader(LIGHTING_INDEX)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("presets")) {
                LightingPreset[] presets = GSON.fromJson(
                    json.getAsJsonArray("presets"),
                    LightingPreset[].class
                );
                for (LightingPreset preset : presets) {
                    LIGHTING_PRESETS.put(preset.id, preset);
                }
                LoggerProvider.get().info("Loaded {} lighting presets", LIGHTING_PRESETS.size());
            }
        }
    }

    private static void saveLightingPresetsIndex() {
        try {
            safeWriteFile(LIGHTING_INDEX, () -> {
                JsonObject json = new JsonObject();
                json.add("presets", GSON.toJsonTree(LIGHTING_PRESETS.values().toArray(new LightingPreset[0])));
                return GSON.toJson(json);
            });
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save lighting presets index: {}", e.getMessage());
        }
    }

    // light sources

    public static boolean saveLightSourcesPreset(String presetId, String displayName, String description, CloudsConfiguration config) {
        ensure();
        LightSourcesPreset preset = LightSourcesPreset.fromConfiguration(presetId, displayName, description, config);
        return saveLightSourcesPresetDirect(preset);
    }

    public static boolean saveLightSourcesPresetDirect(LightSourcesPreset preset) {
        ensure();
        preset.lastModified = System.currentTimeMillis();
        File presetFile = new File(LIGHT_SOURCES_DIR, preset.id + ".json");
        
        try {
            safeWriteFile(presetFile, () -> GSON.toJson(preset));
            LIGHT_SOURCES_PRESETS.put(preset.id, preset);
            saveLightSourcesPresetsIndex();
            LoggerProvider.get().info("Saved light sources preset: {}", preset.id);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save light sources preset {}: {}", preset.id, e.getMessage());
            return false;
        }
    }

    public static boolean loadLightSourcesPreset(String presetId, CloudsConfiguration config) {
        ensure();
        LightSourcesPreset preset = LIGHT_SOURCES_PRESETS.get(presetId);
        if (preset == null) {
            LoggerProvider.get().warn("Light sources preset not found: {}", presetId);
            return false;
        }
        
        backupCurrentConfig("light_sources_preset_load");
        preset.applyTo(config);
        return true;
    }

    public static LightSourcesPreset getLightSourcesPreset(String presetId) {
        ensure();
        return LIGHT_SOURCES_PRESETS.get(presetId);
    }

    public static List<LightSourcesPreset> listLightSourcesPresets() {
        ensure();
        return new ArrayList<>(LIGHT_SOURCES_PRESETS.values());
    }

    public static boolean deleteLightSourcesPreset(String presetId) {
        ensure();
        if (!LIGHT_SOURCES_PRESETS.containsKey(presetId)) {
            return false;
        }
        
        File presetFile = new File(LIGHT_SOURCES_DIR, presetId + ".json");
        try {
            backupFile(presetFile, "light_sources_preset_delete");
            Files.deleteIfExists(presetFile.toPath());
            LIGHT_SOURCES_PRESETS.remove(presetId);
            saveLightSourcesPresetsIndex();
            LoggerProvider.get().info("Deleted light sources preset: {}", presetId);
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to delete light sources preset: {}", e.getMessage());
            return false;
        }
    }

    private static void loadLightSourcesPresets() throws IOException {
        LIGHT_SOURCES_PRESETS.clear();
        
        if (!LIGHT_SOURCES_INDEX.exists()) {
            LoggerProvider.get().info("No light sources presets index found");
            return;
        }
        
        try (FileReader reader = new FileReader(LIGHT_SOURCES_INDEX)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("presets")) {
                LightSourcesPreset[] presets = GSON.fromJson(
                    json.getAsJsonArray("presets"),
                    LightSourcesPreset[].class
                );
                for (LightSourcesPreset preset : presets) {
                    LIGHT_SOURCES_PRESETS.put(preset.id, preset);
                }
                LoggerProvider.get().info("Loaded {} light sources presets", LIGHT_SOURCES_PRESETS.size());
            }
        }
    }

    private static void saveLightSourcesPresetsIndex() {
        try {
            safeWriteFile(LIGHT_SOURCES_INDEX, () -> {
                JsonObject json = new JsonObject();
                json.add("presets", GSON.toJsonTree(LIGHT_SOURCES_PRESETS.values().toArray(new LightSourcesPreset[0])));
                return GSON.toJson(json);
            });
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save light sources presets index: {}", e.getMessage());
        }
    }

    /**
     * Ensure controller is initialized
     */
    private static void ensure() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * Create default presets for each category if they don't exist
     */
    private static void createDefaultPresets() {
        // Create default color preset
        if (!COLOR_PRESETS.containsKey("default")) {
            CloudColorPreset defaultColor = new CloudColorPreset("default", "Default Colors", "Default cloud color progression");
            defaultColor.colors = new ArrayList<>(CloudsConfiguration.DEFAULT_COLORS);
            defaultColor.colorMode = CloudsConfiguration.CloudColorProviderMode.VANILLA;
            saveColorPresetDirect(defaultColor);
        }
        
        if (!LIGHTING_PRESETS.containsKey("default")) {
            LightingPreset defaultLighting = new LightingPreset("default", "Default Lighting", "Default lighting parameters");
            saveLightingPresetDirect(defaultLighting);
        }
        
        if (!LIGHT_SOURCES_PRESETS.containsKey("default")) {
            CloudsConfiguration config = CloudsConfiguration.getInstance();
            LightSourcesPreset defaultLights = LightSourcesPreset.fromConfiguration("default", "Default Light Sources", "Default light source configuration", config);
            saveLightSourcesPresetDirect(defaultLights);
        }
    }

    /**
     * Safely write content to a file using atomic operations
     */
    private static void safeWriteFile(File target, FileContentProvider provider) throws IOException {
        File tempFile = new File(target.getParentFile(), target.getName() + ".tmp");
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(provider.getContent());
        }
        
        Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Backup a file with timestamp
     */
    private static void backupFile(File source, String prefix) throws IOException {
        String timestamp = BACKUP_DATE_FORMAT.format(LocalDateTime.now());
        String backupName = prefix + "_" + timestamp + ".json";
        File backupFile = new File(BACKUPS_DIR, backupName);
        
        if (source.exists()) {
            Files.copy(source.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Backup current configuration
     */
    private static void backupCurrentConfig(String prefix) {
        String timestamp = BACKUP_DATE_FORMAT.format(LocalDateTime.now());
        String backupName = prefix + "_" + timestamp + ".json";
        File backupFile = new File(BACKUPS_DIR, backupName);
        
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        try (FileWriter writer = new FileWriter(backupFile)) {
            writer.write(ConfigSerializer.toJson(config));
        }
        catch (IOException e) {
            LoggerProvider.get().error("Failed to backup current configuration: {}", e.getMessage());
        }
    }

    /**
     * Interface for lazy content generation
     */
    @FunctionalInterface
    private interface FileContentProvider {
        String getContent() throws IOException;
    }

    /**
     * Get total count of all presets across all categories
     */
    public static int getTotalPresetCount() {
        ensure();
        return COLOR_PRESETS.size() + LIGHTING_PRESETS.size() + LIGHT_SOURCES_PRESETS.size();
    }

    /**
     * Get count for a specific category
     */
    public static boolean validatePresetPayload(PresetCategory category, String payload) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        return switch (category) {
            case COLORS -> new CloudColorPreset().validate(payload);
            case LIGHTING -> new LightingPreset().validate(payload);
            case LIGHT_SOURCES -> new LightSourcesPreset().validate(payload);
        };
    }

    public static int getPresetCount(PresetCategory category) {
        ensure();
        return switch (category) {
            case COLORS -> COLOR_PRESETS.size();
            case LIGHTING -> LIGHTING_PRESETS.size();
            case LIGHT_SOURCES -> LIGHT_SOURCES_PRESETS.size();
        };
    }

    /**
     * Update preset metadata (name and description) without changing the preset data.
     */
    public static boolean updatePresetMetadata(PresetCategory category, String presetId, String displayName, String description) {
        ensure();
        try {
            switch (category) {
                case COLORS:
                    CloudColorPreset colorPreset = COLOR_PRESETS.get(presetId);
                    if (colorPreset != null) {
                        colorPreset.displayName = displayName;
                        colorPreset.description = description;
                        colorPreset.lastModified = System.currentTimeMillis();
                        return saveColorPresetDirect(colorPreset);
                    }
                    break;
                case LIGHTING:
                    LightingPreset lightingPreset = LIGHTING_PRESETS.get(presetId);
                    if (lightingPreset != null) {
                        lightingPreset.displayName = displayName;
                        lightingPreset.description = description;
                        lightingPreset.lastModified = System.currentTimeMillis();
                        return saveLightingPresetDirect(lightingPreset);
                    }
                    break;
                case LIGHT_SOURCES:
                    LightSourcesPreset lightSourcesPreset = LIGHT_SOURCES_PRESETS.get(presetId);
                    if (lightSourcesPreset != null) {
                        lightSourcesPreset.displayName = displayName;
                        lightSourcesPreset.description = description;
                        lightSourcesPreset.lastModified = System.currentTimeMillis();
                        return saveLightSourcesPresetDirect(lightSourcesPreset);
                    }
                    break;
            }
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to update preset metadata: {}", e.getMessage());
        }
        return false;
    }
}
