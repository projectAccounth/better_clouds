package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
@Mixin(CloudRenderer.class)
public class CloudRendererMixin {

    // Manual override for F/NF since lambda injection fails
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(CallbackInfo ci) {
        ci.cancel();

        CloudStatus cloudStatus = ClientHelper.getOptions().getCloudStatus();
        float partialTicks = ClientHelper.getClient().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = ClientHelper.CameraHelper.getCamera().position();
        
        
        RendererHolder.renderCloud(cloudStatus, camPos, partialTicks);
    }
}
