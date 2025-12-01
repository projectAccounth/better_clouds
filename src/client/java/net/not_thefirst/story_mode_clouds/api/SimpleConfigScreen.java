package net.not_thefirst.story_mode_clouds.api;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.not_thefirst.story_mode_clouds.gui.ColorPreviewBox;
import net.not_thefirst.story_mode_clouds.gui.NumericInputField;
import net.not_thefirst.story_mode_clouds.gui.ScrollArea;
import net.not_thefirst.story_mode_clouds.gui.SimpleSliderButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleConfigScreen extends Screen {
    private final Screen parent;
    private final List<Runnable> onCloseActions = new ArrayList<>();

    private ScrollArea area;

    public SimpleConfigScreen(Screen parent, String title) {
        super(Component.translatable(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // scrolling container for config widgets
        this.area = new ScrollArea(40, 40, this.width - 80, this.height - 80);
        this.addRenderableWidget(area);

        // Done button at bottom
        this.addRenderableWidget(
            Button.builder(Component.translatable("cloud_tweaks.done"), (btn) -> {
                // finalize numeric fields
                area.commitAll();
                // run custom save actions
                onCloseActions.forEach(Runnable::run);
                // return
                Minecraft.getInstance().setScreen(parent);

                Minecraft.getInstance().levelRenderer.getCloudRenderer().markForRebuild();
            }).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build()
        );
    }

    /** Adds a toggle button (label + toggle) */
    public void addToggle(String label, boolean initial, Consumer<Boolean> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int buttonWidth = 100;
            int labelWidth = rowWidth - buttonWidth - 6;

            // Label on left
            area.addLabel(Component.translatable(label), rowX, rowY + 6, 0xFFFFFF);

            // Toggle on right
            CycleButton<Boolean> button = CycleButton.onOffBuilder(initial)
                .create(rowX + labelWidth + 6, rowY, buttonWidth, 20,
                        Component.empty(), (btn, value) -> onChange.accept(value));
            area.addWidget(button);
        });
    }

    /** Adds a slider with numeric input */
    public void addSliderWithBox(String label, double initial, double min, double max, double step,
                                 Consumer<Double> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int boxWidth = 55;
            int gap = 6;

            int labelWidth = this.font.width(label) + 4;
            int sliderWidth = rowWidth - labelWidth - boxWidth - 2 * gap;

            // Label
            area.addLabel(Component.translatable(label), rowX, rowY + 6, 0xFFFFFF);

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

            // keep slider and box synced
            slider.onChange = slider.onChange.andThen(val -> box.setValue(String.format("%.2f", val)));
            box.onChange = box.onChange.andThen(val -> slider.setRealValue(val));

            area.addWidget(slider);
            area.addWidget(box);
            area.registerCommit(box::commit);
        });
    }

    /** Adds a simple slider (label + slider) */
    public void addSlider(String label, double initial, double min, double max, double step, Consumer<Double> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int labelWidth = this.font.width(label) + 4;
            int sliderWidth = rowWidth - labelWidth - 6;

            // Label
            area.addLabel(Component.translatable(label), rowX, rowY + 6, 0xFFFFFF);

            // Slider
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    public void addCategory(String text, HorizontalAlignment alignment) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int yOffset = 6; // vertical padding inside row

            // Compute x depending on alignment
            int labelX;
            switch (alignment) {
                case LEFT -> labelX = rowX;
                case CENTER -> labelX = rowX + rowWidth / 2 - Minecraft.getInstance().font.width(text) / 2;
                case RIGHT -> labelX = rowX + rowWidth - Minecraft.getInstance().font.width(text);
                default -> labelX = rowX;
            }

            area.addLabel(Component.translatable(text), labelX, rowY + yOffset, 0xFFFFFF);
        });
    }

    /** Adds a color picker row with a visual preview and R/G/B sliders */
    public void addColorPicker(String label, int initialColor, Consumer<Integer> onChange) {
        area.addRow((rowX, rowY, rowWidth) -> {
            int gap = 6;
            int previewSize = 20;
            int sliderWidth = (rowWidth - previewSize - 3 * gap) / 3;

            // Extract initial RGB
            int r = (initialColor >> 16) & 0xFF;
            int g = (initialColor >> 8) & 0xFF;
            int b = initialColor & 0xFF;

            // Color preview box
            ColorPreviewBox preview = new ColorPreviewBox(rowX, rowY, previewSize, previewSize, initialColor);
            area.addWidget(preview);

            // Holders for sliders so the lambda can reference them
            final SimpleSliderButton[] rSliderHolder = new SimpleSliderButton[1];
            final SimpleSliderButton[] gSliderHolder = new SimpleSliderButton[1];
            final SimpleSliderButton[] bSliderHolder = new SimpleSliderButton[1];

            // Lambda to update color
            Consumer<Double> updateColor = val -> {
                int newColor = ((int) rSliderHolder[0].getRealValue() << 16) |
                            ((int) gSliderHolder[0].getRealValue() << 8) |
                            ((int) bSliderHolder[0].getRealValue());
                preview.setColor(newColor);
                onChange.accept(newColor);
            };

            // R slider
            SimpleSliderButton rSlider = new SimpleSliderButton(
                rowX + previewSize + gap, rowY, sliderWidth, 20,
                "R", r, 0, 255, 1, updateColor
            );
            rSliderHolder[0] = rSlider;
            area.addWidget(rSlider);

            // G slider
            SimpleSliderButton gSlider = new SimpleSliderButton(
                rowX + previewSize + 2 * gap + sliderWidth, rowY, sliderWidth, 20,
                "G", g, 0, 255, 1, updateColor
            );
            gSliderHolder[0] = gSlider;
            area.addWidget(gSlider);

            // B slider
            SimpleSliderButton bSlider = new SimpleSliderButton(
                rowX + previewSize + 3 * gap + 2 * sliderWidth, rowY, sliderWidth, 20,
                "B", b, 0, 255, 1, updateColor
            );
            bSliderHolder[0] = bSlider;
            area.addWidget(bSlider);
        });
    }

    /** Simple enum for horizontal alignment */
    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }
}
