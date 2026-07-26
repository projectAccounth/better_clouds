package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

public class MeshType {
    private final String name;
    private final boolean doDepthWrite;
    private final boolean nativeBuilder;

    public MeshType(String name, boolean depth, boolean nativeBuilder) {
        this.name = name;
        this.doDepthWrite = depth;
        this.nativeBuilder = nativeBuilder;
    }

    public String name() { return this.name; }
    public String getName() { return name(); }
    public boolean doDepthWrite() { return doDepthWrite; }
    public boolean usesNativeBuilder() { return nativeBuilder; }
}
