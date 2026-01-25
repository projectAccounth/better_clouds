package net.not_thefirst.story_mode_clouds.renderer;

import java.util.Optional;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.math.Texture;

public class RendererHolder {
    private static CustomCloudRenderer renderer = new CustomCloudRenderer();

    private RendererHolder() {}

    public static CustomCloudRenderer get() {
        return RendererHolder.renderer;
    }

    public static void renderCloud(
        CloudStatus status,
        Vec3 vec3,
        float partialTicks
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.levelRenderer == null) return;

        if (renderer == null) {
            renderer = new CustomCloudRenderer();
        }

        if (!renderer.getCurrentTexture().isPresent()) {
            Optional<Texture.TextureData> texture = renderer.prepare(client.getResourceManager(), 
                Profiler.get(), CustomCloudRenderer.TEXTURE_LOCATION);
            renderer.apply(texture, client.getResourceManager(), Profiler.get());
        }

        if (!CloudsConfiguration.getInstance().CLOUDS_RENDERED) return;

        renderer.render(status, vec3, partialTicks);
    }
}
