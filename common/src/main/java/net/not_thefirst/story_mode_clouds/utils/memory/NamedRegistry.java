package net.not_thefirst.story_mode_clouds.utils.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public abstract class NamedRegistry<T> {

    protected Map<String, T> registry;
    protected boolean frozen = false;

    protected NamedRegistry(Map<String, T> initialRegistry) {
        this.registry = initialRegistry;
    }

    protected NamedRegistry() {
        this.registry = new HashMap<>();
    }

    /**
     * Adds a new builder to the registry. Throws an exception if the registry is frozen or if the name is already registered.
     * @param name The name of the builder to register.
     * @param builderSupplier The supplier of the builder instance.
     * @return The registered builder instance.
     */
    public T register(String name, Supplier<T> builderSupplier) {
        if (frozen) throw new IllegalStateException("Registry is frozen.");
        if (registry.containsKey(name)) throw new IllegalArgumentException(
            "Mesh builder already registered: " + name
        );

        T builderInstance = builderSupplier.get();
        registry.put(name, builderInstance);
        return builderInstance;
    }

    public boolean isRegistered(String name) {
        return registry.containsKey(name);
    }

    /**
     * Returns true if the registry is frozen, false otherwise.
     * @return True if the registry is frozen, false otherwise.
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Returns a set of all registered names.
     * @return A set of all registered names.
     */
    public Set<String> keys() {
        return registry.keySet();
    }

    /**
     * Returns a map of all registered names and their corresponding objects.
     * @return A map of all registered names and their corresponding objects.
     */
    public Map<String, T> registry() {
        return new HashMap<>(this.registry);
    }

    /**
     * Returns a list of all registered objects.
     * @return A list of all registered objects.
     */
    public List<T> values() {
        return frozen
            ? List.copyOf(registry.values())
            : new ArrayList<>(registry.values());
    }

    /**
     * Tries to get an object from the registry by name.
     * @param name The name of the object to get.
     * @return The object if found, null otherwise.
     */
    @Nullable
    public T tryGetObject(String name) {
        try {
            return getObject(name);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets an object from the registry by name.
     * @param name The name of the object to get.
     * @return The object if found, throws IllegalArgumentException otherwise.
     */
    public T getObject(String name) {
        T builder = registry.get(name);
        if (builder == null) throw new IllegalArgumentException(
            "No builder registered for state: " + name
        );
        return builder;
    }

    /**
     * Freezes the registry, making it immutable.
     */
    public void freezeRegistry() {
        registry = Collections.unmodifiableMap(registry);
        frozen = true;
    }

    /**
     * Unfreezes the registry, making it mutable.
     * Restricted to subclasses.
     */
    protected void unfreezeRegistry() {
        frozen = false;
    }
}
