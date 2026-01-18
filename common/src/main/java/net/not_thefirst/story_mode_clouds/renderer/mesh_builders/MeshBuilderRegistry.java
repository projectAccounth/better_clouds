package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.utils.memory.NamedRegistry;

public class MeshBuilderRegistry extends NamedRegistry<MeshTypeBuilder> {
    public static final MeshTypeBuilder POPULATED;
    public static final  MeshTypeBuilder POPULATED_ND;
    public static final  MeshTypeBuilder NORMAL;
    public static final  MeshTypeBuilder BEVELED;
    public static final  MeshTypeBuilder NORMAL_FAST;

    public static final  MeshBuilderRegistry INSTANCE = new MeshBuilderRegistry();
    public static MeshBuilderRegistry getInstance() {
        return INSTANCE;
    }

    static {
        NORMAL_FAST   = INSTANCE.register("NORMAL_FAST"  , ClassicFastMeshBuilder::new);
        POPULATED     = INSTANCE.register("POPULATED"    , PuffMeshBuilder::new);
        POPULATED_ND  = INSTANCE.register("POPULATED_ND" , PuffMeshBuilder::new);
        NORMAL        = INSTANCE.register("NORMAL"       , ClassicMeshBuilder::new);
        BEVELED       = INSTANCE.register("BEVELED"      , BeveledMeshBuilder::new);
    }
}
