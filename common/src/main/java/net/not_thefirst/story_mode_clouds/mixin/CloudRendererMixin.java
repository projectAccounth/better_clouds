package net.not_thefirst.story_mode_clouds.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void interceptRender(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Matrix4f projMatrix,
        Matrix4f modelViewMatrix,
        Vec3 vec3,
        float partialTicks,
        CallbackInfo ci
    ) {
        ci.cancel();

        RendererHolder.renderCloud(cloudColor, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks);
    }
}
