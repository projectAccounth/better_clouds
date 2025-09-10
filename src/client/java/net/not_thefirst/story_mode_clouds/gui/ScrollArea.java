package net.not_thefirst.story_mode_clouds.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ScrollArea extends AbstractScrollArea {
    private final List<AbstractWidget> children = new ArrayList<>();
    private final List<LabelEntry> labels = new ArrayList<>();
    private final List<Runnable> commits = new ArrayList<>();

    private int nextRowY = 5;  // vertical cursor for rows
    private final int rowSpacing = 24;
    private final int paddingX = 10;

    private AbstractWidget focusedChild = null;

    public ScrollArea(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    private double toLocalY(double globalY) {
        return globalY + scrollAmount();
    }

    /* --- Row system --- */
    @FunctionalInterface
    public interface RowBuilder {
        void build(int rowX, int rowY, int rowWidth);
    }

    public void addRow(RowBuilder builder) {
        int rowX = getX() + paddingX;
        int rowY = getY() + nextRowY;
        int rowWidth = this.width - 2 * paddingX;

        builder.build(rowX, rowY, rowWidth);
        nextRowY += rowSpacing;
    }

    public void addLabel(Component text, int x, int y, int color) {
        labels.add(new LabelEntry(text, x, y, color));
    }

    public void addWidget(AbstractWidget widget) {
        children.add(widget);
    }

    public void registerCommit(Runnable commit) {
        commits.add(commit);
    }

    public void commitAll() {
        commits.forEach(Runnable::run);
    }

    public int getRight() {
        return getX() + width;
    }

    public int getBottom() {
        return getY() + height;
    }

    /* --- Rendering & input --- */
    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        enableScissor(getX(), getY(), getRight(), getBottom());

        int offsetY = (int) scrollAmount();

        poseStack.pushPose();
        poseStack.translate(0, -offsetY, 0);

        for (LabelEntry lbl : labels) {
            Minecraft.getInstance().font.draw(poseStack, lbl.text, lbl.x, lbl.y, lbl.color);
        }

        for (AbstractWidget w : children) {
            w.render(poseStack, mouseX, mouseY + offsetY, delta);
        }

        poseStack.popPose();
        disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        double localY = toLocalY(my);
        for (AbstractWidget w : children) {
            if (((GuiEventListener) w).mouseClicked(mx, localY, btn)) {
                if (focusedChild != null) focusedChild.setFocused(false);
                focusedChild = w;
                focusedChild.setFocused(true);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        double localY = toLocalY(my);
        boolean consumed = false;
        for (AbstractWidget w : children) {
            if (((GuiEventListener) w).mouseReleased(mx, localY, btn)) consumed = true;
        }

        if (focusedChild != null && !(focusedChild instanceof NumericInputField)) {
            focusedChild.setFocused(false);
            focusedChild = null;
        }

        return consumed || super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        double localY = toLocalY(my);
        boolean consumed = false;
        for (AbstractWidget w : children) {
            if (((GuiEventListener) w).mouseDragged(mx, localY, btn, dx, dy)) consumed = true;
        }
        return consumed || super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (focusedChild != null) {
            boolean consumed = ((GuiEventListener) focusedChild).keyPressed(key, scancode, mods);

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedChild instanceof NumericInputField numeric) {
                    numeric.commit();
                }
                focusedChild.setFocused(false);
                focusedChild = null;
                return true;
            }

            return consumed;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (focusedChild != null && ((GuiEventListener) focusedChild).charTyped(chr, mods)) return true;
        return super.charTyped(chr, mods);
    }

    protected int contentHeight() {
        return nextRowY + 5;
    }

    @Override
    protected double scrollRate() {
        return 12.0;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    private record LabelEntry(Component text, int x, int y, int color) {}

    @Override
    protected int getInnerHeight() {
        return contentHeight();
    }

    @Override
    protected void renderContents(PoseStack poseStack, int i, int j, float f) {
        renderWidget(poseStack, i, j, f);
    }

    @Override
    protected boolean scrollbarVisible() {
        return false;
    }
}
