package net.not_thefirst.story_mode_clouds.utils.math;

public class MathUtils {
    private MathUtils() {}

    public static <T extends Comparable<T>> T clamp(T value, T min, T max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    public static float lerp(float frt, float start, float end) {
        return start + frt * (end - start);
    }
}
