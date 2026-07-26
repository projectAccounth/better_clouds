package net.not_thefirst.story_mode_clouds.config.screens;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * sdjsdjsjdsjd
 */
public class YACLDataSettings {
    private YACLDataSettings() {}

    public static ConfigCategory buildSkyColorSettings(CloudsConfiguration config) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.sky_color"));
        
        List<SkyColorKeypoint> colors = config.CLOUD_COLOR;
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.add_keypoint"))
            .action((yacl, btn) -> {
                if (colors.size() < 32) {
                    SkyColorKeypoint newKeypoint = new SkyColorKeypoint();
                    newKeypoint.time = colors.isEmpty() ? 0 : colors.get(colors.size() - 1).time + 1000;
                    newKeypoint.color = 0xFFFFFF;
                    colors.add(newKeypoint);
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.keypoint_added")
                    );
                    ClientHelper.setScreen(null);
                }
            })
            .build());
        
        for (int i = 0; i < colors.size(); i++) {
            SkyColorKeypoint kp = colors.get(i);
            
            var groupBuilder = OptionGroup.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.raw.keypoint"))
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.time"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.time_of_day")))
                    .binding(kp.time, () -> kp.time, v -> kp.time = v)
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 24000))
                    .build())
                
                .option(Option.<Color>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.color"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.cloud_color")))
                    .binding(
                        new Color(kp.color),
                        () -> new Color(kp.color),
                        v -> kp.color = v.getRGB()
                    )
                    .controller(ColorControllerBuilder::create)
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.remove"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.delete_keypoint")))
                    .action((yacl, btn) -> {
                        if (colors.size() > 1 && colors.remove(kp)) {
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.translatable("cloudtweaks.presets.removed_keypoint")
                            );
                            ClientHelper.setScreen(null);
                        }
                    })
                    .build());
            
            builder.group(groupBuilder.build());
        }
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.save_color_preset"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_color_preset.tooltip")))
            .action((yacl, btn) -> {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String presetName = ComponentWrapper.translatable("cloudtweaks.presets.default_color_preset_name_prefix").getString() + formatTimestamp(System.currentTimeMillis());
                if (PresetController.saveColorPreset(timestamp, presetName, ComponentWrapper.translatable("cloudtweaks.presets.default_color_preset_description").getString(), config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.saved_color_preset_prefix")
                    );
                }
            })
            .build());
        
        return builder.build();
    }
    
    private static String formatTimestamp(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    public static ConfigCategory buildLightSourcesSettings(CloudsConfiguration config) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.light_sources"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.light_sources"));
        
        List<DiffuseLight> lights = config.LIGHTING.lights;
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.add_light"))
            .action((yacl, btn) -> {
                DiffuseLight newLight = new DiffuseLight();
                lights.add(newLight);
                CloudsConfiguration.save();
                ClientHelper.sendLocalSystemMessage(
                    ComponentWrapper.translatable("cloudtweaks.presets.added_light")
                );
                ClientHelper.setScreen(null);
            })
            .build());
        
        for (int i = 0; i < lights.size(); i++) {
            DiffuseLight light = lights.get(i);
            
            var groupBuilder = OptionGroup.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.entry.light"))
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.direction_x"))
                    .binding(light.getXDirection(), light::getXDirection, light::setXDirection)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.direction_y"))
                    .binding(light.getYDirection(), light::getYDirection, light::setYDirection)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.direction_z"))
                    .binding(light.getZDirection(), light::getZDirection, light::setZDirection)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 1.0f).step(0.01f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.intensity"))
                    .binding(light.intensity(), light::intensity, light::setIntensity)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.remove"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.light.delete")))
                    .action((yacl, btn) -> {
                        if (lights.size() > 1 && lights.remove(light)) {
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.translatable("cloudtweaks.presets.removed_light")
                            );
                            ClientHelper.setScreen(null);
                        }
                    })
                    .build());
            
            builder.group(groupBuilder.build());
        }
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.save_light_sources_preset"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_light_sources_preset.tooltip")))
            .action((yacl, btn) -> {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String presetName = ComponentWrapper.translatable("cloudtweaks.presets.default_light_sources_preset_name_prefix").getString() + formatTimestamp(System.currentTimeMillis());
                if (PresetController.saveLightSourcesPreset(timestamp, presetName, ComponentWrapper.translatable("cloudtweaks.presets.default_light_sources_preset_description").getString(), config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.saved_light_sources_preset_prefix")
                    );
                }
            })
            .build());
        
        return builder.build();
    }

    public static Screen createLayerEditingScreen(int layerIndex, Screen backScreen) {
        return createLayerEditingScreen(CloudsConfiguration.Dimension.OVERWORLD, layerIndex, backScreen);
    }

    public static Screen createLayerEditingScreen(CloudsConfiguration.Dimension dimension, int layerIndex, Screen backScreen) {
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        LayerConfiguration layer = config.getLayerInDimension(dimension, layerIndex);
        
        return YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.layer.settings_title"))
            .save(CloudsConfiguration::save)
            
            .category(buildLayerBasicSettings(layer))
            .category(buildLayerAppearanceSettings(layer))
            .category(buildOutlineCategory(layer))
            .category(buildTextureCategory(layer))
            .category(buildLayerBevelSettings(layer))
            .category(buildLayerFogSettings(layer))
            .category(buildLayerFadeSettings(layer))
            .category(buildLayerSaveAsPresetCategory(layer, dimension))
            
            .build()
            .generateScreen(backScreen);
    }

    private static ConfigCategory buildLayerBasicSettings(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.basic"))
            
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.name"))
                .binding(layer.NAME, () -> layer.NAME, v -> layer.NAME = v)
                .controller(StringControllerBuilder::create)
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.enabled"))
                .binding(layer.IS_ENABLED, () -> layer.IS_ENABLED, v -> layer.IS_ENABLED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.rendered"))
                .binding(layer.LAYER_RENDERED, () -> layer.LAYER_RENDERED, v -> layer.LAYER_RENDERED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.height"))
                .binding(layer.LAYER_HEIGHT, () -> layer.LAYER_HEIGHT, v -> layer.LAYER_HEIGHT = v)
                .controller(IntegerFieldControllerBuilder::create)
                .build())
            
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.cloud_mode"))
                .binding(layer.MODE, () -> layer.MODE, v -> layer.MODE = v)
                .controller(opt -> DropdownStringControllerBuilder.create(opt)
                    .values(MeshTypeRegistry.getInstance().keys()
                        .stream()
                        .filter(s -> s != null)
                        .toArray(String[]::new)))
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fog_enabled"))
                .binding(layer.FOG_ENABLED, () -> layer.FOG_ENABLED, v -> layer.FOG_ENABLED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .build();
    }

    private static ConfigCategory buildOutlineCategory(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.outline"))
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_size"))
                .binding(layer.APPEARANCE.OUTLINE_SIZE, () -> layer.APPEARANCE.OUTLINE_SIZE, v -> layer.APPEARANCE.OUTLINE_SIZE = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, ConfigConstants.MAX_CLOUDS_OUTLINE_SIZE).step(0.01f))
                .build())
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_alpha"))
                .binding(layer.APPEARANCE.OUTLINE_ALPHA, () -> layer.APPEARANCE.OUTLINE_ALPHA, v -> layer.APPEARANCE.OUTLINE_ALPHA = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                .build())
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_brightness"))
                .binding(layer.APPEARANCE.OUTLINE_BRIGHTNESS, () -> layer.APPEARANCE.OUTLINE_BRIGHTNESS, v -> layer.APPEARANCE.OUTLINE_BRIGHTNESS = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.01f))
                .build())
            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_color"))
                .binding(
                    new Color(layer.APPEARANCE.OUTLINE_COLOR),
                    () -> new Color(layer.APPEARANCE.OUTLINE_COLOR),
                    v -> layer.APPEARANCE.OUTLINE_COLOR = v.getRGB()
                )
                .controller(opt -> ColorControllerBuilder.create(opt))
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_custom_brightness"))
                .binding(layer.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS, () -> layer.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS, v -> layer.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.outline_override_texture_color"))
                .binding(layer.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR, () -> layer.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR, v -> layer.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            .build();

    }

    private static ConfigCategory buildTextureCategory(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.texture"))
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.custom_texture_namespace"))
                .binding(layer.APPEARANCE.TEXTURE_NAMESPACE, () -> layer.APPEARANCE.TEXTURE_NAMESPACE, v -> layer.APPEARANCE.TEXTURE_NAMESPACE = v)
                .controller(StringControllerBuilder::create)
                .build())
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.custom_texture_name"))
                .binding(layer.APPEARANCE.TEXTURE_NAME, () -> layer.APPEARANCE.TEXTURE_NAME, v -> layer.APPEARANCE.TEXTURE_NAME = v)
                .controller(StringControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.custom_texture_flip_x"))
                .binding(layer.APPEARANCE.TEXTURE_HORIZONTAL_FLIP, () -> layer.APPEARANCE.TEXTURE_HORIZONTAL_FLIP, v -> layer.APPEARANCE.TEXTURE_HORIZONTAL_FLIP = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.custom_texture_flip_y"))
                .binding(layer.APPEARANCE.TEXTURE_VERTICAL_FLIP, () -> layer.APPEARANCE.TEXTURE_VERTICAL_FLIP, v -> layer.APPEARANCE.TEXTURE_VERTICAL_FLIP = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.preserve_original_texture_color"))
                .binding(layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR, () -> layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR, v -> layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            .build();
    }

    private static ConfigCategory buildLayerAppearanceSettings(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.appearance"))
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.shading_enabled"))
                .binding(layer.APPEARANCE.SHADING_ENABLED, () -> layer.APPEARANCE.SHADING_ENABLED, v -> layer.APPEARANCE.SHADING_ENABLED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.use_custom_alpha"))
                .binding(layer.APPEARANCE.USES_CUSTOM_ALPHA, () -> layer.APPEARANCE.USES_CUSTOM_ALPHA, v -> layer.APPEARANCE.USES_CUSTOM_ALPHA = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.base_alpha"))
                .binding(layer.APPEARANCE.BASE_ALPHA, () -> layer.APPEARANCE.BASE_ALPHA, v -> layer.APPEARANCE.BASE_ALPHA = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 255).step(1))
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.custom_brightness"))
                .binding(layer.APPEARANCE.CUSTOM_BRIGHTNESS, () -> layer.APPEARANCE.CUSTOM_BRIGHTNESS, v -> layer.APPEARANCE.CUSTOM_BRIGHTNESS = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.brightness"))
                .binding(layer.APPEARANCE.BRIGHTNESS, () -> layer.APPEARANCE.BRIGHTNESS, v -> layer.APPEARANCE.BRIGHTNESS = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.use_custom_color"))
                .binding(layer.APPEARANCE.USES_CUSTOM_COLOR, () -> layer.APPEARANCE.USES_CUSTOM_COLOR, v -> layer.APPEARANCE.USES_CUSTOM_COLOR = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.color"))
                .binding(
                    new Color(layer.APPEARANCE.LAYER_COLOR),
                    () -> new Color(layer.APPEARANCE.LAYER_COLOR),
                    v -> layer.APPEARANCE.LAYER_COLOR = v.getRGB()
                )
                .controller(ColorControllerBuilder::create)
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.y_scale"))
                .binding(layer.APPEARANCE.CLOUD_Y_SCALE, () -> layer.APPEARANCE.CLOUD_Y_SCALE, v -> layer.APPEARANCE.CLOUD_Y_SCALE = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 10.0f).step(0.1f))
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.offset_x"))
                .binding(layer.APPEARANCE.LAYER_OFFSET_X, () -> layer.APPEARANCE.LAYER_OFFSET_X, v -> layer.APPEARANCE.LAYER_OFFSET_X = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(-ConfigConstants.MAX_LAYER_OFFSET, ConfigConstants.MAX_LAYER_OFFSET))
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.offset_z"))
                .binding(layer.APPEARANCE.LAYER_OFFSET_Z, () -> layer.APPEARANCE.LAYER_OFFSET_Z, v -> layer.APPEARANCE.LAYER_OFFSET_Z = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(-ConfigConstants.MAX_LAYER_OFFSET, ConfigConstants.MAX_LAYER_OFFSET))
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.speed_x"))
                .binding(layer.APPEARANCE.LAYER_SPEED_X, () -> layer.APPEARANCE.LAYER_SPEED_X, v -> layer.APPEARANCE.LAYER_SPEED_X = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-ConfigConstants.MAX_AXIS_VELOCITY, ConfigConstants.MAX_AXIS_VELOCITY).step(0.01f))
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.speed_z"))
                .binding(layer.APPEARANCE.LAYER_SPEED_Z, () -> layer.APPEARANCE.LAYER_SPEED_Z, v -> layer.APPEARANCE.LAYER_SPEED_Z = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-ConfigConstants.MAX_AXIS_VELOCITY, ConfigConstants.MAX_AXIS_VELOCITY).step(0.01f))
                .build())
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.height_offset"))
                .binding(layer.APPEARANCE.LAYER_HEIGHT_OFFSET, () -> layer.APPEARANCE.LAYER_HEIGHT_OFFSET, v -> layer.APPEARANCE.LAYER_HEIGHT_OFFSET = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_LAYER_HEIGHT_OFFSET, ConfigConstants.MAX_LAYER_HEIGHT_OFFSET).step(ConfigConstants.LAYER_HEIGHT_OFFSET_STEP))
                .build())
            
            .build();
        }

    private static ConfigCategory buildLayerBevelSettings(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.bevel"))
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.bevel_size"))
                .binding(layer.BEVEL.BEVEL_SIZE, () -> layer.BEVEL.BEVEL_SIZE, v -> layer.BEVEL.BEVEL_SIZE = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_BEVEL_SIZE, ConfigConstants.MAX_BEVEL_SIZE).step(0.05f))
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.edge_segments"))
                .binding(layer.BEVEL.BEVEL_EDGE_SEGMENTS, () -> layer.BEVEL.BEVEL_EDGE_SEGMENTS, v -> layer.BEVEL.BEVEL_EDGE_SEGMENTS = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_EDGE_SEGMENTS, ConfigConstants.MAX_EDGE_SEGMENTS).step(1))
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.corner_segments"))
                .binding(layer.BEVEL.BEVEL_CORNER_SEGMENTS, () -> layer.BEVEL.BEVEL_CORNER_SEGMENTS, v -> layer.BEVEL.BEVEL_CORNER_SEGMENTS = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(ConfigConstants.MIN_CORNER_SEGMENTS, ConfigConstants.MAX_CORNER_SEGMENTS).step(1))
                .build())
            
            .build();
    }

    private static ConfigCategory buildLayerFogSettings(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.fog"))
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fog_start"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.fog_start")))
                .binding(
                    (int) (layer.FOG.FOG_START_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                    () -> (int) (layer.FOG.FOG_START_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                    v -> layer.FOG.FOG_START_DISTANCE = (float)v * ConfigConstants.FOG_DISTANCE_CHUNK_SIZE)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(ConfigConstants.MIN_FOG_DISTANCE_CHUNKS, ConfigConstants.MAX_FOG_START_DISTANCE_CHUNKS))
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fog_end"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.fog_end")))
                .binding(
                    (int) (layer.FOG.FOG_END_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                    () -> (int) (layer.FOG.FOG_END_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                    v -> layer.FOG.FOG_END_DISTANCE = (float)v * ConfigConstants.FOG_DISTANCE_CHUNK_SIZE)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(ConfigConstants.MIN_FOG_DISTANCE_CHUNKS, ConfigConstants.MAX_FOG_END_DISTANCE_CHUNKS))
                .build())
            
            .build();
    }

    private static ConfigCategory buildLayerFadeSettings(LayerConfiguration layer) {
        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.group.fade"))
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fade_enabled"))
                .binding(layer.FADE.FADE_ENABLED, () -> layer.FADE.FADE_ENABLED, v -> layer.FADE.FADE_ENABLED = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.inverted_fade"))
                .binding(layer.FADE.INVERTED_FADE, () -> layer.FADE.INVERTED_FADE, v -> layer.FADE.INVERTED_FADE = v)
                .controller(BooleanControllerBuilder::create)
                .build())

            .option(Option.<FadeType>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fade_mode"))
                .binding(layer.FADE.FADE_TYPE, () -> layer.FADE.FADE_TYPE, v -> layer.FADE.FADE_TYPE = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(FadeType.class))
                .build())

            .option(Option.<Boolean>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.color_fade"))
                .binding(layer.FADE.COLOR_FADE, () -> layer.FADE.COLOR_FADE, v -> layer.FADE.COLOR_FADE = v)
                .controller(BooleanControllerBuilder::create)
                .build())
            
            .option(Option.<Integer>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fade_alpha"))
                .binding(layer.FADE.FADE_ALPHA, () -> layer.FADE.FADE_ALPHA, v -> layer.FADE.FADE_ALPHA = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 255).step(1))
                .build())

            .option(Option.<Color>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.fade_to_color"))
                .binding(new Color(layer.FADE.FADE_TO_COLOR), () -> new Color(layer.FADE.FADE_TO_COLOR), v -> layer.FADE.FADE_TO_COLOR = v.getRGB())
                .controller(ColorControllerBuilder::create)
                .build())
            
            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.transition_range"))
                .binding(layer.FADE.TRANSITION_RANGE, () -> layer.FADE.TRANSITION_RANGE, v -> layer.FADE.TRANSITION_RANGE = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 100.0f).step(0.5f))
                .build())

            .option(Option.<Float>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.static_fade_rel_y"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.static_fade_rel_y")))
                .binding(layer.FADE.STATIC_FADE_REL_Y, () -> layer.FADE.STATIC_FADE_REL_Y, v -> layer.FADE.STATIC_FADE_REL_Y = v)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-50.0f, 50.0f).step(0.5f))
                .build())
            
            .build();
    }

    public static ConfigCategory buildLayersSettings(CloudsConfiguration config) {
        return buildLayersSettingsForDimension(config, CloudsConfiguration.Dimension.OVERWORLD, 
            "cloudtweaks.category.overworld_layers", "cloudtweaks.desc.overworld_layers");
    }

    public static ConfigCategory buildNetherLayersSettings(CloudsConfiguration config) {
        return buildLayersSettingsForDimension(config, CloudsConfiguration.Dimension.NETHER,
            "cloudtweaks.category.nether_layers", "cloudtweaks.desc.nether_layers");
    }

    public static ConfigCategory buildEndLayersSettings(CloudsConfiguration config) {
        return buildLayersSettingsForDimension(config, CloudsConfiguration.Dimension.END,
            "cloudtweaks.category.end_layers", "cloudtweaks.desc.end_layers");
    }

    private static ConfigCategory buildLayersSettingsForDimension(
        CloudsConfiguration config, 
        CloudsConfiguration.Dimension dimension,
        String categoryNameKey,
        String categoryDescKey) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable(categoryNameKey))
            .tooltip(ComponentWrapper.translatable(categoryDescKey));
        
        LayerHolder layerHolder = config.getLayerHolder(dimension);
        
        // Add per-dimension render toggle
        builder.option(Option.<Boolean>createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.dimension_clouds_rendered"))
            .binding(
                config.getCloudRendered(dimension),
                () -> config.getCloudRendered(dimension),
                v -> config.setCloudRendered(dimension, v)
            )
            .controller(BooleanControllerBuilder::create)
            .build());
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.title.add_layer"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.add_layer")))
            .action((yacl, btn) -> {
                LayerConfiguration newLayer = new LayerConfiguration(layerHolder.layers.size());
                layerHolder.addLayer(newLayer);
                CloudsConfiguration.save();
                ClientHelper.sendLocalSystemMessage(
                    ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.added_layer_prefix").getString() + layerHolder.layers.size() + ComponentWrapper.translatable("cloudtweaks.message.added_layer_to").getString() + dimension.getId())
                );
                ClientHelper.setScreen(null);
            })
            .build());
        
        for (int i = 0; i < layerHolder.layers.size(); i++) {
            LayerConfiguration layer = layerHolder.layers.get(i);
            final int layerIndex = i;
            
            var layerGroup = OptionGroup.createBuilder()
                .name(ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.layer.label_prefix").getString() + (i + 1) + ": " + layer.NAME))
                
                .option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.name"))
                    .binding(layer.NAME, () -> layer.NAME, v -> layer.NAME = v)
                    .controller(StringControllerBuilder::create)
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.edit"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.edit_layer_settings")))
                    .action((yacl, btn) -> {
                        Screen editScreen = createLayerEditingScreen(dimension, layerIndex, ClientHelper.getCurrentScreen());
                        if (editScreen != null) {
                            ClientHelper.setScreen(editScreen);
                        }
                    })
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.remove_layer"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.remove_layer")))
                    .action((yacl, btn) -> {
                        if (layerHolder.layers.size() > 1) {
                            layerHolder.removeLayer(layerIndex);
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.removed_layer_prefix").getString() + dimension.getId())
                            );
                            ClientHelper.setScreen(null);
                        }
                    })
                    .build());
            
            builder.group(layerGroup.build());
        }
        
        // Add Layer Presets button
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.layer_presets_button"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.layer_presets_button.tooltip")))
            .action((yacl, btn) -> {
                Screen presetsScreen = YACLLayerPresetsScreen.createLayerPresetsScreen(dimension, ClientHelper.getCurrentScreen());
                if (presetsScreen != null) {
                    ClientHelper.setScreen(presetsScreen);
                }
            })
            .build());
        
        return builder.build();
    }

    public static Screen createLayerPresetEditingScreen(LayerConfiguration layer, String presetId, 
                                                         CloudsConfiguration.Dimension dimension, Screen backScreen) {
        return YetAnotherConfigLib.createBuilder()
            .title(ComponentWrapper.translatable("cloudtweaks.presets.edit_layer_prefix"))
            .save(() -> LayerPresets.saveLayerPreset(
                presetId,
                layer.NAME,
                ComponentWrapper.translatable("cloudtweaks.presets.modified_layer_preset").getString(),
                layer,
                    dimension.getId()
                )
            )
            
            .category(buildLayerBasicSettings(layer))
            .category(buildLayerAppearanceSettings(layer))
            .category(buildOutlineCategory(layer))
            .category(buildTextureCategory(layer))
            .category(buildLayerBevelSettings(layer))
            .category(buildLayerFogSettings(layer))
            .category(buildLayerFadeSettings(layer))
            .category(buildLayerPresetSaveCategory(layer, presetId, dimension, backScreen))
            
            .build()
            .generateScreen(backScreen);
    }

    /**
     * Build a category with options to save/update the layer preset.
     */
    private static ConfigCategory buildLayerPresetSaveCategory(LayerConfiguration layer, String presetId, 
                                                               CloudsConfiguration.Dimension dimension, Screen backScreen) {
        var saveData = new Object() {
            String displayName = layer.NAME;
            String description = ComponentWrapper.translatable("cloudtweaks.presets.layer_preset_description").getString();
        };

        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.save_as_preset"))

            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
                .binding(saveData.description, () -> saveData.description, v -> saveData.description = v)
                .controller(StringControllerBuilder::create)
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.update_preset"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_changes_description")))
                .action((yacl, btn) -> {
                    boolean success = LayerPresets.saveLayerPreset(
                        presetId,
                        saveData.displayName,
                        saveData.description,
                        layer,
                        dimension.getId()
                    );

                    if (success) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.updated_preset_prefix").getString() + saveData.displayName)
                        );
                        ClientHelper.setScreen(backScreen);
                    }
                })
                .build())
            
            .build();
    }

    /**
     * Build a category with option to save the current layer as a preset.
     */
    private static ConfigCategory buildLayerSaveAsPresetCategory(LayerConfiguration layer, CloudsConfiguration.Dimension dimension) {
        var saveData = new Object() {
            String presetId = "layer_" + System.currentTimeMillis();
            String displayName = layer.NAME;
            String description = ComponentWrapper.translatable("cloudtweaks.presets.custom_layer_preset_description").getString();
        };

        return ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.presets.save_as_preset"))
            
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.preset_id"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.tooltip.preset_id")))
                .binding(saveData.presetId, () -> saveData.presetId, v -> saveData.presetId = v)
                .controller(StringControllerBuilder::create)
                .build())
            
            .option(Option.<String>createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.option.preset_desc"))
                .binding(saveData.description, () -> saveData.description, v -> saveData.description = v)
                .controller(StringControllerBuilder::create)
                .build())
            
            .option(ButtonOption.createBuilder()
                .name(ComponentWrapper.translatable("cloudtweaks.presets.save_as_preset"))
                .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.presets.save_layer_as_preset_description")))
                .action((yacl, btn) -> {
                    boolean success = LayerPresets.saveLayerPreset(
                        saveData.presetId,
                        saveData.displayName,
                        saveData.description,
                        layer,
                        dimension.getId()
                    );

                    if (success) {
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.saved_layer_preset_prefix").getString() + saveData.displayName)
                        );
                    }
                })
                .build())
            
            .build();
    }
}
