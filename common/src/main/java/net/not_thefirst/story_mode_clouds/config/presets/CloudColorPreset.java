package net.not_thefirst.story_mode_clouds.config.presets;

import java.util.ArrayList;
import java.util.List;

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
}
