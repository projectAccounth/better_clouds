package net.not_thefirst.story_mode_clouds.config.state;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class RenderStatus {
    private RenderStatus() {}

    private static boolean renderFailed = false;

    public static boolean hasRenderFailed() {
        return renderFailed;
    }

    public static void onRenderFailed() {
        renderFailed = true;
    }

    public static void clearRenderFailed() {
        renderFailed = false;
    }

    public static boolean shouldRender() {
        return CloudsConfiguration.getInstance().CLOUDS_RENDERED && !renderFailed;
    }
}
