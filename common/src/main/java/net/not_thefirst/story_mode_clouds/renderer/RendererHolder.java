package net.not_thefirst.story_mode_clouds.renderer;

import net.minecraft.client.CloudStatus;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

public class RendererHolder {
    private static CustomCloudRenderer renderer;

    private RendererHolder() {}

    public static CustomCloudRenderer get() {
        return RendererHolder.renderer;
    }

    public static void renderCloud(
        CloudStatus status,
        Vec3 vec3,
        float partialTicks
    ) {
        if (ClientHelper.getLevel() == null || ClientHelper.getLevelRenderer() == null) return;

        if (renderer == null) {
            renderer = new CustomCloudRenderer();
        }

        if (!CloudsConfiguration.getInstance().CLOUDS_RENDERED) return;

        renderer.render(status, vec3, partialTicks);
    }
}
