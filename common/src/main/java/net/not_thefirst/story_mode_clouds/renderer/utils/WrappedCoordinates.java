package net.not_thefirst.story_mode_clouds.renderer.utils;

@SuppressWarnings("unused")
public class WrappedCoordinates {
    private final int[] wrappedX;
    private final int[] wrappedZ;
    private final int width;
    private final int height;
    
    public WrappedCoordinates(int centerX, int centerZ, int range, int width, int height) {
        this.width = width;
        this.height = height;
        int size = range * 2 + 1;
        this.wrappedX = new int[size];
        this.wrappedZ = new int[size];
        
        for (int i = 0; i < size; i++) {
            wrappedX[i] = Math.floorMod(centerX + (i - range), width);
            wrappedZ[i] = Math.floorMod(centerZ + (i - range), height);
        }
    }
    
    public int getWrappedX(int offsetX, int range) {
        return wrappedX[offsetX + range];
    }
    
    public int getWrappedZ(int offsetZ, int range) {
        return wrappedZ[offsetZ + range];
    }
    
    public int getCellIndex(int offsetX, int offsetZ, int range) {
        int x = wrappedX[offsetX + range];
        int z = wrappedZ[offsetZ + range];
        return x + z * width;
    }
}
