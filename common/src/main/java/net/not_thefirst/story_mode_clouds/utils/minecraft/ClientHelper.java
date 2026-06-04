package net.not_thefirst.story_mode_clouds.utils.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientHelper {
    private ClientHelper() {}

    public static void sendLocalSystemMessage(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(message);
        }
    }
}
