package net.not_thefirst.story_mode_clouds.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TranslatableComponent;

public class ColorPreviewBox extends AbstractWidget {
    private int color;

    public ColorPreviewBox(int x, int y, int width, int height, int initialColor) {
        super(x, y, width, height, new TranslatableComponent(""));
        this.color = initialColor;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // Just draw a filled rectangle using AbstractGui#fill (inherited from GuiComponent)
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000 | this.color);
    }
}

