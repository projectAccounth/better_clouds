package net.not_thefirst.story_mode_clouds.renderer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class RendererHolder {
    private static CustomCloudRenderer renderer = new CustomCloudRenderer();

    public static CustomCloudRenderer get() {
        return RendererHolder.renderer;
    }

    public static void renderCloud(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Matrix4f modelViewMatrix,
        Vec3 vec3,
        float partialTicks,
        PoseStack poseStack
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.levelRenderer == null) return;

        if (renderer == null) {
            renderer = new CustomCloudRenderer();
        }

        if (renderer.currentTexture.isEmpty()) {
            var texture = renderer.prepare(client.getResourceManager(), 
                client.getProfiler(), CustomCloudRenderer.TEXTURE_LOCATION);
            renderer.apply(texture, client.getResourceManager(), client.getProfiler());
        }

        if (!CloudsConfiguration.INSTANCE.CLOUDS_RENDERED) return;

        renderer.render(cloudColor, status, cloudHeight, modelViewMatrix, vec3, partialTicks, poseStack);
    }
}
