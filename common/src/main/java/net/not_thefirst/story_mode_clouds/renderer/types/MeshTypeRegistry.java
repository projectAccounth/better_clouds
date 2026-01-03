package net.not_thefirst.story_mode_clouds.renderer.types;

import net.not_thefirst.story_mode_clouds.utils.memory.NamedRegistry;

public class MeshTypeRegistry extends NamedRegistry<MeshType> {
    public static MeshType NORMAL;
    public static MeshType BEVELED;
    public static MeshType PUFF;
    public static MeshType PUFF_NO_DEPTH;
    public static MeshType NORMAL_FAST;

    private static MeshTypeRegistry INSTANCE = new MeshTypeRegistry();
    public static MeshTypeRegistry getInstance() {
        return INSTANCE;
    }

    static {
        NORMAL_FAST   = INSTANCE.register("NORMAL_FAST"  , () -> new MeshType("NORMAL_FAST"  , true ));
        NORMAL        = INSTANCE.register("NORMAL"       , () -> new MeshType("NORMAL"       , true ));
        BEVELED       = INSTANCE.register("BEVELED"      , () -> new MeshType("BEVELED"      , true ));
        PUFF          = INSTANCE.register("POPULATED"    , () -> new MeshType("PUFF"         , true ));
        PUFF_NO_DEPTH = INSTANCE.register("POPULATED_ND" , () -> new MeshType("PUFF_NO_DEPTH", false));
    }
}
