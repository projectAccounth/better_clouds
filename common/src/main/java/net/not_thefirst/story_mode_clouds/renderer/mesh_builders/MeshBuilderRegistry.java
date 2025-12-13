package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;

public class MeshBuilderRegistry {
    private static Map<String, MeshTypeBuilder> builders = new HashMap<>();
    private static boolean frozen = false;
    
    public static MeshTypeBuilder PUFF;
    public static MeshTypeBuilder NORMAL;

    public static MeshTypeBuilder register(String name, Supplier<MeshTypeBuilder> builderSupplier) {
        if (frozen)
            throw new IllegalStateException("Registry is frozen.");
        if (builders.containsKey(name))
            throw new IllegalArgumentException("Mesh builder already registered: " + name);
        
        MeshTypeBuilder builderInstance = builderSupplier.get();
        builders.put(name, builderInstance);
        return builderInstance;
    }

    public static MeshTypeBuilder getBuilder(String name) {
        MeshTypeBuilder builder = builders.get(name);
        if (builder == null)
            throw new IllegalArgumentException("No builder registered for state: " + name);
        return builder;
    }

    public static void freezeRegistry() {
        builders = Collections.unmodifiableMap(builders);
        frozen = true;
    }

    static {
        PUFF   = register(CustomCloudRenderer.CloudMode.POPULATED.name(), PuffMeshBuilder::new);
        NORMAL = register(CustomCloudRenderer.CloudMode.NORMAL.name()   , ClassicMeshBuilder::new);
    }
}
