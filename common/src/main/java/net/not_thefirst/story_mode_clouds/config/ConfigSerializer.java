package net.not_thefirst.story_mode_clouds.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Provides serialization and deserialization of CloudsConfiguration to/from strings.
 * Supports Base64 encoding for compact shareable strings.
 */
public class ConfigSerializer {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Serialize a CloudsConfiguration to a JSON string.
     *
     * @param config The configuration to serialize
     * @return JSON representation of the configuration
     */
    public static String toJson(CloudsConfiguration config) {
        return GSON.toJson(config);
    }

    /**
     * Deserialize a CloudsConfiguration from a JSON string.
     *
     * @param json The JSON string
     * @return Deserialized CloudsConfiguration, or null if deserialization fails
     */
    public static CloudsConfiguration fromJson(String json) {
        try {
            return GSON.fromJson(json, CloudsConfiguration.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Serialize a CloudsConfiguration to a Base64-encoded string for easy sharing.
     * The string is compact and can be placed in chat, config files, etc.
     *
     * @param config The configuration to serialize
     * @return Base64-encoded string representation
     */
    public static String toBase64(CloudsConfiguration config) {
        String json = toJson(config);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Deserialize a CloudsConfiguration from a Base64-encoded string.
     *
     * @param base64 The Base64-encoded string
     * @return Deserialized CloudsConfiguration, or null if decoding/deserialization fails
     */
    public static CloudsConfiguration fromBase64(String base64) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Estimate the encoding format from a string.
     * Returns "base64" if it looks like Base64, "json" otherwise.
     *
     * @param data The string to check
     * @return Either "base64" or "json"
     */
    public static String guessFormat(String data) {
        // random evil trick
        if (data.trim().startsWith("{")) {
            return "json";
        }
        return "base64";
    }

    /**
     * Deserialize from either JSON or Base64 format (auto-detects).
     *
     * @param data The string to deserialize
     * @return Deserialized CloudsConfiguration, or null if deserialization fails
     */
    public static CloudsConfiguration fromAny(String data) {
        String format = guessFormat(data);
        if ("json".equals(format)) {
            return fromJson(data);
        } else {
            return fromBase64(data);
        }
    }
}
