package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.not_thefirst.story_mode_clouds.config.presets.controllers.PresetController;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import java.util.HashMap;
import java.util.Map;

public class PresetBrowserScreenManager extends ScreenManager {
    private static final Map<PresetController.PresetCategory, PresetBrowserScreenManager> instances = new HashMap<>();
    
    private final PresetController.PresetCategory category;
    private final CloudsConfiguration config;

    private PresetBrowserScreenManager(PresetController.PresetCategory category) {
        super();
        this.category = category;
        this.config = CloudsConfiguration.getInstance();
    }

    public static PresetBrowserScreenManager getInstance(PresetController.PresetCategory category) {
        return instances.computeIfAbsent(category, c -> new PresetBrowserScreenManager(c));
    }

    public static void clearInstance(PresetController.PresetCategory category) {
        instances.remove(category);
    }

    public static void clearAll() {
        instances.clear();
    }

    @Override
    public Screen buildScreen() {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen("cloudtweaks.presets", CloudsConfiguration::save);
        var categoryBuilder = backend.createCategory("cloudtweaks.presets", null);
        
        switch (category) {
            case COLORS:
                for (CloudColorPreset preset : PresetController.listColorPresets()) {
                    addColorPresetGroup(categoryBuilder, preset);
                }
                break;
            case LIGHTING:
                for (LightingPreset preset : PresetController.listLightingPresets()) {
                    addLightingPresetGroup(categoryBuilder, preset);
                }
                break;
            case LIGHT_SOURCES:
                for (LightSourcesPreset preset : PresetController.listLightSourcesPresets()) {
                    addLightSourcesPresetGroup(categoryBuilder, preset);
                }
                break;
        }
        
        categoryBuilder.action(backend.createAction(
            "cloudtweaks.presets.import",
            "cloudtweaks.presets.import.tooltip",
            () -> {
                try {
                    String clipboard = ClipboardUtils.getClipboard(ClientHelper.getGLFWHandle());
                    if (clipboard != null && !clipboard.isEmpty()) {
                        if (PresetController.validatePresetPayload(category, clipboard)) {
                            showImportDialog(clipboard);
                        } else {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal("Invalid clipboard payload for the selected preset category.")
                            );
                        }
                    }
                } catch (Exception e) {
                    LoggerProvider.get().error("Failed to read import payload from clipboard: {}", e.getMessage());
                }
            }
        ));

        categoryBuilder.action(backend.createAction(
            "cloudtweaks.presets.back",
            "cloudtweaks.presets.back.tooltip",
            () -> ClientHelper.setScreen(parentScreen)
        ));

        screenBuilder.category(categoryBuilder);
        currentScreen = screenBuilder.build(parentScreen);
        return currentScreen;
    }

    private void addColorPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, CloudColorPreset preset) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditorScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.presets.export",
            "cloudtweaks.presets.export_base64.tooltip",
            () -> {
                String base64 = PresetController.exportColorPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.edit",
            "cloudtweaks.option.edit.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditingScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.color.tooltip",
            () -> {
                if (PresetController.loadColorPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal(
                            ComponentWrapper.translatable("cloudtweaks.message.loaded_color_preset_prefix").getString() + preset.displayName
                        )
                    );
                }
            }
        ));

        if (!preset.id.equals("default")) {
            group.action(backend.createAction(
                "cloudtweaks.option.delete_preset_action",
                "cloudtweaks.tooltip.delete_preset_action",
                () -> {
                    showDeleteConfirmation(preset.displayName, () -> {
                        if (PresetController.deleteColorPreset(preset.id)) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(
                                    ComponentWrapper.translatable("cloudtweaks.message.deleted_color_preset_prefix").getString() + preset.displayName
                                )
                            );
                            ClientHelper.setScreen(refreshScreen());
                        }
                    });
                }
            ));
        }

        categoryBuilder.group(group);
    }

    private void addLightingPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightingPreset preset) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditorScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.presets.export",
            "cloudtweaks.presets.export_base64.tooltip",
            () -> {
                String base64 = PresetController.exportLightingPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.edit",
            "cloudtweaks.option.edit.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditingScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.lighting.tooltip",
            () -> {
                if (PresetController.loadLightingPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal(
                            ComponentWrapper.translatable("cloudtweaks.message.loaded_lighting_preset_prefix").getString() + preset.displayName
                        )
                    );
                }
            }
        ));

        if (!preset.id.equals("default")) {
            group.action(backend.createAction(
                "cloudtweaks.option.delete_preset_action",
                "cloudtweaks.tooltip.delete_preset_action",
                () -> {
                    showDeleteConfirmation(preset.displayName, () -> {
                        if (PresetController.deleteLightingPreset(preset.id)) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(
                                    ComponentWrapper.translatable("cloudtweaks.message.deleted_lighting_preset_prefix").getString() + preset.displayName
                                )
                            );
                            ClientHelper.setScreen(refreshScreen());
                        }
                    });
                }
            ));
        }

        categoryBuilder.group(group);
    }

    private void addLightSourcesPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightSourcesPreset preset) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditorScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.presets.export",
            "cloudtweaks.presets.export_base64.tooltip",
            () -> {
                String base64 = PresetController.exportLightSourcesPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.edit",
            "cloudtweaks.option.edit.tooltip",
            () -> {
                PresetEditorScreenManager editorMgr = PresetEditorScreenManager.getInstance(category, preset.id);
                editorMgr.setParentScreen(currentScreen);
                ClientHelper.setScreen(editorMgr.buildEditingScreen());
            }
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.light_sources.tooltip",
            () -> {
                if (PresetController.loadLightSourcesPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal(
                            ComponentWrapper.translatable("cloudtweaks.message.loaded_light_sources_preset_prefix").getString() + preset.displayName
                        )
                    );
                }
            }
        ));

        if (!preset.id.equals("default")) {
            group.action(backend.createAction(
                "cloudtweaks.option.delete_preset_action",
                "cloudtweaks.tooltip.delete_preset_action",
                () -> {
                    showDeleteConfirmation(preset.displayName, () -> {
                        if (PresetController.deleteLightSourcesPreset(preset.id)) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.translatable("cloudtweaks.message.deleted_light_sources_preset_prefix")
                            );
                            ClientHelper.setScreen(refreshScreen());
                        }
                    });
                }
            ));
        }

        categoryBuilder.group(group);
    }

    private void showImportDialog(String payload) {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen("cloudtweaks.presets.import_title", () -> {});

        var importData = new Object() {
            String presetId = "imported_" + System.currentTimeMillis();
            String displayName = "Imported Preset";
            String description = "Imported preset from clipboard";
        };

        var categoryBuilder = backend.createCategory("cloudtweaks.presets.import_details", null)
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
            ));

        categoryBuilder.action(backend.createAction(
            "cloudtweaks.presets.import_button",
            "cloudtweaks.presets.import_button.tooltip",
            () -> {
                boolean success = switch (category) {
                    case COLORS -> PresetController.importColorPresetFromPayload(importData.presetId, importData.displayName, importData.description, payload);
                    case LIGHTING -> PresetController.importLightingPresetFromPayload(importData.presetId, importData.displayName, importData.description, payload);
                    case LIGHT_SOURCES -> PresetController.importLightSourcesPresetFromPayload(importData.presetId, importData.displayName, importData.description, payload);
                };

                if (success) {
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.imported_preset")
                    );
                    ClientHelper.setScreen(refreshScreen());
                }
            }
        ));

        screenBuilder.category(categoryBuilder);
        ClientHelper.setScreen(screenBuilder.build(currentScreen));
    }

    private void showDeleteConfirmation(String presetName, Runnable deleteAction) {
        ClientHelper.setScreen(ScreenManager.buildDeleteConfirmationScreen(
            presetName,
            currentScreen,
            deleteAction
        ));
    }

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }
}
