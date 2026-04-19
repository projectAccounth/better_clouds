package net.not_thefirst.story_mode_clouds.renderer;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;

/**
 * Manages per-layer cloud textures using resource identifiers.
 * Allows different cloud layers to use different texture sources.
 */
public class CloudTextureLayers {
    private static final Map<Integer, IdentifierWrapper> LAYER_TEXTURES = new HashMap<>();
    private static final IdentifierWrapper DEFAULT_TEXTURE = 
        IdentifierWrapper.of("minecraft", "textures/environment/clouds.png");

    /**
     * Set the texture for a specific layer.
     *
     * @param layerIndex The layer index
     * @param identifier The texture resource identifier
     */
    public static void setLayerTexture(int layerIndex, IdentifierWrapper identifier) {
        LAYER_TEXTURES.put(layerIndex, identifier);
        LoggerProvider.get().info("Set texture for layer " + layerIndex + ": " + identifier.toString());
    }

    /**
     * Get the texture identifier for a layer, or the default if not set.
     *
     * @param layerIndex The layer index
     * @return IdentifierWrapper for the layer's texture
     */
    public static IdentifierWrapper getLayerTexture(int layerIndex) {
        return LAYER_TEXTURES.getOrDefault(layerIndex, DEFAULT_TEXTURE);
    }

    /**
     * Get the underlying Minecraft Identifier for a layer (for Minecraft interop).
     *
     * @param layerIndex The layer index
     * @return Minecraft Identifier
     */
    public static ResourceLocation getLayerTextureId(int layerIndex) {
        return getLayerTexture(layerIndex).getDelegate();
    }

    /**
     * Clear all custom layer textures, reverting to defaults.
     */
    public static void clearCustomTextures() {
        LAYER_TEXTURES.clear();
    }

    /**
     * Load a texture from the resource manager for a given layer.
     *
     * @param resourceManager The resource manager
     * @param layerIndex The layer index
     * @return Loaded texture data, or null if not found
     */
    public static Texture.TextureData loadLayerTexture(ResourceManager resourceManager, int layerIndex) {
        IdentifierWrapper wrapper = getLayerTexture(layerIndex);
        try (java.io.InputStream inputStream = resourceManager.open(wrapper.getDelegate())) {
            return Texture.buildTexture(inputStream).orElse(null);
        } catch (java.io.IOException e) {
            LoggerProvider.get().warn("Failed to load texture for layer " + layerIndex + ": " + wrapper.toString());
            return null;
        }
    }

    /**
     * Set a layer texture from config/presets format (namespace:path).
     *
     * @param layerIndex The layer index
     * @param textureString Texture identifier in format "namespace:path"
     */
    public static void setLayerTextureFromString(int layerIndex, String textureString) {
        try {
            IdentifierWrapper identifier = IdentifierWrapper.tryParse(textureString);
            if (identifier != null) {
                setLayerTexture(layerIndex, identifier);
            } else {
                LoggerProvider.get().warn("Invalid texture identifier: " + textureString);
            }
        } catch (IllegalArgumentException e) {
            LoggerProvider.get().warn("Invalid texture identifier: " + textureString);
        }
    }
}
