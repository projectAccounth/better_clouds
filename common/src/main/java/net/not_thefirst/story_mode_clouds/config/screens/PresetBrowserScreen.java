package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ComponentWrapper;
import net.not_thefirst.story_mode_clouds.config.presets.*;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

public class PresetBrowserScreen {
    private PresetBrowserScreen() {}
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static Screen createBrowserScreen(PresetController.PresetCategory category, Screen backScreen) {
        var builder = YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.literal(formatCategoryName(category) + " - Presets"))
            .save(CloudsConfiguration::save);
        
        var categoryBuilder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.literal(formatCategoryName(category)));
        
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
            .name(ComponentWrapper.literal("Back"))
            .description(OptionDescription.of(ComponentWrapper.literal("Return to presets menu")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) mc.setScreen(backScreen);
            })
            .build());
        
        builder.category(categoryBuilder.build());
        return new YACLScreen(builder.build(), backScreen);
    }
    
    private static void addColorPresetGroup(ConfigCategory.Builder categoryBuilder, CloudColorPreset preset, CloudsConfiguration config, Screen backScreen) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.literal(
                "Description: " + preset.description + "\n" +
                "Last modified: " + lastMod + "\n" +
                "ID: " + preset.id
            )));
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("View"))
            .description(OptionDescription.of(ComponentWrapper.literal("View preset details")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createEditorScreen(
                        PresetController.PresetCategory.COLORS, preset.id, backScreen
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Edit"))
            .description(OptionDescription.of(ComponentWrapper.literal("Edit preset name and description")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createMetadataEditScreen(
                        PresetController.PresetCategory.COLORS, preset.id, yacl
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Load"))
            .description(OptionDescription.of(ComponentWrapper.literal("Load this color preset")))
            .action((yacl, btn) -> {
                if (PresetController.loadColorPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();

                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal("§aLoaded color preset: " + preset.displayName)
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Delete"))
                .description(OptionDescription.of(ComponentWrapper.literal("Delete this preset")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteColorPreset(preset.id)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal("§aDeleted color preset: " + preset.displayName)
                            );
                            mc.setScreen(createBrowserScreen(PresetController.PresetCategory.COLORS, backScreen));
                        }
                    }
                })
                .build());
        }
        
        categoryBuilder.group(group.build());
    }
    
    private static void addLightingPresetGroup(ConfigCategory.Builder categoryBuilder, LightingPreset preset, CloudsConfiguration config, Screen backScreen) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.literal(
                "Description: " + preset.description + "\n" +
                "Last modified: " + lastMod + "\n" +
                "ID: " + preset.id
            )));
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("View"))
            .description(OptionDescription.of(ComponentWrapper.literal("View preset details")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createEditorScreen(
                        PresetController.PresetCategory.LIGHTING, preset.id, backScreen
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Edit"))
            .description(OptionDescription.of(ComponentWrapper.literal("Edit preset name and description")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createMetadataEditScreen(
                        PresetController.PresetCategory.LIGHTING, preset.id, yacl
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Load"))
            .description(OptionDescription.of(ComponentWrapper.literal("Load this lighting preset")))
            .action((yacl, btn) -> {
                if (PresetController.loadLightingPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal("Loaded lighting preset: " + preset.displayName)
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Delete"))
                .description(OptionDescription.of(ComponentWrapper.literal("Delete this preset")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteLightingPreset(preset.id)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            ClientHelper.sendLocalSystemMessage((
                                ComponentWrapper.literal("Deleted lighting preset: " + preset.displayName)
                            ));
                            mc.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHTING, backScreen));
                        }
                    }
                })
                .build());
        }
        
        categoryBuilder.group(group.build());
    }
    
    private static void addLightSourcesPresetGroup(ConfigCategory.Builder categoryBuilder, LightSourcesPreset preset, CloudsConfiguration config, Screen backScreen) {
        String lastMod = preset.lastModified > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(preset.lastModified)) : "Unknown";
        
        var group = OptionGroup.createBuilder()
            .name(ComponentWrapper.literal(preset.displayName))
            .description(OptionDescription.of(ComponentWrapper.literal(
                "Description: " + preset.description + "\n" +
                "Last modified: " + lastMod + "\n" +
                "ID: " + preset.id
            )));
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("View"))
            .description(OptionDescription.of(ComponentWrapper.literal("View preset details")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createEditorScreen(
                        PresetController.PresetCategory.LIGHT_SOURCES, preset.id, backScreen
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Edit"))
            .description(OptionDescription.of(ComponentWrapper.literal("Edit preset name and description")))
            .action((yacl, btn) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(PresetEditorScreen.createMetadataEditScreen(
                        PresetController.PresetCategory.LIGHT_SOURCES, preset.id, yacl
                    ));
                }
            })
            .build());
        
        group.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.literal("Load"))
            .description(OptionDescription.of(ComponentWrapper.literal("Load this light sources preset")))
            .action((yacl, btn) -> {
                if (PresetController.loadLightSourcesPreset(preset.id, config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal("§aLoaded light sources preset: " + preset.displayName)
                    );
                }
            })
            .build());
        
        if (!preset.id.equals("default")) {
            group.option(ButtonOption.createBuilder()
                .name(ComponentWrapper.literal("Delete"))
                .description(OptionDescription.of(ComponentWrapper.literal("Delete this preset")))
                .action((yacl, btn) -> {
                    if (PresetController.deleteLightSourcesPreset(preset.id)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal("§aDeleted light sources preset: " + preset.displayName)
                            );
                            mc.setScreen(createBrowserScreen(PresetController.PresetCategory.LIGHT_SOURCES, backScreen));
                        }
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
}
