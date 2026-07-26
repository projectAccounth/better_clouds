package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPreset;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets.LayerPresetMetadata;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import dev.isxander.yacl3.gui.YACLScreen;
import java.util.List;

public class YACLLayerPresetsScreen {
    private YACLLayerPresetsScreen() {}

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    public static Screen createLayerPresetsScreen(CloudsConfiguration.Dimension dimension, Screen backScreen) {
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal(dimension.getId().substring(0, 1).toUpperCase() + dimension.getId().substring(1)))
            .save(CloudsConfiguration::save);

        List<LayerPresetMetadata> presets = LayerPresets.getPresetsForDimension(dimension.getId());

        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.available_presets"));

        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.import"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import.tooltip")))
                .action((yacl, btn) -> {
                    try {
                        String clipboard = ClipboardUtils.getClipboard(ClientHelper.getGLFWHandle());
                        if (clipboard != null && !clipboard.isEmpty()) {
                            if (LayerPresets.validateLayerPresetPayload(clipboard)) {
                                showImportDialog(clipboard, dimension, backScreen);
                            } else {
                                ClientHelper.sendLocalSystemMessage(
                                    ComponentWrapper.literal("Invalid clipboard payload for a layer preset.")
                                );
                            }
                        }
                    } catch (Exception e) {
                        LoggerProvider.get().info("Failed to read from clipboard: {}", e.getMessage());
                    }
                })
            .build());

        for (LayerPresetMetadata preset : presets) {
            var presetGroup = OptionGroup.createBuilder()
                .name(ComponentWrapper.literal(preset.displayName))
                .description(OptionDescription.of(ComponentWrapper.literal(preset.description)));

            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.load_preset_action"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.load.color.tooltip")))
                .action((yacl, btn) -> {
                    LayerPreset loadedPreset = LayerPresets.loadLayerPreset(preset.id);
                    if (loadedPreset != null) {
                        loadedPreset.applyTo(config);
                        CloudsConfiguration.save();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.loaded_layer_preset_prefix")
                        );
                        ClientHelper.setScreen(createLayerPresetsScreen(dimension, backScreen));
                    }
                })
                .build());

            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.export"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export_base64.tooltip")))
                .action((yacl, btn) -> {
                    String base64 = LayerPresets.exportLayerPresetAsBase64(preset.id);
                    if (base64 != null) {
                        copyToClipboard(base64);
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                        );
                    }
                })
                .build());

            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.edit"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.edit.tooltip")))
                .action((yacl, btn) -> {
                    LayerPreset loadedPreset = LayerPresets.loadLayerPreset(preset.id);
                    if (loadedPreset != null && loadedPreset.layer != null) {
                        Screen editScreen = YACLDataSettings.createLayerPresetEditingScreen(
                            loadedPreset.layer, preset.id, dimension, backScreen
                        );
                        if (editScreen != null) {
                            ClientHelper.setScreen(editScreen);
                        }
                    }
                })
                .build());

            // Fnatic manager here.
            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.delete"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.delete.tooltip")))
                .action((yacl, btn) -> {
                    if (LayerPresets.deleteLayerPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.deleted_layer_preset_prefix")
                        );
                        ClientHelper.setScreen(createLayerPresetsScreen(dimension, backScreen));
                    }
                })
                .build());

            categoryBuilder.group(presetGroup.build());
        }

        builder.category(categoryBuilder.build());
        return builder.build().generateScreen(backScreen);
    }

    private static void showImportDialog(String base64Data, CloudsConfiguration.Dimension dimension, Screen parentScreen) {
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.presets.import_title"))
            .save(() -> {});

        var importData = new Object() {
            String presetId = "imported_" + System.currentTimeMillis();
            String displayName = "Imported Layer";
            String description = "Layer preset imported from Base64";
        };

        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.import_details"))

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import.preset_id"))
                .binding(importData.presetId, () -> importData.presetId, v -> importData.presetId = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import.display_name"))
                .binding(importData.displayName, () -> importData.displayName, v -> importData.displayName = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import.description"))
                .binding(importData.description, () -> importData.description, v -> importData.description = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import_button"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import_button.tooltip")))
                .action((yacl, btn) -> {
                    boolean success = LayerPresets.importLayerPresetFromPayload(
                        importData.presetId,
                        importData.displayName,
                        importData.description,
                        base64Data,
                        dimension.getId()
                    );

                    if (success) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.imported_layer_preset")
                        );
                        LoggerProvider.get().info("Imported preset: {} (ID: {})", importData.displayName, importData.presetId);
                        ClientHelper.setScreen(null);
                    }
                })
                .build());

        builder.category(categoryBuilder.build());
        ClientHelper.setScreen(new YACLScreen(builder.build(), parentScreen));
    }
}
