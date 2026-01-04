package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.CloudRendererHolder;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin implements CloudRendererHolder {

    @Override
    public CustomCloudRenderer getCustomCloudRenderer() {
        return RendererHolder.get();
    }

    @Dynamic
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void interceptCloudRender(
        PoseStack poseStack,
        Matrix4f modelViewMatrix,
        float partialTicks,
        double cameraX, double cameraY, double cameraZ,
        CallbackInfo ci
    ) {
        ci.cancel();

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        CloudStatus status = client.options.getCloudsType();
        float cloudHeight  = client.level.effects().getCloudHeight();

        if (Float.isNaN(cloudHeight)) return;

        Vec3 cam = new Vec3(cameraX, cameraY, cameraZ);

        RendererHolder.renderCloud(0, status, cloudHeight, modelViewMatrix, cam, partialTicks, poseStack);
    }
}