package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPreset;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets.LayerPresetMetadata;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import java.util.List;

public class LayerPresetsScreen {
    private LayerPresetsScreen() {}

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    public static Screen createLayerPresetsScreen(CloudsConfiguration.Dimension dimension, Screen backScreen) {
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen(dimension.getId().substring(0, 1).toUpperCase() + dimension.getId().substring(1), CloudsConfiguration::save);

        List<LayerPresetMetadata> presets = LayerPresets.getPresetsForDimension(dimension.getId());

        var category = backend.createCategory("cloudtweaks.presets.available_presets", "cloudtweaks.presets.available_presets.tooltip");
        
        category
            .action(backend.createAction(
                "cloudtweaks.presets.import",
                "cloudtweaks.presets.import.tooltip",
                () -> {
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
            );

        for (LayerPresetMetadata preset : presets) {
            var group = backend.createGroup(preset.displayName);

            group
                .action(backend.createAction(
                    "cloudtweaks.presets.load",
                    "cloudtweaks.presets.load.tooltip",
                    () -> {
                        LayerPreset loadedPreset = LayerPresets.loadLayerPreset(preset.id);
                        if (loadedPreset != null) {
                            loadedPreset.applyTo(config);
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(
                                    ComponentWrapper.translatable("cloudtweaks.message.loaded_layer_preset_prefix").getString() + preset.displayName
                                )
                            );
                            ClientHelper.setScreen(createLayerPresetsScreen(dimension, backScreen));
                        }
                    }
                ))
                .action(backend.createAction(
                    "cloudtweaks.presets.export",
                    "cloudtweaks.presets.export.tooltip",
                    () -> {
                        String base64 = LayerPresets.exportLayerPresetAsBase64(preset.id);
                        if (base64 != null) {
                            copyToClipboard(base64);
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(
                                    ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard").getString() + preset.displayName
                                )
                            );
                        }
                    }
                ))
                .action(backend.createAction(
                    "cloudtweaks.presets.edit",
                    "cloudtweaks.presets.edit.tooltip",
                    () -> {
                        LayerPreset loadedPreset = LayerPresets.loadLayerPreset(preset.id);
                        if (loadedPreset != null && loadedPreset.layer != null) {
                            Screen editScreen = DataSettings.createLayerPresetEditingScreen(
                                loadedPreset.layer, preset.id, dimension, backScreen
                            );
                            if (editScreen != null) {
                                ClientHelper.setScreen(editScreen);
                            }
                        }
                    }
                ))
                .action(backend.createAction(
                    "cloudtweaks.presets.delete",
                    "cloudtweaks.presets.delete.tooltip",
                    () -> {
                        if (LayerPresets.deleteLayerPreset(preset.id)) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(
                                    ComponentWrapper.translatable("cloudtweaks.message.deleted_layer_preset_prefix").getString() + preset.displayName
                                )
                            );
                            ClientHelper.setScreen(createLayerPresetsScreen(dimension, backScreen));
                        }
                    }
                ));

            category.group(group);
        }

        screenBuilder.category(category);
        return screenBuilder.build(backScreen);
    }

    private static void showImportDialog(String base64Data, CloudsConfiguration.Dimension dimension, Screen parentScreen) {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen("cloudtweaks.presets.import_title", () -> {});
        var importData = new Object() {
            String presetId = "imported_" + System.currentTimeMillis();
            String displayName = "Imported Layer";
            String description = "Layer preset imported from Base64";
        };

        var category = backend.createCategory("cloudtweaks.presets.import_details", null);

        category
            .option(backend.stringOption(
                "cloudtweaks.presets.import.preset_id",
                null,
                () -> importData.presetId,
                v -> importData.presetId = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.import.display_name",
                null,
                () -> importData.displayName,
                v -> importData.displayName = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.presets.import.description",
                null,
                () -> importData.description,
                v -> importData.description = v
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.import_button",
                "cloudtweaks.presets.import_button.tooltip",
                () -> {
                    boolean success = LayerPresets.importLayerPresetFromPayload(
                        importData.presetId,
                        importData.displayName,
                        importData.description,
                        base64Data,
                        dimension.getId()
                    );

                    if (success) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal(
                                ComponentWrapper.translatable("cloudtweaks.message.imported_layer_preset").getString() + importData.displayName
                            )
                        );
                        LoggerProvider.get().info("Imported preset: {} (ID: {})", importData.displayName, importData.presetId);
                        ClientHelper.setScreen(null);
                    }
                }
            ));

        screenBuilder.category(category);
        var screen = screenBuilder.build(parentScreen);
        ClientHelper.setScreen(screen);
    }
}