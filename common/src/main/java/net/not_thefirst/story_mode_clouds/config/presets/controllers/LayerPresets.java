package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPreset;
import net.not_thefirst.story_mode_clouds.config.presets.Preset;

public final class LayerPresets extends BasePresetController<LayerPreset> {
    private static final LayerPresets INSTANCE = new LayerPresets();

    private LayerPresets() {
        super(new File("config/cloud_presets/layers"), new File("config/cloud_presets/backups"), LayerPreset.class);
    }

    public static void initialize() {
        INSTANCE.ensureInitialized();
    }

    public static boolean saveLayerPreset(String presetId, String displayName, String description,
                                          LayerConfiguration layer, String dimension) {
        initialize();
        CloudsConfiguration.Dimension parsedDimension = CloudsConfiguration.Dimension.get(dimension);
        if (parsedDimension == null || layer == null) return false;
        return INSTANCE.save(LayerPreset.fromLayer(presetId, displayName, description, layer, parsedDimension));
    }

    public static LayerPreset loadLayerPreset(String presetId) {
        initialize();
        return INSTANCE.get(presetId);
    }

    public static String exportLayerPresetAsBase64(String presetId) {
        LayerPreset preset = loadLayerPreset(presetId);
        if (preset == null || preset.layer == null) return null;
        JsonObject root = new JsonObject();
        root.add("layer", INSTANCE.gson.toJsonTree(preset.layer));
        return java.util.Base64.getEncoder().encodeToString(
            INSTANCE.gson.toJson(root).getBytes(StandardCharsets.UTF_8));
    }

    public static String exportLayerPresetAsBase64(LayerPreset preset) {
        if (preset == null || preset.layer == null) return null;
        JsonObject root = new JsonObject();
        root.add("layer", INSTANCE.gson.toJsonTree(preset.layer));
        return java.util.Base64.getEncoder().encodeToString(
            INSTANCE.gson.toJson(root).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean importLayerPresetFromBase64(String presetId, String displayName, String description,
                                                       String base64Data, String dimension) {
        return importLayerPresetFromPayload(presetId, displayName, description, base64Data, dimension);
    }

    public static boolean importLayerPresetFromPayload(String presetId, String displayName, String description,
                                                       String payload, String dimension) {
        initialize();
        try {
            String json = Preset.decodePayload(payload);
            if (json == null) return false;
            JsonElement parsed = JsonParser.parseString(json);
            JsonElement layerElement = parsed.isJsonObject() && parsed.getAsJsonObject().has("layer")
                ? parsed.getAsJsonObject().get("layer") : parsed;
            LayerConfiguration layer = INSTANCE.gson.fromJson(layerElement, LayerConfiguration.class);
            CloudsConfiguration.Dimension parsedDimension = CloudsConfiguration.Dimension.get(dimension);
            if (parsedDimension == null || !validateLayerConfiguration(layer)) return false;
            return INSTANCE.save(LayerPreset.fromLayer(presetId, displayName, description, layer, parsedDimension));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateLayerConfiguration(LayerConfiguration layer) {
        if (layer == null || layer.NAME == null || layer.NAME.isBlank()) return false;
        if (layer.LAYER_HEIGHT < 0) return false;
        if (layer.FADE != null) {
            if (layer.FADE.FADE_ALPHA < 0 || layer.FADE.FADE_ALPHA > 255) return false;
            if (layer.FADE.TRANSITION_RANGE < 0) return false;
        }
        return true;
    }

    public static boolean validateLayerPresetPayload(String payload) {
        try {
            String json = Preset.decodePayload(payload);
            if (json == null) return false;
            JsonElement parsed = JsonParser.parseString(json);
            JsonElement layerElement = parsed.isJsonObject() && parsed.getAsJsonObject().has("layer")
                ? parsed.getAsJsonObject().get("layer") : parsed;
            return validateLayerConfiguration(INSTANCE.gson.fromJson(layerElement, LayerConfiguration.class));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean deleteLayerPreset(String presetId) {
        initialize();
        return INSTANCE.delete(presetId);
    }

    public static Map<String, LayerPresetMetadata> getAllPresets() {
        initialize();
        Map<String, LayerPresetMetadata> result = new HashMap<>();
        for (BasePresetController.PresetMetadata entry : INSTANCE.metadataEntries().values()) {
            result.put(entry.id, (LayerPresetMetadata) entry);
        }
        return result;
    }

    public static List<LayerPresetMetadata> getPresetsForDimension(String dimension) {
        initialize();
        return getAllPresets().values().stream()
            .filter(preset -> dimension.equals(preset.dimension)).toList();
    }

    public static LayerPresetMetadata getPresetMetadata(String presetId) {
        initialize();
        return (LayerPresetMetadata) INSTANCE.metadataEntries().get(presetId);
    }

    @Override
    protected PresetMetadata createMetadata(LayerPreset preset) {
        return new LayerPresetMetadata(preset.getId(), preset.getDisplayName(), preset.getDescription(),
                                       preset.dimension == null ? null : preset.dimension.getId(), preset.getLastModified());
    }

    @Override
    protected PresetMetadata readMetadata(JsonObject object) {
        return gson.fromJson(object, LayerPresetMetadata.class);
    }

    @Override
    protected LayerPreset readPreset(FileReader reader, PresetMetadata entry) {
        JsonElement element = JsonParser.parseReader(reader);
        if (element.isJsonObject() && element.getAsJsonObject().has("layer")) {
            LayerPreset preset = gson.fromJson(element, LayerPreset.class);
            if (preset != null && preset.layer != null) preset.layer.refreshCustomTypeConfig();
            return preset;
        }
        LayerConfiguration layer = gson.fromJson(element, LayerConfiguration.class);
        LayerPresetMetadata metadata = (LayerPresetMetadata) entry;
        CloudsConfiguration.Dimension dimension = CloudsConfiguration.Dimension.get(metadata.dimension);
        return dimension == null ? null : LayerPreset.fromLayer(metadata.id, metadata.displayName,
            metadata.description, layer, dimension);
    }

    @Override
    protected void setMetadata(LayerPreset preset, PresetMetadata metadata) {
        LayerPresetMetadata layerMetadata = (LayerPresetMetadata) metadata;
        preset.id = layerMetadata.id;
        preset.displayName = layerMetadata.displayName;
        preset.description = layerMetadata.description;
        preset.lastModified = layerMetadata.lastModified;
        preset.dimension = CloudsConfiguration.Dimension.get(layerMetadata.dimension);
    }

    @Override
    protected boolean isValid(LayerPreset preset) {
        return preset != null && preset.dimension != null && validateLayerConfiguration(preset.layer);
    }

    @Override
    protected LayerPreset createEmptyPreset() { return new LayerPreset(); }

    public static class LayerPresetMetadata extends PresetMetadata {
        public String dimension;

        public LayerPresetMetadata(String id, String displayName, String description, String dimension) {
            this(id, displayName, description, dimension, System.currentTimeMillis());
        }

        public LayerPresetMetadata(String id, String displayName, String description, String dimension, long lastModified) {
            super(id, displayName, description, lastModified);
            this.dimension = dimension;
        }

        public LayerPresetMetadata() {}
    }
}
