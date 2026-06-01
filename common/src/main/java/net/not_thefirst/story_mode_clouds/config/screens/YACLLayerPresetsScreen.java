package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.config.YACLDataSettings;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration;
import net.not_thefirst.story_mode_clouds.config.screens.LayerPresets.LayerPresetMetadata;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class YACLLayerPresetsScreen {
    private YACLLayerPresetsScreen() {}

    private static void copyToClipboard(String text) {
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    public static Screen createLayerPresetsScreen(CloudsConfiguration.Dimension dimension, Screen backScreen) {
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal("Layer Presets - " + dimension.getId().substring(0, 1).toUpperCase() + dimension.getId().substring(1)))
            .save(CloudsConfiguration::save);

        List<LayerPresetMetadata> presets = LayerPresets.getPresetsForDimension(dimension.getId());

        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.literal("Available Presets"));

        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Import from Base64"))
            .description(OptionDescription.of(ComponentWrapper.literal("Paste a Base64 encoded layer preset")))
            .action((yacl, btn) -> {
                try {
                    String clipboard = (String) java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (clipboard != null && !clipboard.isEmpty()) {
                        showImportDialog(clipboard, dimension, backScreen);
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
                .name(ComponentWrapper.literal("Load"))
                .description(OptionDescription.of(ComponentWrapper.literal("Load this preset")))
                .action((yacl, btn) -> {
                    LayerConfiguration loadedLayer = LayerPresets.loadLayerPreset(preset.id);
                    if (loadedLayer != null) {
                        // Create a new layer from preset
                        LayerConfiguration newLayer = new LayerConfiguration(
                            config.getLayerCount(dimension)
                        );
                        newLayer.copy(loadedLayer);
                        config.getLayerHolder(dimension).addLayer(newLayer);
                        CloudsConfiguration.save();
                        
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.sendSystemMessage(
                                ComponentWrapper.literal("Loaded layer preset: " + preset.displayName)
                            );
                        }
                        // Refresh the presets screen (don't know if ts works)
                        if (mc != null) {
                            mc.setScreen(createLayerPresetsScreen(dimension, backScreen));
                        }
                    }
                })
                .build());

            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Export"))
                .description(OptionDescription.of(ComponentWrapper.literal("Copy to clipboard as Base64")))
                .action((yacl, btn) -> {
                    String base64 = LayerPresets.exportLayerPresetAsBase64(preset.id);
                    if (base64 != null) {
                        copyToClipboard(base64);
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.sendSystemMessage(
                                ComponentWrapper.literal("Preset exported to clipboard")
                            );
                        }
                    }
                })
                .build());

            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Modify"))
                .description(OptionDescription.of(ComponentWrapper.literal("Edit this preset")))
                .action((yacl, btn) -> {
                    LayerConfiguration loadedLayer = LayerPresets.loadLayerPreset(preset.id);
                    if (loadedLayer != null) {
                        Screen editScreen = YACLDataSettings.createLayerPresetEditingScreen(
                            loadedLayer, preset.id, dimension, backScreen
                        );
                        if (editScreen != null) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null) {
                                mc.setScreen(editScreen);
                            }
                        }
                    }
                })
                .build());

            // Fnatic manager here.
            presetGroup.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Delete"))
                .description(OptionDescription.of(ComponentWrapper.literal("Delete this preset")))
                .action((yacl, btn) -> {
                    if (LayerPresets.deleteLayerPreset(preset.id)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.sendSystemMessage(
                                ComponentWrapper.literal("Deleted layer preset: " + preset.displayName)
                            );
                        }
                        if (mc != null) {
                            mc.setScreen(createLayerPresetsScreen(dimension, backScreen));
                        }
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
            .title(ComponentWrapper.literal("Import Layer Preset"))
            .save(() -> {});

        var importData = new Object() {
            String presetId = "imported_" + System.currentTimeMillis();
            String displayName = "Imported Layer";
            String description = "Layer preset imported from Base64";
        };

        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.literal("Import Details"))

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.literal("Preset ID"))
                .binding(importData.presetId, () -> importData.presetId, v -> importData.presetId = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.literal("Display Name"))
                .binding(importData.displayName, () -> importData.displayName, v -> importData.displayName = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.literal("Description"))
                .binding(importData.description, () -> importData.description, v -> importData.description = v)
                .controller(StringControllerBuilder::create)
                .build())

            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Import"))
                .description(OptionDescription.of(ComponentWrapper.literal("Import this preset")))
                .action((yacl, btn) -> {
                    boolean success = LayerPresets.importLayerPresetFromBase64(
                        importData.presetId,
                        importData.displayName,
                        importData.description,
                        base64Data,
                        dimension.getId()
                    );

                    Minecraft mc = Minecraft.getInstance();
                    if (success && mc != null && mc.player != null) {
                        mc.player.sendSystemMessage(
                            ComponentWrapper.literal("Layer preset imported successfully")
                        );
                        if (mc != null) {
                            mc.setScreen(null);
                        }
                    }
                })
                .build());

        builder.category(categoryBuilder.build());
        builder.build().generateScreen(parentScreen).onClose();
    }
}
