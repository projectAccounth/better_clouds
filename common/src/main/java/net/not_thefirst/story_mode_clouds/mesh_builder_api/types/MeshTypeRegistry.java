package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import net.not_thefirst.story_mode_clouds.utils.memory.NamedRegistry;

public class MeshTypeRegistry extends NamedRegistry<MeshType> {
    private static final MeshTypeRegistry INSTANCE = new MeshTypeRegistry();
    public static MeshTypeRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void clear() {
        super.clear();
    }
}
