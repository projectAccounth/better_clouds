package net.not_thefirst.story_mode_clouds.utils.math;

import java.util.HashMap;
import java.util.Map;

public class NoiseCache {
    private static final Map<Long, PerlinNoise> CACHE = new HashMap<>();

    public static PerlinNoise getNoiseInstance(final long seed) {
        return CACHE.computeIfAbsent(seed, s -> new PerlinNoise(s));
    }
}
