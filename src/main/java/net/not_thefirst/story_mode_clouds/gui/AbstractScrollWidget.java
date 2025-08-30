package net.not_thefirst.story_mode_clouds.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractScrollWidget extends AbstractWidget {
    protected int scrollAmount;
    protected boolean scrolling;

    public AbstractScrollWidget(int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        boolean inside = this.isMouseOver(mouseX, mouseY);
        boolean scrollbar = this.scrollbarVisible() &&
            mouseX >= this.x + this.width - 8 && mouseX < this.x + this.width &&
            mouseY >= this.y && mouseY < this.y + this.height;

        if (scrollbar && button == 0) {
            this.scrolling = true;
            return true;
        }
        return inside || scrollbar;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.visible && this.isFocused() && this.scrolling) {
            if (mouseY < this.y) {
                this.setScrollAmount(0);
            } else if (mouseY > this.y + this.height) {
                this.setScrollAmount(this.getMaxScrollAmount());
            } else {
                int barHeight = this.getScrollBarHeight();
                double step = Math.max(1, (double)this.getMaxScrollAmount() / (this.height - barHeight));
                this.setScrollAmount((int)(this.scrollAmount + dy * step));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.setScrollAmount(this.scrollAmount - (int)(delta * this.scrollRate()));
        return true;
    }

    protected void setScrollAmount(int amt) {
        this.scrollAmount = Mth.clamp(amt, 0, this.getMaxScrollAmount());
    }

    protected int getScrollBarHeight() {
        return Mth.clamp((this.height * this.height) / this.getContentHeight(), 32, this.height);
    }

    protected int getMaxScrollAmount() {
        return Math.max(0, this.getContentHeight() - this.height);
    }

    /** Total height of scrollable content */
    protected abstract int getContentHeight();

    /** Pixels per scroll wheel notch */
    protected abstract double scrollRate();

    /** Render actual inner contents */
    protected abstract void renderContents(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, int scrollY);

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        // draw background
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF202020);

        // clip manually (no scissor in 1.16)
        poseStack.pushPose();
        poseStack.translate(0, -this.scrollAmount, 0);
        renderContents(poseStack, mouseX, mouseY, partialTicks, this.scrollAmount);
        poseStack.popPose();

        // draw scrollbar if needed
        if (this.scrollbarVisible()) {
            int barHeight = this.getScrollBarHeight();
            int barTop = (int)((double)this.scrollAmount * (this.height - barHeight) / this.getMaxScrollAmount()) + this.y;
            fill(poseStack, this.x + this.width - 6, barTop, this.x + this.width - 2, barTop + barHeight, 0xFFAAAAAA);
        }
    }

    protected boolean scrollbarVisible() {
        return this.getContentHeight() > this.height;
    }
}
