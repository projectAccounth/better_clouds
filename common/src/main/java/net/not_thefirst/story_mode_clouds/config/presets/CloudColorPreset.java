package net.not_thefirst.story_mode_clouds.config.presets;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * Represents a cloud color preset containing time-based color keypoints.
 * This is a category preset that only stores cloud color settings.
 */
public class CloudColorPreset implements Preset {
    public String id;
    public String displayName;
    public String description;
    public long lastModified;
    
    public List<CloudsConfiguration.SkyColorKeypoint> colors;
    public CloudsConfiguration.CloudColorProviderMode colorMode;

    public CloudColorPreset(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.lastModified = System.currentTimeMillis();
        this.colors = new ArrayList<>();
        this.colorMode = CloudsConfiguration.CloudColorProviderMode.VANILLA;
    }

    public CloudColorPreset() {
        this("", "", "");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Create a preset from current configuration
     */
    public static CloudColorPreset fromConfiguration(String id, String displayName, String description, CloudsConfiguration config) {
        CloudColorPreset preset = new CloudColorPreset(id, displayName, description);
        if (config.CLOUD_COLOR != null) {
            preset.colors = new ArrayList<>(config.CLOUD_COLOR);
        }
        preset.colorMode = config.COLOR_MODE;
        return preset;
    }

    /**
     * Apply this preset's settings to the given configuration
     */
    @Override
    public void applyTo(CloudsConfiguration config) {
        if (this.colors != null && !this.colors.isEmpty()) {
            config.CLOUD_COLOR = new ArrayList<>(this.colors);
        }
        config.COLOR_MODE = this.colorMode;
        LoggerProvider.get().info("Applied cloud color preset: {}", this.displayName);
    }

    /**
     * Create a copy of this preset with a new ID
     */
    @Override
    public CloudColorPreset copy(String newId, String newDisplayName) {
        CloudColorPreset copy = new CloudColorPreset(newId, newDisplayName, this.description);
        copy.colors = new ArrayList<>(this.colors);
        copy.colorMode = this.colorMode;
        return copy;
    }

    public void sortColorsByTime() {
        if (colors == null) return;
        colors.sort((a, b) -> Integer.compare(a.time, b.time));
    }

    @Override
    public boolean validate(String payload) {
        String json = Preset.decodePayload(payload);
        JsonObject obj = Preset.parseJsonObject(json);
        if (obj == null) return false;
        if (!obj.has("colors") || !obj.get("colors").isJsonArray()) return false;
        if (!obj.has("colorMode")) return false;

        JsonArray colorsArray = obj.getAsJsonArray("colors");
        if (colorsArray.size() == 0) return false;

        for (var element : colorsArray) {
            if (!element.isJsonObject()) return false;
            var colorObject = element.getAsJsonObject();
            if (!colorObject.has("time") || !colorObject.has("color")) return false;
        }

        return true;
    }

    public boolean isValid() {
        if (id == null || id.isBlank()) return false;
        if (displayName == null || displayName.isBlank()) return false;
        if (colors == null || colors.isEmpty()) return false;

        for (CloudsConfiguration.SkyColorKeypoint kp : colors) {
            if (kp == null) return false;
            if (kp.time < 0 || kp.time > CloudsConfiguration.LightingParameters.DAY_LENGTH) return false;
            if (kp.color < 0) return false; 
            // any color format is valid, 
            // as they're all valid 32-bit integers 
            // (we only take the lower 24-bit for the RGB components, idfc about format corruption)
        }
        return true;
    }
}
