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

@Mixin(value = LevelRenderer.class, priority = 16384, remap = false)
public abstract class LevelRendererMixin {

    @Inject(method = "renderClouds", at = @At("INVOKE"), cancellable = true, require = 0)
    private void interceptCloudRender(
        CallbackInfo ci
    ) {
        if (!CloudsConfiguration.getInstance().CLOUDS_RENDERED) 
            return;
        ci.cancel();

        CloudStatus cloudStatus = ClientHelper.getOptions().getCloudsType();
        float partialTicks = ClientHelper.getClient().getFrameTime();
        Vec3 camPos = ClientHelper.CameraHelper.getCamera().getPosition();

        RendererHolder.renderCloud(cloudStatus, camPos, partialTicks);
    }
}