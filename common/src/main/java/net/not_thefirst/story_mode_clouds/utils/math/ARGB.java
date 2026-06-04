package net.not_thefirst.story_mode_clouds.utils.math;

public final class ARGB {
    private ARGB() {}

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    public static int alpha(int color) { return (color >>> 24) & 0xFF; }
    public static int red(int color)   { return (color >>> 16) & 0xFF; }
    public static int green(int color) { return (color >>>  8) & 0xFF; }
    public static int blue(int color)  { return (color       ) & 0xFF; }

    public static float alphaFloat(int color) { return alpha(color) / 255.0f; }
    public static float redFloat(int color)   { return red(color)   / 255.0f; }
    public static float greenFloat(int color) { return green(color) / 255.0f; }
    public static float blueFloat(int color)  { return blue(color)  / 255.0f; }

    public static int color(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) |
               ((r & 0xFF) << 16) |
               ((g & 0xFF) << 8)  |
               (b & 0xFF);
    }

    public static int toRGBA(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b = argb & 0xFF;
        return ((r & 0xFF) << 24) |
               ((g & 0xFF) << 16) |
               ((b & 0xFF) << 8)  |
               (a & 0xFF);
    }

    public static int colorFromFloat(float a, float r, float g, float b) {
        return color(
            (int)(a * 255.0f + 0.5f),
            (int)(r * 255.0f + 0.5f),
            (int)(g * 255.0f + 0.5f),
            (int)(b * 255.0f + 0.5f)
        );
    }
    
    public static int fromRGBA(int r, int g, int b, int a) {
        return color(a, r, g, b);
    }

    public static int fromRGBAFloat(float r, float g, float b, float a) {
        return colorFromFloat(a, r, g, b);
    }

    public static int fromRGB(int r, int g, int b) {
        return color(255, r, g, b);
    }

    public static int fromRGBFloat(float r, float g, float b) {
        return colorFromFloat(1.0f, r, g, b);
    }

    public static int fromRGBA(int color) {
        int r = (color >>> 24) & 0xFF;
        int g = (color >>> 16) & 0xFF;
        int b = (color >>> 8)  & 0xFF;
        int a = color & 0xFF;
        return color(a, r, g, b);
    }

    public static int fromRGBA(int rgb, int alpha) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8)  & 0xFF;
        int b = rgb & 0xFF;
        return color(alpha, r, g, b);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int withAlphaFloat(int color, float alpha) {
        return withAlpha(color, (int)(alpha * 255.0f + 0.5f));
    }

    public static int lerp(int colorA, int colorB, float t) {
        t = clamp01(t);
        int a = (int)(alpha(colorA) + (alpha(colorB) - alpha(colorA)) * t);
        int r = (int)(red(colorA)   + (red(colorB)   - red(colorA))   * t);
        int g = (int)(green(colorA) + (green(colorB) - green(colorA)) * t);
        int b = (int)(blue(colorA)  + (blue(colorB)  - blue(colorA))  * t);
        return color(a, r, g, b);
    }

    public static int multiply(int colorA, int colorB) {
        int a = (alpha(colorA) * alpha(colorB)) / 255;
        int r = (red(colorA)   * red(colorB))   / 255;
        int g = (green(colorA) * green(colorB)) / 255;
        int b = (blue(colorA)  * blue(colorB))  / 255;
        return color(a, r, g, b);
    }

    public static int multiply(int color, float scalar) {
        scalar = clamp01(scalar);
        int a = (int)(alpha(color) * scalar);
        int r = (int)(red(color)   * scalar);
        int g = (int)(green(color) * scalar);
        int b = (int)(blue(color)  * scalar);
        return color(a, r, g, b);
    }
    
    private static float clamp01(float v) {
        if (v < 0.0f) {
            return 0.0f;
        }
        return v > 1f ? 1f : v;
    }
}
