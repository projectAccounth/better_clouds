package net.not_thefirst.story_mode_clouds.utils.minecraft;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

public class ClientHelper {
    private ClientHelper() {}

    public static void sendLocalSystemMessage(Component message) {
        if (getClient().player != null) {
            getClient().player.sendSystemMessage(message);
        }
    }

    public static void setScreen(Screen screen) {
        if (screen == null) return;
        getClient().gui.setScreen(screen);
    }

    public static Screen getCurrentScreen() {
        if (getClient().gui == null) return null;
        return getClient().gui.screen();
    }

    public static Minecraft getClient() {
        return Minecraft.getInstance();
    }

    public static Options getOptions() {
        return getClient().options;
    }

    public static ClientLevel getLevel() {
        return getClient().level;
    }

    public static LevelRenderer getLevelRenderer() {
        return getClient().levelRenderer;
    }

    public static class GameRendererHelper {
        private GameRendererHelper() {}

        public static GameRenderer getRenderer() {
            return getClient().gameRenderer;
        }

        public static Camera getCamera() {
            if (getRenderer() == null) return null;
            return getRenderer().mainCamera();
        }
    }

    public static class LevelRendererHelper {
        private LevelRendererHelper() {}

        public static LevelRenderer getRenderer() {
            return getClient().levelRenderer;
        }
    }

    public static class CameraHelper {
        private CameraHelper() {}

        public static Camera getCamera() {
            GameRenderer renderer = GameRendererHelper.getRenderer();
            if (renderer == null) return null;
            return renderer.mainCamera();
        }
    }

    public static class ResourceHelper {
        private ResourceHelper() {}

        public static ResourceManager getResourceManager() {
            return ClientHelper.getClient().getResourceManager();
        }
    }
}
