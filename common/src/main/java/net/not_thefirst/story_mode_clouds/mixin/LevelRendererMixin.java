package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.CloudRendererHolder;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin implements CloudRendererHolder {

    @Override
    public CustomCloudRenderer getCustomCloudRenderer() {
        return RendererHolder.get();
    }

    // Fabric
    @Dynamic
    @Inject(method = { 
        CloudRenderInjection.MODERN_FABRIC_RENDER, 
        CloudRenderInjection.MODERN_FORGE_RENDER }, at = @At("INVOKE"), cancellable = true, require = 0)
    private void interceptCloudRender(
        CallbackInfo ci
    ) {
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
    private int forceAlphaCheckToPass(int originalColor) {
        return Math.max(1, ARGB.alpha(originalColor));
    }
}