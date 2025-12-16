package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;

import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.CloudRendererHolder;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.resource.ResourceHandle;

@Mixin(value = LevelRenderer.class, priority = 16384)
public abstract class LevelRendererMixin implements CloudRendererHolder {

    @Override
    public CustomCloudRenderer getCustomCloudRenderer() {
        return RendererHolder.get();
    }

    // Fabric
    @Dynamic
    @Inject(method = "method_62205", at = @At("INVOKE"), cancellable = true, require = 0)
    private void interceptCloudRender(
        ResourceHandle<?> instance,
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

    @Dynamic
    @Inject(method = { "lambda$addCloudsPass$6" }, 
    at = 
        @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;F)V"
        ), 
        cancellable = true, require = 0
    )
    public void interceptCloudRenderForge(
        ResourceHandle<?> renderer, 
        float partialTicks, 
        Vec3 cam, 
        Matrix4f projMatrix, 
        Matrix4f modelView, 
        int color,
        CloudStatus status, 
        float cloudHeight, 
        CallbackInfo ci
    ) {
        ci.cancel();

        RendererHolder.renderCloud(color, status, cloudHeight, projMatrix, modelView, cam, partialTicks);
    }
}