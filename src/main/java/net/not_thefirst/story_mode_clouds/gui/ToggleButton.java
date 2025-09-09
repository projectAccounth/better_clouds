package net.not_thefirst.story_mode_clouds.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.Consumer;

public class ToggleButton extends AbstractWidget {
    private boolean value;
    private final Consumer<Boolean> onToggle;

    public ToggleButton(int x, int y, int width, int height, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, width, height, new TranslatableComponent(""));
        this.value = initial;
        this.onToggle = onToggle;
        updateMessage();
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean newValue) {
        this.value = newValue;
        updateMessage();
        onToggle.accept(this.value);
    }

    private void updateMessage() {
        // Shows "ON"/"OFF" text
        this.setMessage(new TranslatableComponent(value ? "options.on" : "options.off"));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setValue(!value);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        int color = this.isHovered ? 0xFFFFFFA0 : 0xFFFFFFFF;

        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height,
                this.value ? 0xFF228833 : 0xFF883333);

        // Draw centered ON/OFF text
        GUIUtils.DrawCenteredString(poseStack, mc.font, this.getMessage(),
                this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
    }
}
