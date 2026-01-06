package net.not_thefirst.story_mode_clouds.renderer.types;

public class MeshType {
    private final String name;
    private final boolean doDepthWrite;

    public MeshType(String name, boolean depth) {
        this.name = name;
        this.doDepthWrite = depth;
    }

    public String name() { return this.name; }
    public boolean doDepthWrite() { return doDepthWrite; }
}
