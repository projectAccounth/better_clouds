package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

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
        if (!CloudsConfiguration.getInstance().CLOUDS_RENDERED) 
            return;
        ci.cancel();

        CloudStatus cloudStatus = ClientHelper.getOptions().getCloudStatus();
        float partialTicks = ClientHelper.getClient().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = ClientHelper.CameraHelper.getCamera().position();

        RendererHolder.renderCloud(cloudStatus, camPos, partialTicks);
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ARGB;alpha(I)I"
        )
    )
    private int forceAlphaCheckToPass(int originalAlpha) {
        if (!CloudsConfiguration.getInstance().CLOUDS_RENDERED) {
            return originalAlpha;
        }
        return Math.max(1, originalAlpha);
    }
}