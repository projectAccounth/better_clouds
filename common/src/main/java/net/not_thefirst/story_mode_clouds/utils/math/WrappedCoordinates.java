package net.not_thefirst.story_mode_clouds.utils.math;

public class WrappedCoordinates {
    private final int[] wrappedX;
    private final int[] wrappedZ;

    private final int centerX;
    private final int centerZ;

    private final int width;
    private final int height;
    private final int range;
    
    public WrappedCoordinates(int centerX, int centerZ, int range, int width, int height) {
        this.width = width;
        this.height = height;
        int size = range * 2 + 1;
        this.wrappedX = new int[size];
        this.wrappedZ = new int[size];
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.range = range;
        
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

    public int getCellIndex(int offsetX, int offsetZ) {
        return getCellIndex(offsetX, offsetZ, this.range);
    }

    public int getCellIndexWrapped(int wx, int wz) {
        return wx + wz * width;
    }

    public int getWrappedX(int offsetX) {
        return wrappedX[offsetX + this.range];
    }
    
    public int getWrappedZ(int offsetZ) {
        return wrappedZ[offsetZ + this.range];
    }

    public int getRange() { return range; }

    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
}
