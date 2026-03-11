package net.not_thefirst.story_mode_clouds.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;

public class YACLPresetWidgets {
    private static String importBuffer = "";
    private static String exportedBase64 = "";
    private static Screen parentScreen = null;

    public static void setParentScreen(Screen screen) {
        parentScreen = screen;
    }

    public static Screen createPresetPreviewScreen(String presetId, Screen backScreen) {
        ConfigPresets.PresetMetadata metadata = ConfigPresets.getPresetMetadata(presetId);
        if (metadata == null) return null;

        CloudsConfiguration presetConfig = ConfigPresets.loadPresetConfiguration(presetId);
        if (presetConfig == null) return null;

        String displayName = metadata.displayName != null ? metadata.displayName : presetId;
        
        int layerCount = presetConfig.getLayerCount();
        float ambientLight = presetConfig.LIGHTING.AMBIENT_LIGHTING_STRENGTH;
        int cloudGridSize = presetConfig.CLOUD_GRID_SIZE;
        int maxLightCount = presetConfig.LIGHTING.lights.size();
        float maxLightingShading = presetConfig.LIGHTING.MAX_LIGHTING_SHADING;
        
        String presetBase64 = ConfigPresets.exportPresetAsBase64(presetId);
        
        var yaclBuilder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal("Preview: " + displayName))
            .category(ConfigCategory.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.preview.config"))
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.total_layers"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.total_layers.tooltip")))
                    .binding(
                        layerCount,
                        () -> layerCount,
                        v -> {}
                    )
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(layerCount, layerCount + 1).step(1))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.grid_size"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.grid_size.tooltip")))
                    .binding(
                        cloudGridSize,
                        () -> cloudGridSize,
                        v -> {}
                    )
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(cloudGridSize, cloudGridSize + 1).step(1))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.ambient"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.ambient.tooltip")))
                    .binding(
                        ambientLight,
                        () -> ambientLight,
                        v -> {}
                    )
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(ambientLight, ambientLight + 1.0f).step(0.1f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.max_shading"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.max_shading.tooltip")))
                    .binding(
                        maxLightingShading,
                        () -> maxLightingShading,
                        v -> {}
                    )
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(maxLightingShading, maxLightingShading + 1.0f).step(0.1f))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.lights"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.lights.tooltip")))
                    .binding(
                        maxLightCount,
                        () -> maxLightCount,
                        v -> {}
                    )
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(maxLightCount, maxLightCount + 1).step(1))
                    .build())
                
                .build())
            
            .category(ConfigCategory.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.preview.sharing"))
                
                .option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.export"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.export.tooltip")))
                    .binding(
                        presetBase64 != null ? presetBase64 : "",
                        () -> presetBase64 != null ? presetBase64 : "",
                        v -> {}
                    )
                    .controller(opt -> StringControllerBuilder.create(opt))
                    .build())
                
                .build())
            
            .category(ConfigCategory.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.preview.actions"))
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.presets.back"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.back.tooltip")))
                    .action((screen, btn) -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            mc.setScreen(backScreen);
                        }
                    })
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.preview.load"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.preview.load.tooltip")))
                    .action((screen, btn) -> {
                        if (ConfigPresets.loadPreset(presetId)) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("§aLoaded preset: " + displayName),
                                    false
                                );
                            }
                            if (mc != null) {
                                mc.setScreen(parentScreen);
                            }
                        }
                    })
                    .build())
                
                .build())
            
            .build();
        
        return new YACLScreen(yaclBuilder, backScreen);
    }

    public static ConfigCategory createPresetsCategory() {
        // Generate Base64 on initialization (how do I throttle this)
        String base64 = ConfigPresets.exportCurrentConfigAsBase64();
        if (base64 != null) {
            exportedBase64 = base64;
        }
        
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.presets"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.category.presets"));

        // Quick Actions section
        var quickActionsBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.quick_actions"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.quick_actions.tooltip")))
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.save_new"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_new.tooltip")))
                .action((yacl, btn) -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "Preset_" + timestamp;
                    if (ConfigPresets.savePreset(timestamp, presetName, "Saved via YACL")) {
                        CloudsConfiguration.save();
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.displayClientMessage(
                                ComponentWrapper.literal("§aSaved preset: " + presetName),
                                false
                            );
                        }
                        if (mc != null) {
                            mc.setScreen(parentScreen);
                        }
                    }
                })
                .build());

        builder.group(quickActionsBuilder.build());

        // current config export
        var exportBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.export"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export.tooltip")))
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.export_base64"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.export_base64.tooltip")))
                .binding(exportedBase64, () -> exportedBase64, v -> {}) 
                .controller(opt -> StringControllerBuilder.create(opt))
                .build());

        builder.group(exportBuilder.build());

        // b64 import
        var importBuilder = OptionGroup.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.import"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import.tooltip")))
            
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import_paste"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import_paste.tooltip")))
                .binding(importBuffer, () -> importBuffer, v -> importBuffer = v)
                .controller(opt -> StringControllerBuilder.create(opt))
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.import_button"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.import_button.tooltip")))
                .action((yacl, btn) -> {
                    if (importBuffer != null && !importBuffer.trim().isEmpty()) {
                        if (ConfigPresets.importPresetFromBase64WithAutoName(importBuffer)) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("§aPreset imported successfully"),
                                    false
                                );
                            }
                            importBuffer = "";
                            if (mc != null) {
                                mc.setScreen(parentScreen);
                            }
                        } else {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("§cFailed to import preset"),
                                    false
                                );
                            }
                        }
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.displayClientMessage(
                                ComponentWrapper.literal("§cPlease paste a Base64 string first"),
                                false
                            );
                        }
                    }
                })
                .build());

        builder.group(importBuilder.build());

        // Presets list - each preset gets its own top-level group
        String[] presetIds = ConfigPresets.listPresetIds().toArray(new String[0]);
        
        if (presetIds.length > 0) {
            // Separate presets by type
            java.util.List<String> myPresets = new java.util.ArrayList<>();
            java.util.List<String> importedPresets = new java.util.ArrayList<>();
            java.util.List<String> resourcePackPresets = new java.util.ArrayList<>();
            
            for (String presetId : presetIds) {
                ConfigPresets.PresetMetadata metadata = ConfigPresets.getPresetMetadata(presetId);
                if (metadata == null) continue;
                
                if (metadata.fromResourcePack) {
                    resourcePackPresets.add(presetId);
                } else if (presetId.startsWith("imported_") || (metadata.description != null && metadata.description.contains("Imported"))) {
                    importedPresets.add(presetId);
                } else {
                    myPresets.add(presetId);
                }
            }
            
            for (String presetId : myPresets) {
                createPresetGroup(builder, presetId, "My Preset");
            }
            
            for (String presetId : importedPresets) {
                createPresetGroup(builder, presetId, "Imported");
            }
            
            for (String presetId : resourcePackPresets) {
                createPresetGroup(builder, presetId, "Resource Pack");
            }
        } else {
            builder.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.no_presets"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.no_presets.tooltip")))
                .action((yacl, btn) -> {})
                .build());
        }

        return builder.build();
    }

    private static void createPresetGroup(ConfigCategory.Builder categoryBuilder, String presetId, String presetType) {
        ConfigPresets.PresetMetadata metadata = ConfigPresets.getPresetMetadata(presetId);
        if (metadata == null) return;

        String displayName = metadata.displayName != null ? metadata.displayName : presetId;
        String groupName = displayName + " [" + presetType + "]";
        
        var presetGroup = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(groupName))
            .description(OptionDescription.of(ComponentWrapper.literal(metadata.description != null ? metadata.description : "")));
        
        presetGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.name"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.name.tooltip")))
            .binding(
                displayName,
                () -> displayName,
                newName -> {
                    if (newName != null && !newName.isEmpty() && !newName.equals(displayName)) {
                        if (ConfigPresets.renamePreset(presetId, newName)) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("§aRenamed to: " + newName),
                                    false
                                );
                            }
                            if (mc != null) {
                                mc.setScreen(parentScreen);
                            }
                        }
                    }
                }
            )
            .controller(opt -> StringControllerBuilder.create(opt))
            .build());
        
        // Load button
        presetGroup.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.load"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.load.tooltip")))
            .action((yacl, btn) -> {
                if (ConfigPresets.loadPreset(presetId)) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.player != null) {
                        mc.player.displayClientMessage(
                            ComponentWrapper.literal("§aLoaded preset: " + displayName),
                            false
                        );
                    }
                    if (mc != null) {
                        mc.setScreen(parentScreen);
                    }

                    if (RendererHolder.get() != null) {
                        RendererHolder.get().markForRebuild();
                    }
                }
            })
            .build());

        // Preview button
        presetGroup.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.preview"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.preview.tooltip")))
            .action((yacl, btn) -> {
                Screen previewScreen = createPresetPreviewScreen(presetId, Minecraft.getInstance().screen);
                if (previewScreen != null) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.setScreen(previewScreen);
                    }
                }
            })
            .build());

        // Base64 String field
        presetGroup.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.base64"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.base64.tooltip")))
            .binding(
                ConfigPresets.exportPresetAsBase64(presetId) != null ? ConfigPresets.exportPresetAsBase64(presetId) : "",
                () -> ConfigPresets.exportPresetAsBase64(presetId) != null ? ConfigPresets.exportPresetAsBase64(presetId) : "",
                v -> {} 
            )
            .controller(opt -> StringControllerBuilder.create(opt))
            .build());

        // Duplicate preset
        presetGroup.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.duplicate"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.duplicate.tooltip")))
            .action((yacl, btn) -> {
                String newPresetId = presetId + "_copy_" + System.currentTimeMillis();
                String newPresetName = displayName + " (Copy)";
                if (ConfigPresets.copyPreset(presetId, newPresetId, newPresetName)) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.player != null) {
                        mc.player.displayClientMessage(
                            ComponentWrapper.literal("Copied preset: " + newPresetName),
                            false
                        );
                    }
                    if (mc != null) {
                        mc.setScreen(parentScreen);
                    }
                }
            })
            .build());

        // Delete preset
        presetGroup.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.delete"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.delete.tooltip")))
            .action((yacl, btn) -> {
                if (ConfigPresets.deletePreset(presetId)) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.player != null) {
                        mc.player.displayClientMessage(
                            ComponentWrapper.literal("Deleted preset: " + metadata.displayName),
                            false
                        );
                    }
                    if (mc != null) {
                        mc.setScreen(parentScreen);
                    }
                }
            })
            .build());
        
        categoryBuilder.group(presetGroup.build());
    }
}
