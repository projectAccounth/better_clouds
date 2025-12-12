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

    /**
     * Adds a new category to the category map if it does not already exist.
     *
     * @param categoryTranslationKey The key for the category.
     * @return true if the category exists (either after creating or found in the map), false otherwise.
     */
    public static boolean addCategory(String categoryTranslationKey) {
        return getCategoryMap().computeIfAbsent(
            categoryTranslationKey, 
            // compute the category key if not in the map
            key -> getCategoryMap()
                    .values() // get all existing category IDs
                    .stream()
                    .max(Integer::compareTo) // find the highest ID
                    .orElse(0) // if no categories exist, start from 0
                    + 1 // assign the next available ID
        ) != null; // returns true if the value is not null (i.e., successfully added/found)
    }

    /**
     * Adds a new binding to the available key mappings of Minecraft.
     *
     * @param binding The KeyMapping for binding.
     * @return The exact KeyMapping passed in.
     */
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
