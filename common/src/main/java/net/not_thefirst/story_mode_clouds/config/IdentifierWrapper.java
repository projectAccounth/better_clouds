package net.not_thefirst.story_mode_clouds.config;


import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * Stable wrapper around Minecraft's Identifier/ResourceLocation class.
 * Too lazy to maintain the hell Mojang's API created, made this to reduce the load.
 */
public class IdentifierWrapper {
    private final ResourceLocation delegate;

    /**
     * Create an identifier from namespace and path.
     * 
     * @param namespace The namespace (e.g., "minecraft", "cloud_twerks")
     * @param path The path (e.g., "textures/environment/clouds.png")
     * @return A new IdentifierWrapper
     */
    public static IdentifierWrapper of(String namespace, String path) {
        return new IdentifierWrapper(new ResourceLocation(namespace, path));
    }

    /**
     * Parse an identifier from a string in "namespace:path" format.
     * 
     * @param string The identifier string (e.g., "minecraft:textures/environment/clouds.png")
     * @return A new IdentifierWrapper
     * @throws IllegalArgumentException if the string is not a valid identifier
     */
    public static IdentifierWrapper parse(String string) {
        return new IdentifierWrapper(ResourceLocation.tryParse(string));
    }

    /**
     * Attempt to parse an identifier from a string.
     * Returns null if the string is not valid.
     * 
     * @param string The identifier string
     * @return A new IdentifierWrapper, or null if invalid
     */
    public static @Nullable IdentifierWrapper tryParse(String string) {
        ResourceLocation id = ResourceLocation.tryParse(string);
        return id != null ? new IdentifierWrapper(id) : null;
    }

    /**
     * Create an identifier with the default "minecraft" namespace.
     * 
     * @param path The path
     * @return A new IdentifierWrapper with "minecraft" namespace
     */
    public static IdentifierWrapper withDefaultNamespace(String path) {
        return new IdentifierWrapper(new ResourceLocation(path));
    }

    private IdentifierWrapper(ResourceLocation delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the namespace ComponentWrapper.
     * 
     * @return The namespace (e.g., "minecraft")
     */
    public String getNamespace() {
        return delegate.getNamespace();
    }

    /**
     * Get the path ComponentWrapper.
     * 
     * @return The path (e.g., "textures/environment/clouds.png")
     */
    public String getPath() {
        return delegate.getPath();
    }

    /**
     * Create a new identifier with a different path but same namespace.
     * 
     * @param newPath The new path
     * @return A new IdentifierWrapper
     */
    public IdentifierWrapper withPath(String newPath) {
        return new IdentifierWrapper(delegate.withPath(newPath));
    }

    /**
     * Get the underlying Minecraft Identifier for interop with Minecraft APIs.
     * Prefer using the wrapper methods when possible.
     * 
     * @return The underlying Identifier/ResourceLocation
     */
    public ResourceLocation getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentifierWrapper)) return false;
        IdentifierWrapper that = (IdentifierWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
