package net.not_thefirst.story_mode_clouds.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public abstract class NamedRegistry<T> {
    protected Map<String, T> registry;
    protected static boolean frozen = false;

    protected NamedRegistry(Map<String, T> initialRegistry) {
        this.registry = initialRegistry;
    }

    protected NamedRegistry() {
        this.registry = new HashMap<>();
    }

    public T register(String name, Supplier<T> builderSupplier) {
        if (frozen)
            throw new IllegalStateException("Registry is frozen.");
        if (registry.containsKey(name))
            throw new IllegalArgumentException("Mesh builder already registered: " + name);
        
        T builderInstance = builderSupplier.get();
        registry.put(name, builderInstance);
        return builderInstance;
    }
    
    public boolean isRegistered(String name) {
        return registry.containsKey(name);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Set<String> keys() {
        return registry.keySet();
    }

    public T getObject(String name) {
        T builder = registry.get(name);
        if (builder == null)
            throw new IllegalArgumentException("No builder registered for state: " + name);
        return builder;
    }

    public void freezeRegistry() {
        registry = Collections.unmodifiableMap(registry);
        frozen = true;
    }

    protected void unfreezeRegistry() {
        frozen = false;
    }
}
