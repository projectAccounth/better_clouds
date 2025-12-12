package net.not_thefirst.story_mode_clouds.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
	static Map<String, Integer> GetCategoryMap() { throw new UnsupportedOperationException(); };
}
