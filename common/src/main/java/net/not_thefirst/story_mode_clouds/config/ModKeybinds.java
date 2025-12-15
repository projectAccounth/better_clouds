package net.not_thefirst.story_mode_clouds.config;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.not_thefirst.story_mode_clouds.api.keybindings.KeyMappingHelper;

public class ModKeybinds {
    public static KeyMapping addKeybind(String id, int key, String category) {
        return KeyMappingHelper.registerKeyBinding(new KeyMapping(id, key, category));
    }

    public static KeyMapping OPEN_CONFIG_KEYBIND = new KeyMapping("key.bind", InputConstants.KEY_O, "Misc");

    public static void initialize() {

    }
}
