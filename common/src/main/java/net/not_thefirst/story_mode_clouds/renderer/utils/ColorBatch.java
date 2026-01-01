package net.not_thefirst.story_mode_clouds.renderer.utils;

import net.not_thefirst.story_mode_clouds.utils.ARGB;

public class ColorBatch {
    private static final int COMPONENTS = 4; // R, G, B, A
    private final float[] data;
    private int count = 0;
    
    public ColorBatch(int maxColors) {
        this.data = new float[maxColors * COMPONENTS];
    }
    
    public void add(int argbColor) {
        int idx = count * COMPONENTS;
        data[idx]     = ARGB.redFloat(argbColor);
        data[idx + 1] = ARGB.greenFloat(argbColor);
        data[idx + 2] = ARGB.blueFloat(argbColor);
        data[idx + 3] = ARGB.alphaFloat(argbColor);
        count++;
    }
    
    public void reset() {
        count = 0;
    }
    
    public float getR(int colorIndex) { return data[colorIndex * COMPONENTS]; }
    public float getG(int colorIndex) { return data[colorIndex * COMPONENTS + 1]; }
    public float getB(int colorIndex) { return data[colorIndex * COMPONENTS + 2]; }
    public float getA(int colorIndex) { return data[colorIndex * COMPONENTS + 3]; }
    
    public static class PreDecomposed {
        public final float r, g, b, a;
        
        public PreDecomposed(int argbColor) {
            this.r = ARGB.redFloat(argbColor);
            this.g = ARGB.greenFloat(argbColor);
            this.b = ARGB.blueFloat(argbColor);
            this.a = ARGB.alphaFloat(argbColor);
        }
    }
}
