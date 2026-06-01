package net.not_thefirst.story_mode_clouds.config.presets;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * Represents a lighting preset containing ambient and shading settings.
 * This is a category preset that only stores lighting parameters (excluding light sources).
 */
public class LightingPreset implements Preset {
    public String id;
    public String displayName;
    public String description;
    public long lastModified;
    
    public float ambientStrength;
    public float maxShadingStrength;
    public CloudsConfiguration.ShadingMode shadingMode;
    public CloudsConfiguration.LightingType lightingType;
    
    public int dayStart;
    public int dayEnd;
    public int dayNoon;

    public LightingPreset(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.lastModified = System.currentTimeMillis();
        
        // Initialize with defaults
        this.ambientStrength = 0.7f;
        this.maxShadingStrength = 0.8f;
        this.shadingMode = CloudsConfiguration.ShadingMode.GOURAUD;
        this.lightingType = CloudsConfiguration.LightingType.STATIC;
        this.dayStart = 0;
        this.dayEnd = 13000;
        this.dayNoon = 6000;
    }

    public LightingPreset() {
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
    public static LightingPreset fromConfiguration(String id, String displayName, String description, CloudsConfiguration config) {
        LightingPreset preset = new LightingPreset(id, displayName, description);
        
        CloudsConfiguration.LightingParameters lighting = config.LIGHTING;
        preset.ambientStrength = lighting.AMBIENT_LIGHTING_STRENGTH;
        preset.maxShadingStrength = lighting.MAX_LIGHTING_SHADING;
        preset.shadingMode = lighting.SHADING_MODE;
        preset.lightingType = lighting.LIGHTING_TYPE;
        preset.dayStart = lighting.DAY_START;
        preset.dayEnd = lighting.DAY_END;
        preset.dayNoon = lighting.DAY_NOON;
        
        return preset;
    }

    /**
     * Apply this preset's settings to the given configuration
     */
    @Override
    public void applyTo(CloudsConfiguration config) {
        CloudsConfiguration.LightingParameters lighting = config.LIGHTING;
        lighting.AMBIENT_LIGHTING_STRENGTH = this.ambientStrength;
        lighting.MAX_LIGHTING_SHADING = this.maxShadingStrength;
        lighting.SHADING_MODE = this.shadingMode;
        lighting.LIGHTING_TYPE = this.lightingType;
        lighting.DAY_START = this.dayStart;
        lighting.DAY_END = this.dayEnd;
        lighting.DAY_NOON = this.dayNoon;
        LoggerProvider.get().info("Applied lighting preset: {}", this.displayName);
    }

    /**
     * Create a copy of this preset with a new ID
     */
    @Override
    public LightingPreset copy(String newId, String newDisplayName) {
        LightingPreset copy = new LightingPreset(newId, newDisplayName, this.description);
        copy.ambientStrength = this.ambientStrength;
        copy.maxShadingStrength = this.maxShadingStrength;
        copy.shadingMode = this.shadingMode;
        copy.lightingType = this.lightingType;
        copy.dayStart = this.dayStart;
        copy.dayEnd = this.dayEnd;
        copy.dayNoon = this.dayNoon;
        return copy;
    }
}
