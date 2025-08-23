package net.not_thefirst.story_mode_clouds.gui;

import net.minecraft.client.gui.GuiGraphics;
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
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF000000 | color);
        // Border
        // gfx.draw(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
