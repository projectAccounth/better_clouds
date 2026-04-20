package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

@Mixin(Minecraft.class)
public class ClientMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onResourcePacksReloaded(CallbackInfo ci) {
        ModRenderPipelines.registerCloudPipelines();
        ModRenderPipelines.getProgramManager().reloadAll();
        ModRenderPipelines.postReload();
    }
}
