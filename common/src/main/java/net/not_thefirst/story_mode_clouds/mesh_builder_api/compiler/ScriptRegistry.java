package net.not_thefirst.story_mode_clouds.mesh_builder_api.compiler;

import org.luaj.vm2.Prototype;

import net.not_thefirst.story_mode_clouds.config.resources.ResourceHandler;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.memory.SharedHandle;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptRegistry {

    private static final Map<String, SharedHandle<Prototype>> BYTECODE_CACHE = new ConcurrentHashMap<>();

    public static void loadScript(String scriptId, IdentifierWrapper assetPath) {
        try (InputStream resourceStream = ResourceHandler.getResource(ClientHelper.getClient().getResourceManager(), assetPath).open()) {
            Prototype compiledBlueprint = LuaJitCompiler.compile(resourceStream, scriptId + ".lua");
            
            BYTECODE_CACHE.put(scriptId, new SharedHandle<>(compiledBlueprint));
            LoggerProvider.get().info("Loaded script {} with id {}", assetPath, scriptId);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to load structural resource path {}: {}", assetPath, e.getMessage());
        }
    }

    public static void loadScript(String scriptId, InputStream stream) {
        try {
            Prototype compiledBlueprint = LuaJitCompiler.compile(stream, scriptId + ".lua");
            
            BYTECODE_CACHE.put(scriptId, new SharedHandle<>(compiledBlueprint));
            LoggerProvider.get().info("Loaded script with id {}", scriptId);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to load structural resource stream {}: {}", stream, e.getMessage());
        }
    }

    public static void clear() {
        BYTECODE_CACHE.forEach((name, script) -> {
            script.invalidate(proto -> {
            });

            LoggerProvider.get().info("Successfully cleared script {}", name);
        });

        BYTECODE_CACHE.clear();
    }

    public static SharedHandle<Prototype> getCachedScript(String scriptId) {
        return BYTECODE_CACHE.get(scriptId);
    }
}
