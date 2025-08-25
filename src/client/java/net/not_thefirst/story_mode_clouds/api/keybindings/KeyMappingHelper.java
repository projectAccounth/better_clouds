package net.not_thefirst.story_mode_clouds.api.keybindings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.not_thefirst.story_mode_clouds.mixin.KeyMappingAccessor;

public final class KeyMappingHelper {

    private static final List<KeyMapping> KEY_MAPPINGS = new ReferenceArrayList<>();

    private static Map<String, Integer> getCategoryMap() {
        return KeyMappingAccessor.GetCategoryMap();
    }

    public static boolean addCategory(String categoryTranslationKey) {
        return getCategoryMap().computeIfAbsent(categoryTranslationKey, key ->
            getCategoryMap().values().stream().max(Integer::compareTo).orElse(0) + 1
        ) != null;
    }

    public static KeyMapping registerKeyBinding(KeyMapping binding) {
        if (Minecraft.getInstance().options != null) {
            throw new IllegalStateException("Key mappings cannot be registered in run-time!");
        }

        if (KEY_MAPPINGS.stream().anyMatch(k ->
                k == binding || k.getTranslatedKeyMessage().equals(binding.getTranslatedKeyMessage())
        )) {
            throw new IllegalArgumentException(
                "Duplicate key binding: " + binding.getTranslatedKeyMessage()
            );
        }

        addCategory(binding.getCategory());
        KEY_MAPPINGS.add(binding);
        return binding;
    }

    public static KeyMapping[] put(KeyMapping[] keys) {
        return Stream.concat(
            Arrays.stream(keys)
                .filter(k -> !KEY_MAPPINGS.contains(k)),
                    KEY_MAPPINGS.stream()
                )
                .toArray(KeyMapping[]::new);
    }
}
