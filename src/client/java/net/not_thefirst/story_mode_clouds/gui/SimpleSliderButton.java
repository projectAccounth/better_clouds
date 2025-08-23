package net.not_thefirst.story_mode_clouds.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class SimpleSliderButton extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final double step;
    private final String label;
    public Consumer<Double> onChange;

    public SimpleSliderButton(int x, int y, int width, int height,
                              String label, double initialValue,
                              double min, double max, double step,
                              Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(),
              clamp01((initialValue - min) / (max - min)));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.onChange = onChange;

        updateMessage();
    }

    private static double clamp01(double v) {
        return Mth.clamp(v, 0.0, 1.0);
    }

    private static double snap(double v, double min, double max, double step) {
        double c = Mth.clamp(v, min, max);
        if (step > 0) c = Math.round(c / step) * step;
        return c;
    }

    private double toReal(double slider01) {
        return snap(min + slider01 * (max - min), min, max, step);
    }

    private double to01(double real) {
        return clamp01((snap(real, min, max, step) - min) / (max - min));
    }

    public double getRealValue() {
        return toReal(this.value);
    }

    @Override
    protected void updateMessage() {
        double val = getRealValue();
        this.setMessage(Component.literal(label + ": " + String.format("%.2f", val)));
    }

    @Override
    protected void applyValue() {
        // Snap & renormalize to avoid drift with tiny steps
        double real = getRealValue();
        this.value = to01(real);
        onChange.accept(real);
    }

    /** Programmatic setter in REAL units (does not override Mojang's normalized setter). */
    public void setRealValue(double realValue) {
        double snapped = snap(realValue, min, max, step);
        this.value = to01(snapped);
        updateMessage();
        onChange.accept(snapped);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.active && this.isHoveredOrFocused()) {
            double sliderPos = (mouseX - (this.getX() + 4)) / (this.width - 8);
            this.value = Mth.clamp(sliderPos, 0.0D, 1.0D);
            applyValue();
            updateMessage();
            return true;
        }
        return false;
    }

    // ⛔ Do NOT override onClick/onDrag/onRelease or setValue(double) here.
}
