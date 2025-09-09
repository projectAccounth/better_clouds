package net.not_thefirst.story_mode_clouds.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class GUIUtils {
    public static void DrawCenteredString(PoseStack poseStack, Font font, Component component, int i, int j, int k) {
        String str = component.getString();
        font.drawShadow(poseStack, str, (float)(i - font.width(str) / 2), (float)j, k);
    }

    public static void DrawTextShadowed(PoseStack poseStack, Component text, float x, float y, int color) {
        Minecraft.getInstance().font.drawShadow(poseStack, text.getString(), x, y, color);
    }
}
