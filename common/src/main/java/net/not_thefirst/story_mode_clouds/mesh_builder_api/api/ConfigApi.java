package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/** Groups the configuration values exposed to a generator. */
public final class ConfigApi extends TwoArgFunction {
    private final LuaValue global;
    private final LuaValue layer;
    private final LuaValue type;

    public ConfigApi(LuaValue global, LuaValue layer, LuaValue type) {
        this.global = global;
        this.layer = layer;
        this.type = type;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable config = new LuaTable();
        config.set("global", global);
        config.set("layer", layer);
        config.set("type", type);
        env.set("Config", config);
        return config;
    }
}