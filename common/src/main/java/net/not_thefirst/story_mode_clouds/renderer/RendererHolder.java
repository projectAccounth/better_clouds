package net.not_thefirst.story_mode_clouds.renderer;

import java.util.Optional;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.ProgramManager;
import net.not_thefirst.story_mode_clouds.utils.Texture;

public class RendererHolder {
    private static CustomCloudRenderer renderer = new CustomCloudRenderer();

    public static CustomCloudRenderer get() {
        return RendererHolder.renderer;
    }

    public static void renderCloud(
        int cloudColor,
        CloudStatus status,
        float cloudHeight,
        Vec3 vec3,
        float partialTicks,
        PoseStack poseStack
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.levelRenderer == null) return;

        if (renderer == null) {
            renderer = new CustomCloudRenderer();
        }

        if (!renderer.currentTexture.isPresent()) {
            Optional<Texture.TextureData> texture = renderer.prepare(client.getResourceManager(), 
                client.getProfiler(), CustomCloudRenderer.TEXTURE_LOCATION);
            renderer.apply(texture, client.getResourceManager(), client.getProfiler());
        }

        if (!CloudsConfiguration.INSTANCE.CLOUDS_RENDERED) return;

        renderer.render(cloudColor, status, cloudHeight, vec3, partialTicks, poseStack);
    }
}
