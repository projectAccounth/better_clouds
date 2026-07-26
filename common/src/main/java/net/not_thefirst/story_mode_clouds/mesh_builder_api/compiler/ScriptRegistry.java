package net.not_thefirst.story_mode_clouds.mesh_builder_api.compiler;

import org.luaj.vm2.Prototype;

import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptRegistry {

    private static final Map<String, Prototype> BYTECODE_CACHE = new ConcurrentHashMap<>();

    public static void loadScript(String scriptId, IdentifierWrapper assetPath) {
        try (InputStream resourceStream = ClientHelper.getClient().getResourceManager().open(assetPath.getDelegate())) {
            Prototype compiledBlueprint = LuaJitCompiler.compile(resourceStream, scriptId + ".lua");
            
            BYTECODE_CACHE.put(scriptId, compiledBlueprint);
            LoggerProvider.get().info("[Mod Script Engine] Successfully registered and cached: {}", scriptId);

        } catch (Exception e) {
            LoggerProvider.get().error("[Mod Script Engine] Failed to load structural resource path {}: {}", assetPath, e.getMessage());
        }
    }

    public static Prototype getCachedScript(String scriptId) {
        return BYTECODE_CACHE.get(scriptId);
    }
}
