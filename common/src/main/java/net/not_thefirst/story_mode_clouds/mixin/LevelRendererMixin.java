package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.not_thefirst.story_mode_clouds.config.state.RenderStatus;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin {
    @Inject(method = { 
        CloudRenderInjection.MODERN_FABRIC_RENDER, 
        CloudRenderInjection.MODERN_FORGE_RENDER }, at = @At("INVOKE"), cancellable = true, require = 0)
    private void interceptCloudRender(
        CallbackInfo ci
    ) {
        if (!RenderStatus.shouldRender())
            return;
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void finishCloudRender(CallbackInfo ci) {
        if (!RenderStatus.shouldRender())
            return;
        try {
            RendererHolder.finishCloudRender();
        }
        catch (Exception e) {
            LoggerProvider.get().error("Error finishing cloud render", e);
            RenderStatus.onRenderFailed();
        }
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ARGB;alpha(I)I"
        )
    )
    private int forceAlphaCheckToPass(int originalAlpha) {
        if (!RenderStatus.shouldRender()) {
            return originalAlpha;
        }
        return Math.max(1, originalAlpha);
    }
}