package net.not_thefirst.story_mode_clouds.config.presets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

/**
 * Base interface for all category preset types.
 * Provides common methods for all preset implementations.
 */
public interface Preset {
    
    /**
     * Get the unique identifier for this preset
     */
    String getId();
    
    /**
     * Get the human-readable display name
     */
    String getDisplayName();
    
    /**
     * Get the description of this preset
     */
    String getDescription();
    
    /**
     * Get the timestamp when this preset was last modified
     */
    long getLastModified();
    
    /**
     * Apply this preset's settings to the given configuration
     */
    void applyTo(CloudsConfiguration config);
    
    /**
     * Create a copy of this preset with a new ID and display name
     */
    Preset copy(String newId, String newDisplayName);

    /**
     * Validate a JSON or Base64 payload for this preset type.
     *
     * @param payload JSON or Base64-encoded payload
     * @return true if the payload is valid for this preset type
     */
    boolean validate(String payload);

    static String decodePayload(String payload) {
        if (payload == null) return null;
        String trimmed = payload.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(trimmed);
            return new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static JsonObject parseJsonObject(String json) {
        if (json == null) return null;
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
    }
}
