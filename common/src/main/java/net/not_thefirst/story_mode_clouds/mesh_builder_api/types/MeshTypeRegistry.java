package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import net.not_thefirst.story_mode_clouds.utils.memory.NamedRegistry;

public class MeshTypeRegistry extends NamedRegistry<MeshType> {
    public static MeshType NORMAL;
    public static MeshType POPULATED;
    public static MeshType POPULATED_ND;
    public static MeshType NORMAL_FAST;

    private static final MeshTypeRegistry INSTANCE = new MeshTypeRegistry();
    public static MeshTypeRegistry getInstance() {
        return INSTANCE;
    }

    static {
        NORMAL_FAST    = INSTANCE.register("NORMAL_FAST"   , () -> new MeshType("NORMAL_FAST"   , true , true));
        NORMAL         = INSTANCE.register("NORMAL"        , () -> new MeshType("NORMAL"        , true , true));
        POPULATED      = INSTANCE.register("POPULATED"     , () -> new MeshType("POPULATED"     , true , true));
        POPULATED_ND   = INSTANCE.register("POPULATED_ND"  , () -> new MeshType("POPULATED_ND"  , false, true));
    }
}
