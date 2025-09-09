package net.not_thefirst.story_mode_clouds.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.PoseStack;

// --- scroll area implementation ---
public class ScrollArea extends AbstractScrollWidget {
    private final List<AbstractWidget> children = new ArrayList<>();
    private final List<LabelEntry> labels = new ArrayList<>();
    private final List<Runnable> commits = new ArrayList<>();

    private int nextRowY = 5;  // vertical cursor for rows
    private final int rowSpacing = 24;
    private final int paddingX = 10;

    private AbstractWidget focusedChild = null;

    public ScrollArea(int x, int y, int width, int height) {
        super(x, y, width, height, new TranslatableComponent(""));
    }

    private double toLocalY(double globalY) {
        return globalY + scrollRate();
    }

    /* --- Row system --- */
    @FunctionalInterface
    public interface RowBuilder {
        void build(int rowX, int rowY, int rowWidth);
    }

    /** Adds a new row of widgets using a builder function */
    public void addRow(RowBuilder builder) {
        int rowX = x + paddingX;
        int rowY = y + nextRowY;
        int rowWidth = this.width - 2 * paddingX;

        builder.build(rowX, rowY, rowWidth);

        nextRowY += rowSpacing;
    }

    /** Adds a label (drawn manually, not a widget) */
    public void addLabel(Component text, int x, int y, int color) {
        labels.add(new LabelEntry(text, x, y, color));
    }

    /** Adds a widget at explicit coords (relative to scroll area) */
    public void addWidget(AbstractWidget widget) {
        children.add(widget);
    }

    /** Register commit action for e.g. text boxes */
    public void registerCommit(Runnable commit) {
        commits.add(commit);
    }

    public void commitAll() {
        commits.forEach(Runnable::run);
    }

    /* --- Rendering & input --- */
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        // background
        fill(poseStack, x, y, x + width, y + height, 0xFF202020);

        // clip to scroll area
        enableScissor(this.x, this.y, this.width, this.height);

        poseStack.pushPose();
        poseStack.translate(0, -this.scrollAmount, 0);

        // labels
        for (LabelEntry lbl : labels) {
            GUIUtils.DrawTextShadowed(poseStack, lbl.text, lbl.x, lbl.y, lbl.color);
        }

        // children
        for (AbstractWidget w : children) {
            w.render(poseStack, mouseX, mouseY + this.scrollAmount, partialTicks);
        }

        poseStack.popPose();

        disableScissor();

        // scrollbar
        if (scrollbarVisible()) {
            int barHeight = getScrollBarHeight();
            int barTop = (int)((double)this.scrollAmount * (this.height - barHeight) / getMaxScrollAmount()) + this.y;
            fill(poseStack, this.x + this.width - 6, barTop, this.x + this.width - 2, barTop + barHeight, 0xFFAAAAAA);
        }
    }

    private static void enableScissor(int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int fbHeight = mc.getWindow().getHeight();

        // Convert GUI coords → framebuffer coords (OpenGL origin = bottom-left)
        int scissorX = (int)(x * scale);
        int scissorY = (int)((mc.getWindow().getGuiScaledHeight() - (y + h)) * scale);
        int scissorW = (int)(w * scale);
        int scissorH = (int)(h * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }

    private static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) return false;

        double localY = my + this.scrollAmount;
        for (AbstractWidget w : children) {
            if (w.mouseClicked(mx, localY, btn)) {
                if (focusedChild != null) focusedChild.changeFocus(false);
                focusedChild = w;
                focusedChild.changeFocus(true);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) return false;

        double localY = my + this.scrollAmount;
        boolean consumed = false;
        for (AbstractWidget w : children) {
            if (w.mouseReleased(mx, localY, btn)) consumed = true;
        }
        if (focusedChild != null && !(focusedChild instanceof NumericInputField)) {
            focusedChild.changeFocus(false);
            focusedChild = null;
        }
        return consumed || super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!isMouseOver(mx, my)) return false;
        
        double localY = my + this.scrollAmount;
        boolean consumed = false;
        for (AbstractWidget w : children) {
            if (w.mouseDragged(mx, localY, btn, dx, dy)) consumed = true;
        }
        return consumed || super.mouseDragged(mx, my, btn, dx, dy);
    }


    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (focusedChild != null) {
            boolean consumed = focusedChild.keyPressed(key, scancode, mods);

            // check Enter
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedChild instanceof NumericInputField) {
                    NumericInputField numeric = (NumericInputField) focusedChild;
                    numeric.commit();               // snap to valid number
                }
                focusedChild.changeFocus(false);    // remove focus
                focusedChild = null;
                return true;
            }

            return consumed;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (focusedChild != null && focusedChild.charTyped(chr, mods)) return true;
        return super.charTyped(chr, mods);
    }

    protected int contentHeight() {
        return nextRowY + 5;  // total height used by rows
    }

    protected double scrollRate() {
        return 12.0;
    }

    private class LabelEntry {
        public Component text; 
        public int x, y; 
        public int color;
        public LabelEntry(Component text, int x, int y, int color) {
            this.text = text; this.x = x; this.y = y; this.color = color;
        }
    }

    @Override
    protected int getContentHeight() {
        return contentHeight();
    }

    @Override
    protected void renderContents(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, int scrollY) {
        // Labels
        for (LabelEntry lbl : labels) {
            Minecraft.getInstance().font.draw(poseStack, lbl.text, lbl.x, lbl.y, lbl.color);
        }

        // Children
        for (AbstractWidget w : children) {
            w.render(poseStack, mouseX, mouseY + scrollY, partialTicks);
        }
    }
}
