package net.not_thefirst.story_mode_clouds.config.presets;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;

/**
 * Represents a light sources preset containing directional light configurations.
 * This is a category preset that only stores light source settings.
 */
public class LightSourcesPreset implements Preset {
    public String id;
    public String displayName;
    public String description;
    public long lastModified;
    
    public List<DiffuseLight> lights;

    public LightSourcesPreset(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.lastModified = System.currentTimeMillis();
        this.lights = new ArrayList<>();
    }

    public LightSourcesPreset() {
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
    public static LightSourcesPreset fromConfiguration(String id, String displayName, String description, CloudsConfiguration config) {
        LightSourcesPreset preset = new LightSourcesPreset(id, displayName, description);
        
        if (config.LIGHTING.lights != null) {
            // Deep copy the lights to avoid reference issues
            for (DiffuseLight light : config.LIGHTING.lights) {
                preset.lights.add(new DiffuseLight(light.direction(), light.intensity()));
            }
        }
        
        return preset;
    }

    /**
     * Apply this preset's settings to the given configuration
     */
    @Override
    public void applyTo(CloudsConfiguration config) {
        if (this.lights != null) {
            config.LIGHTING.lights = new ArrayList<>();
            for (DiffuseLight light : this.lights) {
                config.LIGHTING.lights.add(new DiffuseLight(light.direction(), light.intensity()));
            }
        }
        LoggerProvider.get().info("Applied light sources preset: {}", this.displayName);
    }

    /**
     * Create a copy of this preset with a new ID
     */
    @Override
    public LightSourcesPreset copy(String newId, String newDisplayName) {
        LightSourcesPreset copy = new LightSourcesPreset(newId, newDisplayName, this.description);
        if (this.lights != null) {
            for (DiffuseLight light : this.lights) {
                copy.lights.add(new DiffuseLight(light.direction(), light.intensity()));
            }
        }
        return copy;
    }

    @Override
    public boolean validate(String payload) {
        String json = Preset.decodePayload(payload);
        JsonObject obj = Preset.parseJsonObject(json);
        if (obj == null) return false;
        if (!obj.has("lights") || !obj.get("lights").isJsonArray()) return false;

        var lightsArray = obj.getAsJsonArray("lights");
        if (lightsArray.size() == 0) return false;
        for (var element : lightsArray) {
            if (!element.isJsonObject()) return false;
            var light = element.getAsJsonObject();
            if (!light.has("directionX") || !light.has("directionY") || !light.has("directionZ") || !light.has("intensity")) {
                return false;
            }
        }
        return true;
    }

    public boolean isValid() {
        if (id == null || id.isBlank()) return false;
        if (displayName == null || displayName.isBlank()) return false;
        if (lights == null) return false;
        for (DiffuseLight light : lights) {
            if (light == null) return false;
            if (light.intensity() < 0f || light.intensity() > 1f) return false;
            if (Float.isNaN(light.direction().x()) || Float.isNaN(light.direction().y()) || Float.isNaN(light.direction().z())) return false;
        }
        return true;
    }

    /**
     * Get the number of active light sources
     */
    public int getLightCount() {
        return this.lights != null ? this.lights.size() : 0;
    }
}
