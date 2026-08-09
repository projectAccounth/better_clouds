package net.not_thefirst.story_mode_clouds.config;

import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.backend.impl.YaclConfigBackend;

public class BackendHolder {
    private BackendHolder() {}

    private static ConfigBackend backend;

    public static void setBackend(ConfigBackend backend) {
        BackendHolder.backend = backend;
    }

    public static ConfigBackend getBackend() {
        if (backend == null) {
            throw new IllegalStateException("Config backend has not been set.");
        }
        return backend;
    }

    public static void init() {
        setBackend(YaclConfigBackend.create());
    }
}
