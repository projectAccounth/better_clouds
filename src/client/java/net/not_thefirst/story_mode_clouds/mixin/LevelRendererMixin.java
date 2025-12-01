package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;

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
    private void initCloudRenderer(Minecraft minecraft,
		EntityRenderDispatcher entityRenderDispatcher,
		BlockEntityRenderDispatcher blockEntityRenderDispatcher,
		RenderBuffers renderBuffers,
		LevelRenderState levelRenderState,
		FeatureRenderDispatcher featureRenderDispatcher,
        CallbackInfo ci
    ) {
        this.cloudRenderer = new CustomCloudRenderer();
    }

    @Redirect(
        method = "method_62205",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;F)V"
        )
    )
    private void replaceCloudRender(
        CloudRenderer instance,
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Vec3 vec3,
        float partialTicks
    ) {
        renderCloud(cloudColor, status, cloudHeight, vec3, partialTicks);
    }

    @Inject(method = "method_62205", at = @At("HEAD"), cancellable = true)
    private void interceptCloudRender(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Vec3 vec3,
        float partialTicks,
        CallbackInfo ci
    ) {
        if (!Compat.hasSodium()) return;
        ci.cancel();

        renderCloud(cloudColor, status, cloudHeight, vec3, partialTicks);
    }

    private void renderCloud(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Vec3 vec3,
        float partialTicks
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.levelRenderer == null) return;

        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CustomCloudRenderer();
        }

        if (!CloudsConfiguration.INSTANCE.CLOUDS_RENDERED) return;

        this.cloudRenderer.render(cloudColor, status, cloudHeight, vec3, partialTicks);
    }
}