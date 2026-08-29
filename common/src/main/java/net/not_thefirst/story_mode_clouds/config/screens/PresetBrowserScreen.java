package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

public class PresetBrowserScreen {
    private PresetBrowserScreen() {}

    public static Screen createBrowserScreen(PresetController.PresetCategory category, Screen backScreen) {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen("cloudtweaks.presets", CloudsConfiguration::save);
        var categoryBuilder = backend.createCategory("cloudtweaks.presets", null);
        var config = CloudsConfiguration.getInstance();
        final Screen[] self = new Screen[1];
        
        switch (category) {
            case COLORS:
                for (CloudColorPreset preset : PresetController.listColorPresets()) {
                    addColorPresetGroup(categoryBuilder, preset, config, backScreen, self);
                }
                break;
            case LIGHTING:
                for (LightingPreset preset : PresetController.listLightingPresets()) {
                    addLightingPresetGroup(categoryBuilder, preset, config, backScreen, self);
                }
                break;
            case LIGHT_SOURCES:
                for (LightSourcesPreset preset : PresetController.listLightSourcesPresets()) {
                    addLightSourcesPresetGroup(categoryBuilder, preset, config, backScreen, self);
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
                            showImportDialog(category, clipboard, backScreen);
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
            () -> ClientHelper.setScreen(backScreen)
        ));

        screenBuilder.category(categoryBuilder);
        var screen = screenBuilder.build(backScreen);
        self[0] = screen;
        return screen;
    }
    
    private static void addColorPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, CloudColorPreset preset, CloudsConfiguration config, Screen backScreen, Screen[] self) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                PresetController.PresetCategory.COLORS, preset.id, self[0]
            ))
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
            () -> ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                PresetController.PresetCategory.COLORS, preset.id, self[0]
            ))
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.color.tooltip",
            () -> {
                if (PresetController.loadColorPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
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
                    if (PresetController.deleteColorPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal(
                                ComponentWrapper.translatable("cloudtweaks.message.deleted_color_preset_prefix").getString() + preset.displayName
                            )
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.COLORS, backScreen));
                    }
                }
            ));
        }

        categoryBuilder.group(group);
    }
    
    private static void addLightingPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightingPreset preset, CloudsConfiguration config, Screen backScreen, Screen[] self) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                PresetController.PresetCategory.LIGHTING, preset.id, self[0]
            ))
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
            () -> ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                PresetController.PresetCategory.LIGHTING, preset.id, self[0]
            ))
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.lighting.tooltip",
            () -> {
                if (PresetController.loadLightingPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
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
                    if (PresetController.deleteLightingPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal(
                                ComponentWrapper.translatable("cloudtweaks.message.deleted_lighting_preset_prefix").getString() + preset.displayName
                            )
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHTING, backScreen));
                    }
                }
            ));
        }

        categoryBuilder.group(group);
    }
    
    private static void addLightSourcesPresetGroup(ConfigBackend.ConfigCategoryBuilder categoryBuilder, LightSourcesPreset preset, CloudsConfiguration config, Screen backScreen, Screen[] self) {
        var backend = BackendHolder.getBackend();
        var group = backend.createGroup(preset.displayName)
            .description("cloudtweaks.presets.information");

        group.action(backend.createAction(
            "cloudtweaks.presets.preview",
            "cloudtweaks.presets.preview.tooltip",
            () -> ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                PresetController.PresetCategory.LIGHT_SOURCES, preset.id, self[0]
            ))
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
            () -> ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                PresetController.PresetCategory.LIGHT_SOURCES, preset.id, self[0]
            ))
        ));

        group.action(backend.createAction(
            "cloudtweaks.option.load_preset_action",
            "cloudtweaks.presets.load.light_sources.tooltip",
            () -> {
                if (PresetController.loadLightSourcesPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
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
                    if (PresetController.deleteLightSourcesPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.deleted_light_sources_preset_prefix")
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHT_SOURCES, backScreen));
                    }
                }
            ));
        }

        categoryBuilder.group(group);
    }

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    private static void showImportDialog(PresetController.PresetCategory category, String payload, Screen parentScreen) {
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
                    ClientHelper.setScreen(createBrowserScreen(category, parentScreen));
                }
            }
        ));

        screenBuilder.category(categoryBuilder);
        ClientHelper.setScreen(screenBuilder.build(parentScreen));
    }
}
