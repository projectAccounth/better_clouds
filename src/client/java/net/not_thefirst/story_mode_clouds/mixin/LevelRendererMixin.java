package net.not_thefirst.story_mode_clouds.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.not_thefirst.story_mode_clouds.utils.CloudRendererHolder;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin implements CloudRendererHolder {

    private CustomCloudRenderer cloudRenderer;

    @Override
    public CustomCloudRenderer getCloudRenderer() {
        return this.cloudRenderer;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeConstructor(
        Minecraft minecraft,
        EntityRenderDispatcher entityRenderDispatcher,
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        RenderBuffers renderBuffers,
        CallbackInfo ci
    ) {
        this.cloudRenderer = new CustomCloudRenderer();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void interceptCloudRender(
        PoseStack poseStack,
        Matrix4f projMatrix,
        Matrix4f modelViewMatrix,
        float partialTicks,
        double cameraX, double cameraY, double cameraZ,
        CallbackInfo ci
    ) {
        ci.cancel();

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if (this.cloudRenderer != null && this.cloudRenderer.currentTexture.isEmpty()) {
            var texture = this.cloudRenderer.prepare(client.getResourceManager(), client.getProfiler());
            this.cloudRenderer.apply(texture, client.getResourceManager(), client.getProfiler());
        }

        // === Gather vanilla inputs ===
        CloudStatus status = client.options.getCloudsType();
        float cloudHeight  = client.level.effects().getCloudHeight();

        // Vanilla cloud color (Vec3) → ARGB int
        Vec3 vanillaColor = client.level.getCloudColor(partialTicks);
        int color = ARGB.colorFromFloat(
            1.0f,
            (float)vanillaColor.x,
            (float)vanillaColor.y,
            (float)vanillaColor.z
        );

        Vec3 cam = new Vec3(cameraX, cameraY, cameraZ);

        renderCloud(poseStack, color, cloudHeight, status, projMatrix, modelViewMatrix, cam, partialTicks);
    }

    private void renderCloud(
        PoseStack poseStack,
        int color,
        float cloudHeight,
        CloudStatus status,
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

        if (!CloudsConfiguration.INSTANCE.CLOUDS_RENDERED) return;

        this.cloudRenderer.render(color, status, cloudHeight, projMatrix, modelViewMatrix, vec3, partialTicks, poseStack);
    }
}