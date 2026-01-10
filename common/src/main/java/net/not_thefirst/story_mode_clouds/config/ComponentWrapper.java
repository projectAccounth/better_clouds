package net.not_thefirst.story_mode_clouds.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class ComponentWrapper {

    private ComponentWrapper() {}

    public static Component literal(String text) {
        return new TextComponent(text);
    }

    public static Component translatable(String key) {
        return new TranslatableComponent(key);
    }
}
