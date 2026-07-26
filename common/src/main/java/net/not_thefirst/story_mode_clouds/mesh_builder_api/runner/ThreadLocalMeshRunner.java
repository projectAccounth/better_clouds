package net.not_thefirst.story_mode_clouds.mesh_builder_api.runner;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.TableLib;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.ColorApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.MeshApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.NoiseApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.utils.LuaStructMapper;

public class ThreadLocalMeshRunner {

    /**
     * Executes a pre-compiled bytecode prototype using thread-local context values.
     * 
     * @param proto The pre-compiled script blueprint from your cache.
     * @param builder The builder instance for this specific run.
     * @param config Config
     */
    public static void executeIsolatedScript(Prototype proto, 
                                             AbstractStaticMesh.Builder<?, ?> builder, 
                                             CloudsConfiguration.LayerConfiguration config) {
        
        Globals localEnv = new Globals();
        
        localEnv.load(new PackageLib());
        localEnv.load(new MathLib());
        localEnv.load(new TableLib());

        localEnv.load(new MeshApi(builder));
        localEnv.load(new ColorApi());
        localEnv.load(new NoiseApi());

        
        localEnv.set("Config", LuaStructMapper.toLua(config));
        localEnv.set("Appearance", LuaStructMapper.toLua(config.APPEARANCE));

        try {
            LuaClosure scriptInstance = new LuaClosure(proto, localEnv);
            scriptInstance.call();
        } catch (LuaError e) {
            System.err.println("Algorithm runtime crash: " + e.getMessage());
        }
    }
}

