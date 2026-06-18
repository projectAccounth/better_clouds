package net.not_thefirst.story_mode_clouds.utils.rendering;

public abstract class AbstractUBODataBuffer {
    protected final String name;
    protected int size;

    protected AbstractUBODataBuffer(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }
    
    public int getSize() {
        return size;
    }
}
