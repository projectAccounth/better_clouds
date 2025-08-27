package net.not_thefirst.story_mode_clouds.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

// --- scroll area implementation ---
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

    /** Adds a new row of widgets using a builder function */
    public void addRow(RowBuilder builder) {
        int rowX = getX() + paddingX;
        int rowY = getY() + nextRowY;
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
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        gfx.enableScissor(getX(), getY(), getRight(), getBottom());

        int offsetY = (int) scrollAmount();

        gfx.pose().pushMatrix();
        gfx.pose().translate(0, -offsetY);

        // Render labels
        for (LabelEntry lbl : labels) {
            gfx.drawString(Minecraft.getInstance().font,
                           lbl.text, lbl.x, lbl.y, lbl.color);
        }

        // Render widgets
        for (AbstractWidget w : children) {
            w.render(gfx, mouseX, mouseY + offsetY, delta);
        }

        gfx.pose().popMatrix();
        gfx.disableScissor();

        renderScrollbar(gfx);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        double localY = toLocalY(my);
        for (AbstractWidget w : children) {
            if (w.mouseClicked(mx, localY, btn)) {
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
            if (w.mouseReleased(mx, localY, btn)) consumed = true;
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
                if (focusedChild instanceof NumericInputField numeric) {
                    numeric.commit();               // snap to valid number
                }
                focusedChild.setFocused(false);    // remove focus
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


    @Override
    protected int contentHeight() {
        return nextRowY + 5;  // total height used by rows
    }

    @Override
    protected double scrollRate() {
        return 12.0;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    /* --- internal --- */
    private record LabelEntry(Component text, int x, int y, int color) {}
}
