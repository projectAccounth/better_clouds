package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPreset;
import net.not_thefirst.story_mode_clouds.config.presets.controllers.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.controllers.LayerPresets.LayerPresetMetadata;
import net.not_thefirst.story_mode_clouds.config.screens.DataSettings;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayerPresetsScreenManager extends ScreenManager {
    private static final Map<CloudsConfiguration.Dimension, LayerPresetsScreenManager> instances = new HashMap<>();
    
    private final CloudsConfiguration.Dimension dimension;
    private final CloudsConfiguration config;

    private LayerPresetsScreenManager(CloudsConfiguration.Dimension dimension) {
        super();
        this.dimension = dimension;
        this.config = CloudsConfiguration.getInstance();
    }

    public static LayerPresetsScreenManager getInstance(CloudsConfiguration.Dimension dimension) {
        return instances.computeIfAbsent(dimension, d -> new LayerPresetsScreenManager(d));
    }

    public static void clearInstance(CloudsConfiguration.Dimension dimension) {
        instances.remove(dimension);
    }

    public static void clearAll() {
        instances.clear();
    }

    static Screen createLayerPresetEditingScreen(CloudsConfiguration.LayerConfiguration layer, String presetId, 
                                                         CloudsConfiguration.Dimension dimension, Screen backScreen) {
        var backend = BackendHolder.getBackend();

        return backend.createScreen("cloudtweaks.presets.edit_layer_preset", 
            () -> LayerPresets.saveLayerPreset(
                presetId,
                layer.NAME,
                ComponentWrapper.translatable("cloudtweaks.presets.modified_layer_preset").getString(),
                layer,
                dimension.getId()
            ))
            .category(DataSettings.buildLayerBasicSettings(layer))
            .category(DataSettings.buildLayerAppearanceSettings(layer))
            .category(DataSettings.buildOutlineCategory(layer))
            .category(DataSettings.buildTextureCategory(layer))
            .category(DataSettings.buildLayerFogSettings(layer))
            .category(DataSettings.buildLayerFadeSettings(layer))
            .category(DataSettings.buildLayerPresetSaveCategory(layer, presetId, dimension, backScreen))
            .build(backScreen);
    }

    @Override
    public Screen buildScreen() {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen(
            dimension.getId().substring(0, 1).toUpperCase() + dimension.getId().substring(1),
            CloudsConfiguration::save
        );

        List<LayerPresetMetadata> presets = LayerPresets.getPresetsForDimension(dimension.getId());
        var category = backend.createCategory("cloudtweaks.presets.available_presets", "cloudtweaks.presets.available_presets.tooltip");
        
        category.action(backend.createAction(
            "cloudtweaks.presets.import",
            "cloudtweaks.presets.import.tooltip",
            () -> {
                try {
                    String clipboard = ClipboardUtils.getClipboard(ClientHelper.getGLFWHandle());
                    if (clipboard != null && !clipboard.isEmpty()) {
                        if (LayerPresets.validateLayerPresetPayload(clipboard)) {
                            showImportDialog(clipboard);
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
                            // Refresh the layer settings manager with the updated config
                            LayerSettingsScreenManager layerMgr = LayerSettingsScreenManager.getInstance(dimension);
                            ClientHelper.setScreen(layerMgr.refreshScreen());
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
                            Screen editScreen = createLayerPresetEditingScreen(
                                loadedPreset.layer, preset.id, dimension, currentScreen
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
                            ClientHelper.setScreen(refreshScreen());
                        }
                    }
                ));

            category.group(group);
        }

        screenBuilder.category(category);
        currentScreen = screenBuilder.build(parentScreen);
        return currentScreen;
    }

    private void showImportDialog(String base64Data) {
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
                        ClientHelper.setScreen(refreshScreen());
                    }
                }
            ));

        screenBuilder.category(category);
        var screen = screenBuilder.build(currentScreen);
        ClientHelper.setScreen(screen);
    }

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }
}
