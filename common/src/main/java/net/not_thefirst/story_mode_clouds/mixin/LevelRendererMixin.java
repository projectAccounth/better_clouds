package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;

import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin {
    @Dynamic
    @Inject(method = "renderClouds", at = @At("INVOKE"), cancellable = true, require = 0)
    private void interceptCloudRender(
        CallbackInfo ci
    ) {
        ci.cancel();

        Minecraft client = Minecraft.getInstance();
        CloudStatus cloudStatus = client.options.getCloudsType();
        float partialTicks = client.getFrameTime();
        Vec3 camPos = client.gameRenderer.getMainCamera().getPosition();

        RendererHolder.renderCloud(cloudStatus, camPos, partialTicks);
    }
}