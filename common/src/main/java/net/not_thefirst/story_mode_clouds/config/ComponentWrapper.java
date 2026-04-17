package net.not_thefirst.story_mode_clouds.config;

import net.minecraft.network.chat.Component;

public final class ComponentWrapper {

    private ComponentWrapper() {}

    public static Component literal(String text) {
        return Component.literal(text);
    }

    public static Component translatable(String key) {
        return Component.translatable(key);
    }
}
