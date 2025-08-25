package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.not_thefirst.story_mode_clouds.api.keybindings.KeyMappingHelper;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Mutable
	@Final
	@Shadow
	public KeyMapping[] keyMappings;

	@Inject(at = @At("HEAD"), method = "load()V")
	public void putKeys(CallbackInfo info) {
		keyMappings = KeyMappingHelper.put(keyMappings);
	}
}
