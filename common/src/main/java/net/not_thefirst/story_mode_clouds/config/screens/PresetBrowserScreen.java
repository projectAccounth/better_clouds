package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.glfw.ClipboardUtils;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

public class PresetBrowserScreen {
    private PresetBrowserScreen() {}
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static Screen createBrowserScreen(PresetController.PresetCategory category, Screen backScreen) {
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.presets"))
            .save(CloudsConfiguration::save);
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets"));
        
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        
        switch (category) {
            case COLORS:
                for (CloudColorPreset preset : PresetController.listColorPresets()) {
                    addColorPresetGroup(categoryBuilder, preset, config, backScreen);
                }
                break;
            case LIGHTING:
                for (LightingPreset preset : PresetController.listLightingPresets()) {
                    addLightingPresetGroup(categoryBuilder, preset, config, backScreen);
                }
                break;
            case LIGHT_SOURCES:
                for (LightSourcesPreset preset : PresetController.listLightSourcesPresets()) {
                    addLightSourcesPresetGroup(categoryBuilder, preset, config, backScreen);
                }
                break;
        }
        
        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.import"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import.tooltip")))
            .action((yacl, btn) -> {
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
            })
            .build());

        categoryBuilder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.back"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.back.tooltip")))
            .action((yacl, btn) -> ClientHelper.setScreen(backScreen))
            .build());
        
        builder.category(categoryBuilder.build());
        return new YACLScreen(builder.build(), backScreen);
    }
    
    private static void addColorPresetGroup(ConfigCategory.Builder categoryBuilder, CloudColorPreset preset, CloudsConfiguration config, Screen backScreen) {
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.information")))
            ;
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.preview"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.preview.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                    PresetController.PresetCategory.COLORS, preset.id, backScreen
                )))
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.export"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export_base64.tooltip")))
            .action((yacl, btn) -> {
                String base64 = PresetController.exportColorPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            })
            .build());

        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.edit"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.edit.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                    PresetController.PresetCategory.COLORS, preset.id, yacl
                )))
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.load_preset_action"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.load.color.tooltip")))
            .action((yacl, btn) -> {
                if (PresetController.loadColorPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();

                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.loaded_color_preset_prefix")
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.delete_preset_action"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.delete_preset_action")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteColorPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.deleted_color_preset_prefix")
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.COLORS, backScreen));
                    }
                })
                .build());
        }
        
        categoryBuilder.group(group.build());
    }
    
    private static void addLightingPresetGroup(ConfigCategory.Builder categoryBuilder, LightingPreset preset, CloudsConfiguration config, Screen backScreen) {
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.information")))
            ;
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.preview"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.preview.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                    PresetController.PresetCategory.LIGHTING, preset.id, backScreen
                ))
            )
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.export"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export_base64.tooltip")))
            .action((yacl, btn) -> {
                String base64 = PresetController.exportLightingPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            })
            .build());

        group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.edit"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.edit.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                    PresetController.PresetCategory.LIGHTING, preset.id, yacl
                )))
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.load_preset_action"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.load.lighting.tooltip")))
            .action((yacl, btn) -> {
                if (PresetController.loadLightingPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.loaded_lighting_preset_prefix")
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.delete_preset_action"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.delete_preset_action")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteLightingPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.deleted_lighting_preset_prefix")
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHTING, backScreen));
                    }
                })
                .build());
        }
        
        categoryBuilder.group(group.build());
    }
    
    private static void addLightSourcesPresetGroup(ConfigCategory.Builder categoryBuilder, LightSourcesPreset preset, CloudsConfiguration config, Screen backScreen) {
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.information")))
            ;
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.preview"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.preview.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createEditorScreen(
                        PresetController.PresetCategory.LIGHT_SOURCES, preset.id, backScreen
                    )))
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.export"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export_base64.tooltip")))
            .action((yacl, btn) -> {
                String base64 = PresetController.exportLightSourcesPresetAsBase64(preset.id);
                if (base64 != null) {
                    copyToClipboard(base64);
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.presets.exported_to_clipboard")
                    );
                }
            })
            .build());

        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.edit"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.edit.tooltip")))
            .action((yacl, btn) ->
                ClientHelper.setScreen(PresetEditorScreen.createPresetEditingScreen(
                    PresetController.PresetCategory.LIGHT_SOURCES, preset.id, yacl
                )))
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.load_preset_action"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.load.light_sources.tooltip")))
            .action((yacl, btn) -> {
                if (PresetController.loadLightSourcesPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.loaded_light_sources_preset_prefix")
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.delete_preset_action"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.delete_preset_action")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteLightSourcesPreset(preset.id)) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.deleted_light_sources_preset_prefix")
                        );
                        ClientHelper.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHT_SOURCES, backScreen));
                    }
                })
                .build());
        }
        
        categoryBuilder.group(group.build());
    }
    
    private static String formatCategoryName(PresetController.PresetCategory category) {
        return switch (category) {
            case COLORS -> "Cloud Colors";
            case LIGHTING -> "Lighting";
            case LIGHT_SOURCES -> "Light Sources";
        };
    }

    private static void copyToClipboard(String text) {
        try {
            ClipboardUtils.setClipboard(ClientHelper.getGLFWHandle(), text);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to copy to clipboard: {}", e.getMessage());
        }
    }

    private static void showImportDialog(PresetController.PresetCategory category, String payload, Screen parentScreen) {
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.presets.import_title"))
            .save(() -> {});

        var importData = new Object() {
            String presetId = "imported_" + System.currentTimeMillis();
            String displayName = "Imported Preset";
            String description = "Imported preset from clipboard";
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
                })
                .build());

        builder.category(categoryBuilder.build());
        ClientHelper.setScreen(new YACLScreen(builder.build(), parentScreen));
    }
}
