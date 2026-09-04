package net.not_thefirst.story_mode_clouds.mesh_builder_api.runner;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.TableLib;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.Bit64Lib;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.CloudGridApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.ColorApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.ConfigApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.GridCoordsApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.MeshApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.NoiseApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.api.TextureApi;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.utils.LuaStructMapper;
import net.not_thefirst.story_mode_clouds.renderer.MeshBuilder;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;
import net.not_thefirst.story_mode_clouds.utils.math.Texture.TextureData;

public class ThreadLocalMeshRunner {

    /**
     * Executes a pre-compiled bytecode prototype using thread-local context values.
     * 
     * @param proto The pre-compiled script blueprint from your cache.
     * @param builder The builder instance for this specific run.
     * @param config Config
     */
    public static void executeScript(Prototype proto, 
                                    AbstractStaticMesh.Builder<?, ?> builder, 
                                    CloudsConfiguration.LayerConfiguration config,
                                    TextureData gridData, int cx, int cz) {
        
        Globals localEnv = new Globals();

        CloudsConfiguration global = CloudsConfiguration.getInstance();
        
        localEnv.load(new PackageLib());
        localEnv.load(new MathLib());
        localEnv.load(new TableLib());

        localEnv.set("dofile", LuaValue.NIL);
        localEnv.set("loadfile", LuaValue.NIL);

        localEnv.load(new MeshApi(builder));
        localEnv.load(new ColorApi());
        localEnv.load(new NoiseApi());
        localEnv.load(new Bit64Lib());
        localEnv.load(new TextureApi(gridData));
        WrappedCoordinates coordinates = new WrappedCoordinates(cx, cz, global.getCloudGridRange(), gridData.width, gridData.height);
        localEnv.load(new GridCoordsApi(coordinates));
        localEnv.load(new CloudGridApi(gridData, coordinates));

        LuaValue globalConfig = LuaStructMapper.toLua(global);
        LuaValue layerConfig = LuaStructMapper.toLua(config);
        LuaValue typeConfig = LuaValue.NIL;

        localEnv.set("MeshConstants", LuaStructMapper.toLua(new MeshBuilder()));
        localEnv.set("LayerConfig", LuaStructMapper.toLua(config));

        if (config.getTypeConfig(config.MODE) != null) {
            typeConfig = LuaStructMapper.fromMap(config.getTypeConfig(config.MODE).getMap());
            localEnv.set("TypeConfig", typeConfig);
        }
        localEnv.load(new ConfigApi(globalConfig, layerConfig, typeConfig));

        try {
            LuaClosure scriptInstance = new LuaClosure(proto, localEnv);
            scriptInstance.call();
        }
        catch (Exception exc) {
            if (exc instanceof LuaError e) {
                LoggerProvider.get().error("Algorithm runtime crash: {}", e);
            }
            else {
                LoggerProvider.get().error("Runtime exception when constructing script: {}", exc);
            }
        }
    }
}

