package net.not_thefirst.story_mode_clouds.renderer.types;

import net.not_thefirst.story_mode_clouds.utils.memory.NamedRegistry;

public class MeshTypeRegistry extends NamedRegistry<MeshType> {
    public static final MeshType NORMAL;
    public static final MeshType BEVELED;
    public static final MeshType POPULATED;
    public static final MeshType POPULATED_ND;
    public static final MeshType NORMAL_FAST;

    private static final MeshTypeRegistry INSTANCE = new MeshTypeRegistry();
    public static MeshTypeRegistry getInstance() {
        return INSTANCE;
    }

    static {
        NORMAL_FAST   = INSTANCE.register("NORMAL_FAST"  , () -> new MeshType("NORMAL_FAST"  , true ));
        NORMAL        = INSTANCE.register("NORMAL"       , () -> new MeshType("NORMAL"       , true ));
        BEVELED       = INSTANCE.register("BEVELED"      , () -> new MeshType("BEVELED"      , true ));
        POPULATED     = INSTANCE.register("POPULATED"    , () -> new MeshType("POPULATED"    , true ));
        POPULATED_ND  = INSTANCE.register("POPULATED_ND" , () -> new MeshType("POPULATED_ND" , false));
    }
}
