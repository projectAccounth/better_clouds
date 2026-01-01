package net.not_thefirst.story_mode_clouds.renderer.mesh_builders;

import net.not_thefirst.story_mode_clouds.utils.NamedRegistry;

public class MeshBuilderRegistry extends NamedRegistry<MeshTypeBuilder> {
    public static MeshTypeBuilder PUFF;
    public static MeshTypeBuilder PUFF_NO_DEPTH;
    public static MeshTypeBuilder NORMAL;
    public static MeshTypeBuilder BEVELED;

    public static MeshBuilderRegistry INSTANCE = new MeshBuilderRegistry();
    public static MeshBuilderRegistry getInstance() {
        return INSTANCE;
    }

    static {
        PUFF          = INSTANCE.register("POPULATED"    , PuffMeshBuilder::new);
        PUFF_NO_DEPTH = INSTANCE.register("POPULATED_ND" , PuffMeshBuilder::new);
        NORMAL        = INSTANCE.register("NORMAL"       , ClassicMeshBuilder::new);
        BEVELED       = INSTANCE.register("BEVELED"      , BeveledMeshBuilder::new);
    }
}
