package net.not_thefirst.story_mode_clouds.config.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

public class CloudResourceLoader {
    private CloudResourceLoader() {}

    /**
     * Load a texture for a cloud layer using the ResourceHandler.
     * Searches in the specified namespace, "textures" directory, with the configured texture name and ".png" suffix.
     * Sends chat feedback if texture is not found.
     * 
     * @param namespace Namespace to search in
     * @param textureName Texture file name without "textures/" prefix and without ".png" suffix (e.g., "environment/clouds")
     * @return Optional containing TextureData if found and loaded successfully, empty Optional otherwise
     */
    public static Optional<Texture.TextureData> loadLayerTexture(String namespace, String textureName, boolean flipX, boolean flipY) {
        ResourceManager resourceManager = ClientHelper.ResourceHelper.getResourceManager();

        if (resourceManager == null) {
            LoggerProvider.get().warn("ResourceManager is null, cannot load texture: {}:{}", namespace, textureName);
            return Optional.empty();
        }
        
        var resources = ResourceHandler.getResourcesWithNameInDirectoryAndNamespace(
            resourceManager,
            namespace,
            "textures",
            textureName,
            ".png"
        );

        if (resources.isEmpty()) {
            String errorMsg = String.format("Texture not found: %s:%s.png in textures/", namespace, textureName);
            ClientHelper.sendLocalSystemMessage(ComponentWrapper.literal(errorMsg));
            LoggerProvider.get().warn("Custom cloud texture not found: namespace={}, name={}", namespace, textureName);
            return Optional.empty();
        }

        // Get the first resource (should only be one match with exact name)
        var resource = resources.values().stream().findFirst().orElse(null);
        if (resource == null) {
            LoggerProvider.get().warn("Failed to retrieve texture resource");
            return Optional.empty();
        }

        // Load the texture from the resource
        try (InputStream inputStream = resource.open()) {
            Optional<Texture.TextureData> textureData = Texture.buildTexture(inputStream, flipX, flipY);
            if (textureData.isPresent()) {
                LoggerProvider.get().info("Loaded custom cloud texture: {}:{}.png", namespace, textureName);
                return textureData;
            } else {
                String errorMsg = String.format("§cFailed to load texture: %s:%s.png", namespace, textureName);
                ClientHelper.sendLocalSystemMessage(ComponentWrapper.literal(errorMsg));
                LoggerProvider.get().warn("Failed to build texture from resource: {}:{}.png", namespace, textureName);
                return Optional.empty();
            }
        } catch (IOException e) {
            String errorMsg = String.format("§cError reading texture: %s:%s.png - %s", namespace, textureName, e.getMessage());
            ClientHelper.sendLocalSystemMessage(ComponentWrapper.literal(errorMsg));
            LoggerProvider.get().error("Exception loading texture: {}:{}.png - {}", namespace, textureName, e.getMessage());
            return Optional.empty();
        }
    }
}
