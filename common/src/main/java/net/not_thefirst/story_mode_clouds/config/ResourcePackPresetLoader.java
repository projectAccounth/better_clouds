package net.not_thefirst.story_mode_clouds.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * Loads cloud configuration presets from resource packs.
 * Presets are stored as JSON files accessible via resource locations.
 */
public class ResourcePackPresetLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PRESETS_RESOURCE_PATH = "cloud_presets";
    
    private static final Map<String, CloudsConfiguration> RESOURCEPACK_PRESETS = new HashMap<>();

    /**
     * Load a preset from resource packs using an IdentifierWrapper.
     * 
     * @param resourceManager The resource manager to load from
     * @param identifier IdentifierWrapper in format "namespace:path/to/preset.json"
     * @return The loaded configuration, or null if not found
     */
    public static CloudsConfiguration loadPresetFromResources(
            ResourceManager resourceManager,
            IdentifierWrapper identifier) {
        try (InputStream inputStream = resourceManager.open(identifier.getDelegate())) {
            CloudsConfiguration config = GSON.fromJson(
                    new java.io.InputStreamReader(inputStream),
                    CloudsConfiguration.class
            );
            LoggerProvider.get().info("Loaded resource pack preset: " + identifier);
            return config;
        } catch (IOException e) {
            LoggerProvider.get().warn("Failed to load preset from: " + identifier);
            return null;
        }
    }

    /**
     * Construct a resource identifier wrapper for a cloud preset.
     * 
     * @param namespace The resource pack namespace
     * @param presetName Name of the preset (without extension)
     * @return IdentifierWrapper pointing to the preset
     */
    public static IdentifierWrapper createPresetIdentifier(String namespace, String presetName) {
        return IdentifierWrapper.of(namespace, PRESETS_RESOURCE_PATH + "/" + presetName + ".json");
    }

    /**
     * Load an entire preset directory from a resource pack.
     * Expects presets in format: assets/<namespace>/cloud_presets/*.json
     *
     * @param resourceManager The resource manager
     * @param namespace The namespace to load from
     * @return Map of preset names to configurations
     */
    public static Map<String, CloudsConfiguration> loadPresetsFromNamespace(
            ResourceManager resourceManager,
            String namespace) {
        Map<String, CloudsConfiguration> presets = new HashMap<>();
        // Note: In a full implementation, you'd scan the namespace for all preset files
        // This requires resource pack introspection which is complex in Minecraft
        LoggerProvider.get().info("Attempting to load presets from namespace: " + namespace);
        return presets;
    }

    /**
     * Get a cached preset by name.
     *
     * @param presetName The name of the preset
     * @return The configuration, or null if not found
     */
    public static CloudsConfiguration getCachedPreset(String presetName) {
        return RESOURCEPACK_PRESETS.get(presetName);
    }

    /**
     * Cache a loaded preset for faster access.
     *
     * @param presetName The name to cache under
     * @param config The configuration to cache
     */
    public static void cachePreset(String presetName, CloudsConfiguration config) {
        RESOURCEPACK_PRESETS.put(presetName, config);
    }

    /**
     * Clear the preset cache.
     */
    public static void clearCache() {
        RESOURCEPACK_PRESETS.clear();
    }
}
