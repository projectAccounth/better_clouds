package net.not_thefirst.story_mode_clouds.api;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.not_thefirst.story_mode_clouds.gui.ColorPreviewBox;
import net.not_thefirst.story_mode_clouds.gui.NumericInputField;
import net.not_thefirst.story_mode_clouds.gui.ScrollArea;
import net.not_thefirst.story_mode_clouds.gui.SimpleSliderButton;
import net.not_thefirst.story_mode_clouds.gui.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

public class SimpleConfigScreen extends Screen {
    private final Screen parent;
    private final List<Runnable> onCloseActions = new ArrayList<>();

    private ScrollArea area;

    public SimpleConfigScreen(Screen parent, String titleKey) {
        super(new TranslatableComponent(titleKey));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // scrolling container for config widgets
        this.area = new ScrollArea(40, 40, this.width - 80, this.height - 80);
        this.addButton(area);

        // Done button at bottom
        this.addButton(new Button(
            this.width / 2 - 100, this.height - 28, 200, 20,
            new TranslatableComponent("cloud_tweaks.done"),
            (btn) -> {
                area.commitAll();
                onCloseActions.forEach(Runnable::run);
                this.minecraft.setScreen(parent);
            }
        ));
    }

    /** Adds a toggle (label + toggle) */
    public void addToggle(String labelKey, boolean initial, Consumer<Boolean> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int buttonWidth = 100;
            int labelWidth = rowWidth - buttonWidth - 6;

            // Label
            area.addLabel(new TranslatableComponent(labelKey), rowX, rowY + 6, 0xFFFFFF);

            ToggleButton toggle = new ToggleButton(rowX + labelWidth + 6, rowY, buttonWidth, 20, initial, onChange);
            area.addWidget(toggle);
        });
    }

    /** Adds a slider with numeric input */
    public void addSliderWithBox(String labelKey, double initial, double min, double max, double step,
                                 Consumer<Double> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int boxWidth = 55;
            int gap = 6;

            int labelWidth = this.font.width(labelKey) + 4;
            int sliderWidth = rowWidth - labelWidth - boxWidth - 2 * gap;

            // Label
            area.addLabel(new TranslatableComponent(labelKey), rowX, rowY + 6, 0xFFFFFF);

            // Slider
            SimpleSliderButton slider = new SimpleSliderButton(
                rowX + labelWidth + gap, rowY, sliderWidth, 20,
                "", initial, min, max, step, onChange
            );

            // Numeric box
            NumericInputField box = new NumericInputField(
                rowX + rowWidth - boxWidth, rowY, boxWidth, 20,
                initial, min, max, step, val -> {
                    onChange.accept(val);
                }
            );

            // Sync slider <-> box
            slider.onChange = slider.onChange.andThen(val -> box.setValue(String.format("%.2f", val)));
            box.onChange = box.onChange.andThen(val -> slider.setRealValue(val));

            area.addWidget(slider);
            area.addWidget(box);
            area.registerCommit(box::commit);
        });
    }

    /** Adds a simple slider (label + slider) */
    public void addSlider(String labelKey, double initial, double min, double max, double step, Consumer<Double> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int labelWidth = this.font.width(labelKey) + 4;
            int sliderWidth = rowWidth - labelWidth - 6;

            area.addLabel(new TranslatableComponent(labelKey), rowX, rowY + 6, 0xFFFFFF);

            area.addWidget(new SimpleSliderButton(
                rowX + labelWidth + 6, rowY, sliderWidth, 20,
                "", initial, min, max, step, onChange
            ));
        });
    }

    /** Run when "Done" is clicked */
    public void onCloseSave(Runnable saveAction) {
        this.onCloseActions.add(saveAction);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, 20, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, delta);
    }

    public void addCategory(String textKey, HorizontalAlignment alignment) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int yOffset = 6;
            int labelX;

            switch (alignment) {
                case LEFT: labelX = rowX; break;
                case CENTER: labelX = rowX + rowWidth / 2 - Minecraft.getInstance().font.width(textKey) / 2; break;
                case RIGHT: labelX = rowX + rowWidth - Minecraft.getInstance().font.width(textKey); break;
                default: labelX = rowX; break;
            }

            area.addLabel(new TranslatableComponent(textKey), labelX, rowY + yOffset, 0xFFFFFF);
        });
    }

    /** Adds a color picker with preview + R/G/B sliders */
    public void addColorPicker(String labelKey, int initialColor, Consumer<Integer> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int gap = 6;
            int previewSize = 20;
            int sliderWidth = (rowWidth - previewSize - 3 * gap) / 3;

            int r = (initialColor >> 16) & 0xFF;
            int g = (initialColor >> 8) & 0xFF;
            int b = initialColor & 0xFF;

            ColorPreviewBox preview = new ColorPreviewBox(rowX, rowY, previewSize, previewSize, initialColor);
            area.addWidget(preview);

            final SimpleSliderButton[] rSliderHolder = new SimpleSliderButton[1];
            final SimpleSliderButton[] gSliderHolder = new SimpleSliderButton[1];
            final SimpleSliderButton[] bSliderHolder = new SimpleSliderButton[1];

            Consumer<Double> updateColor = val -> {
                int newColor = ((int) rSliderHolder[0].getRealValue() << 16) |
                               ((int) gSliderHolder[0].getRealValue() << 8) |
                               ((int) bSliderHolder[0].getRealValue());
                preview.setColor(newColor);
                onChange.accept(newColor);
            };

            SimpleSliderButton rSlider = new SimpleSliderButton(
                rowX + previewSize + gap, rowY, sliderWidth, 20,
                "R", r, 0, 255, 1, updateColor
            );
            rSliderHolder[0] = rSlider;
            area.addWidget(rSlider);

            SimpleSliderButton gSlider = new SimpleSliderButton(
                rowX + previewSize + 2 * gap + sliderWidth, rowY, sliderWidth, 20,
                "G", g, 0, 255, 1, updateColor
            );
            gSliderHolder[0] = gSlider;
            area.addWidget(gSlider);

            SimpleSliderButton bSlider = new SimpleSliderButton(
                rowX + previewSize + 3 * gap + 2 * sliderWidth, rowY, sliderWidth, 20,
                "B", b, 0, 255, 1, updateColor
            );
            bSliderHolder[0] = bSlider;
            area.addWidget(bSlider);
        });
    }

    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }
}
