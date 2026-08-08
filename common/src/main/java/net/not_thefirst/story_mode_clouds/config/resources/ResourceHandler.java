package net.not_thefirst.story_mode_clouds.config.resources;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

// fancy wrapper
public class ResourceHandler {
    private ResourceHandler() {}

    /**
     * Scans all namespaces with the provided directory name, filtered by suffix, and returns a map of resource identifiers to resources.
     * Expects resources to be located at: <assets/data>/<namespace>/<directory>/*.json
     * @param resourceManager Resource manager
     * @param directory Directory
     * @param suffix File suffix filter (e.g. ".json")
     * @return Map of resource identifiers to resources
     */
    public static Map<IdentifierWrapper, Resource> getResourcesWithPrefixInDirectory(ResourceManager resourceManager, String directory, String suffix) {
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("Suffix cannot be empty when scanning for resources with prefix in directory");
        }
        return getResourcesWithNameInDirectory(resourceManager, directory, "", suffix);
    }

    /**
     * Loads a resource from the resource manager using the provided identifier.
     * @param resourceManager Resource manager
     * @param identifier Resource identifier
     * @return The loaded resource, or null if not found
     */
    @Nullable
    public static Resource getResource(ResourceManager resourceManager, IdentifierWrapper identifier) {
        return resourceManager.getResource(identifier.getDelegate()).orElse(null);
    }

    /**
     * Get all resources with the specified name and suffix in the given directory across all namespaces.
     * Expects resources to be located at: <assets/data>/<namespace>/<directory>/<name><suffix>
     * @param resourceManager Resource manager
     * @param directory Directory 
     * @param name File name without suffix, leave blank to match any name
     * @param suffix File suffix (e.g. ".json"), leave blank to match any suffix
     * @return Map of resource identifiers to resources
     */
    public static Map<IdentifierWrapper, Resource> getResourcesWithNameInDirectory(ResourceManager resourceManager, String directory, String name, String suffix) {
        return getResourcesWithNameInDirectoryAndNamespace(resourceManager, "", directory, name, suffix);
    }

    /**
     * Get all resources with the specified name and suffix in the given directory and namespace.
     * Expects resources to be located at: <assets/data>/<namespace>/<directory>/<name><suffix>
     * @param resourceManager Resource manager
     * @param namespace Namespace to filter by, leave blank to match any namespace
     * @param directory Directory
     * @param name  File name without suffix, leave blank to match any name
     * @param suffix  File suffix (e.g. ".json"), leave blank to match any suffix
     * @return Map of resource identifiers to resources
     */
    public static Map<IdentifierWrapper, Resource> getResourcesWithNameInDirectoryAndNamespace(
        ResourceManager resourceManager, 
        String namespace, 
        String directory, 
        String name, 
        String suffix) {

        var resources = resourceManager.listResources(directory, path -> {
            if (!path.getNamespace().isEmpty() && !path.getNamespace().equals(namespace)) {
                return false;
            }
            String fileName = path.getPath().substring(path.getPath().lastIndexOf("/") + 1);
            if (suffix.isEmpty()) {
                return fileName.equals(name);
            }
            if (name.isEmpty()) {
                return fileName.endsWith(suffix);
            }
            return fileName.equals(name + suffix);
        });
        Map<IdentifierWrapper, Resource> result = new HashMap<>();
        resources.forEach((id, resource) -> {
            IdentifierWrapper wrapper = IdentifierWrapper.of(id.getNamespace(), id.getPath());
            result.put(wrapper, resource);
        });
        return result;
    }

    /**
     * Get all resources with the specified name and suffix in the given directory and namespace.
     * Expects resources to be located at: <assets/data>/<namespace>/<directory>/<name><suffix>
     * @param resourceManager Resource manager
     * @param namespace Namespace to filter by, leave blank to match any namespace
     * @param directory Directory
     * @param name  File name without suffix, leave blank to match any name
     * @param suffix  File suffix (e.g. ".json"), leave blank to match any suffix
     * @return Map of resource identifiers to resources
     */
    public static Map<IdentifierWrapper, Resource> getResourcesInDirectoryAndNamespace(
        ResourceManager resourceManager, 
        String namespace, 
        String directory,
        String suffix) {

        var resources = resourceManager.listResources(directory, path -> {
            if (!path.getNamespace().isEmpty() && !path.getNamespace().equals(namespace)) {
                return false;
            }
            String fileName = path.getPath().substring(path.getPath().lastIndexOf("/") + 1);
            return fileName.endsWith(suffix);
        });
        Map<IdentifierWrapper, Resource> result = new HashMap<>();
        resources.forEach((id, resource) -> {
            IdentifierWrapper wrapper = IdentifierWrapper.of(id.getNamespace(), id.getPath());
            result.put(wrapper, resource);
        });
        return result;
    }

    /**
     * Get all resources with the specified name and suffix across all namespaces.
     * gotta find a way to make this work
     * @param resourceManager Resource manager
     * @param name File name without suffix
     * @param suffix File suffix (e.g. ".json"), leave blank to match any suffix
     * @return Map of resource identifiers to resources
     */
    public static Map<IdentifierWrapper, Resource> getResourcesWithName(ResourceManager resourceManager, String name, String suffix) {
        return getResourcesWithNameInDirectory(resourceManager, "", name, suffix);
    }

    /**
     * Reads the content of a resource as a string.
     * @param resource The resource to read
     * @return The content of the resource as a string, or empty string if an error occurs
     */
    public static String readResourceAsString(Resource resource) {
        try (var reader = resource.openAsReader()) {
            return reader.readAllLines().stream().reduce((a, b) -> a + "\n" + b).orElse("");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
