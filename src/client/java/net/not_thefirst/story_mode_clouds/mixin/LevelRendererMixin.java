package net.not_thefirst.story_mode_clouds.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin {

    @Shadow @Final @Mutable
    private CloudRenderer cloudRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initCloudRenderer(
        Minecraft minecraft,
        EntityRenderDispatcher entityRenderDispatcher,
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        RenderBuffers renderBuffers,
        CallbackInfo ci
    ) {
        this.cloudRenderer = new CustomCloudRenderer();
    }

    @Redirect(
        method = "method_62205",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;F)V"
        )
    )
    private void replaceCloudRender(
        CloudRenderer instance,
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Matrix4f projMatrix,
        Matrix4f modelViewMatrix,
        Vec3 vec3,
        float partialTicks
    ) {
        renderCloud(cloudColor, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks);
    }

    @Inject(method = "method_62205", at = @At("HEAD"), cancellable = true)
    private void interceptCloudRender(
        ResourceHandle<RenderTarget> handle,
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Matrix4f projMatrix,
        Matrix4f modelViewMatrix,
        Vec3 vec3,
        float partialTicks,
        CallbackInfo ci
    ) {
        if (!Compat.hasSodium()) return; // only take over when Sodium is present
        ci.cancel();
        renderCloud(cloudColor, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks);
        // renderCloud(cloudColor, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks);
    }

    private void renderCloud(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Matrix4f projMatrix,
        Matrix4f modelViewMatrix,
        Vec3 vec3,
        float partialTicks
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.levelRenderer == null) return;

        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CustomCloudRenderer();
        }

        this.cloudRenderer.render(cloudColor, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks);
    }
}