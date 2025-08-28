package net.not_thefirst.story_mode_clouds.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.function.Consumer;

public class NumericInputField extends EditBox {
    private final double min;
    private final double max;
    private final double step;
    public Consumer<Double> onChange;

    private Double lastValidValue;

    public NumericInputField(int x, int y, int width, int height,
                             double initialValue, double min, double max, double step,
                             Consumer<Double> onChange) {
        super(Minecraft.getInstance().font, x, y, width, height, new TranslatableComponent(""));
        this.min = min;
        this.max = max;
        this.step = step;
        this.onChange = onChange;

        double clamped = clampStep(initialValue);
        this.lastValidValue = clamped;
        super.setValue(format(clamped));
    }

    private double parseValue(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return lastValidValue != null ? lastValidValue : min;
        }
    }

    private double clampStep(double val) {
        double clamped = Mth.clamp(val, min, max);
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
        }
        return clamped;
    }

    private String format(double val) {
        return String.format("%.2f", val);
    }

    private void updateValue() {
        String text = getValue();
        if (text == null || text.isEmpty() || "-".equals(text) || text.endsWith(".")) {
            // Allow user to be in the middle of typing, don’t clamp yet
            return;
        }

        double parsed = clampStep(parseValue(text));
        if (!Objects.equals(parsed, lastValidValue)) {
            lastValidValue = parsed;
            onChange.accept(parsed);
        }
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        updateValue();
    }

    @Override
    public void setValue(String text) {
        super.setValue(text);
        updateValue();
    }

    public void commit() {
        // Call this on "Done" or when focus is lost, to snap to valid number
        double parsed = clampStep(parseValue(getValue()));
        lastValidValue = parsed;
        super.setValue(format(parsed));
        onChange.accept(parsed);
    }

    @Override
    public void setFocused(boolean focused) {
        boolean was = this.isFocused();
        super.setFocused(focused);
        if (was && !focused) {
            commit(); // snap & notify on blur
        }
    }
}
