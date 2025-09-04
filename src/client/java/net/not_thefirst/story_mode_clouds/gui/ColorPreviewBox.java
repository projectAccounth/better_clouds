package net.not_thefirst.story_mode_clouds.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ColorPreviewBox extends AbstractWidget {
    private int color;

    public ColorPreviewBox(int x, int y, int width, int height, int initialColor) {
        super(x, y, width, height, Component.empty());
        this.color = initialColor;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public void renderWidget(PoseStack gfx, int mouseX, int mouseY, float delta) {
        fill(gfx, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000 | this.color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
