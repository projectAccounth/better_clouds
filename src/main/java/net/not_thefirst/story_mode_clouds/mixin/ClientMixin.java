package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ModKeybinds;

@Mixin(Minecraft.class)
public abstract class ClientMixin {
    @Inject(method = "handleKeybinds()V", at = @At("HEAD"))
    private void handleConfigWindow(CallbackInfo ci) {
        while (ModKeybinds.OPEN_CONFIG_KEYBIND.consumeClick()) {
            Minecraft.getInstance().setScreen(CloudsConfiguration.createConfigScreen(Minecraft.getInstance().screen));
        }
    }
}
