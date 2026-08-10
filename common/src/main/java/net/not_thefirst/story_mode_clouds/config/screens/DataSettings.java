package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerConfiguration.FadeType;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class DataSettings {
    private DataSettings() {}

    public static ConfigBackend.ConfigCategoryBuilder buildSkyColorSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();
        var builder = backend.createCategory("cloudtweaks.option.sky_color", "cloudtweaks.tooltip.sky_color");
        
        List<SkyColorKeypoint> colors = config.CLOUD_COLOR;
        
        builder.action(backend.createAction(
            "cloudtweaks.presets.add_keypoint",
            null,
            () -> {
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
        );
        
        for (int i = 0; i < colors.size(); i++) {
            SkyColorKeypoint kp = colors.get(i);
            
            var group = backend.createGroup("cloudtweaks.raw.keypoint");

            group
                .option(backend.integerOption(
                    "cloudtweaks.raw.time",
                    "cloudtweaks.option.time_of_day",
                    () -> kp.time,
                    v -> kp.time = v
                ).range(0, 24000).field())
                .option(backend.colorOption(
                    "cloudtweaks.raw.color",
                    "cloudtweaks.option.cloud_color",
                    () -> new Color(kp.color),
                    v -> kp.color = v.getRGB()
                ))
                .action(backend.createAction(
                    "cloudtweaks.raw.remove",
                    "cloudtweaks.desc.delete_keypoint",
                    () -> {
                        if (colors.size() > 1 && colors.remove(kp)) {
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.translatable("cloudtweaks.presets.removed_keypoint")
                            );
                            ClientHelper.setScreen(null);
                        }
                    }
                ));

            builder.group(group);
        }

        builder.action(backend.createAction(
            "cloudtweaks.presets.save_color_preset",
            "cloudtweaks.presets.save_color_preset.tooltip",
            () -> {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String presetName = ComponentWrapper.translatable("cloudtweaks.presets.default_color_preset_name_prefix").getString() + formatTimestamp(System.currentTimeMillis());
                if (PresetController.saveColorPreset(timestamp, presetName, ComponentWrapper.translatable("cloudtweaks.presets.default_color_preset_description").getString(), config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.saved_color_preset_prefix")
                    );
                }
            }
        ));

        return builder;
    }
    
    private static String formatTimestamp(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    public static ConfigBackend.ConfigCategoryBuilder buildLightSourcesSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();

        var builder = backend.createCategory("cloudtweaks.category.light_sources", "cloudtweaks.desc.light_sources");

        List<DiffuseLight> lights = config.LIGHTING.lights;
        
        builder.action(backend.createAction(
            "cloudtweaks.presets.add_light",
            "cloudtweaks.presets.add_light.tooltip",
            () -> {
                DiffuseLight newLight = new DiffuseLight();
                lights.add(newLight);
                CloudsConfiguration.save();
                ClientHelper.sendLocalSystemMessage(
                    ComponentWrapper.translatable("cloudtweaks.presets.added_light")
                );
                ClientHelper.setScreen(null);
            })
        );
        
        for (int i = 0; i < lights.size(); i++) {
            DiffuseLight light = lights.get(i);

            var group = backend.createGroup("cloudtweaks.entry.light")
                .option(backend.floatOption(
                    "cloudtweaks.option.direction_x",
                    "cloudtweaks.option.direction_x.tooltip",
                    () -> light.getXDirection(),
                    v -> light.setXDirection(v)
                ).range(-1.0f, 1.0f).step(0.01f).slider())
                .option(backend.floatOption(
                    "cloudtweaks.option.direction_y",
                    "cloudtweaks.option.direction_y.tooltip",
                    () -> light.getYDirection(),
                    v -> light.setYDirection(v)
                ).range(-1.0f, 1.0f).step(0.01f).slider())
                .option(backend.floatOption(
                    "cloudtweaks.option.direction_z",
                    "cloudtweaks.option.direction_z.tooltip",
                    () -> light.getZDirection(),
                    v -> light.setZDirection(v)
                ).range(-1.0f, 1.0f).step(0.01f).slider())
                .option(backend.floatOption(
                    "cloudtweaks.option.intensity",
                    "cloudtweaks.option.intensity.tooltip",
                    () -> light.intensity(),
                    v -> light.setIntensity(v)
                ).range(0.0f, 1.0f).step(0.01f).slider())
                .action(backend.createAction(
                    "cloudtweaks.raw.remove",
                    "cloudtweaks.desc.light.delete",
                    () -> {
                        if (lights.size() > 1 && lights.remove(light)) {
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.translatable("cloudtweaks.presets.removed_light")
                            );
                            ClientHelper.setScreen(null);
                        }
                    }
                ));
            
            builder.group(group);
        }

        builder.action(backend.createAction(
            "cloudtweaks.presets.save_light_sources_preset",
            "cloudtweaks.presets.save_light_sources_preset.tooltip",
            () -> {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String presetName = ComponentWrapper.translatable("cloudtweaks.presets.default_light_sources_preset_name_prefix").getString() + formatTimestamp(System.currentTimeMillis());
                if (PresetController.saveLightSourcesPreset(timestamp, presetName, ComponentWrapper.translatable("cloudtweaks.presets.default_light_sources_preset_description").getString(), config)) {
                    CloudsConfiguration.save();
                    if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.translatable("cloudtweaks.message.saved_light_sources_preset_prefix")
                    );
                }
            }
        ));
        
        return builder;
    }

    public static Screen createLayerEditingScreen(CloudsConfiguration.Dimension dimension, int layerIndex, Screen backScreen) {
        CloudsConfiguration config = CloudsConfiguration.getInstance();
        LayerConfiguration layer = config.getLayerInDimension(dimension, layerIndex);
        
        return BackendHolder.getBackend().createScreen("cloudtweaks.layer.settings_title", CloudsConfiguration::save)
            .category(buildLayerBasicSettings(layer))
            .category(buildLayerAppearanceSettings(layer))
            .category(buildOutlineCategory(layer))
            .category(buildTextureCategory(layer))
            .category(buildLayerBevelSettings(layer))
            .category(buildLayerFogSettings(layer))
            .category(buildLayerFadeSettings(layer))
            .category(buildLayerSaveAsPresetCategory(layer, dimension))
            .build(backScreen);
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerBasicSettings(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.basic", null);

        category
            .option(backend.stringOption(
                "cloudtweaks.option.name",
                null,
                () -> layer.NAME,
                v -> layer.NAME = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.enabled",
                null,
                () -> layer.IS_ENABLED,
                v -> layer.IS_ENABLED = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.rendered",
                null,
                () -> layer.LAYER_RENDERED,
                v -> layer.LAYER_RENDERED = v
            ))
            .option(backend.integerOption(
                "cloudtweaks.option.height",
                null,
                () -> layer.LAYER_HEIGHT,
                v -> layer.LAYER_HEIGHT = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.cloud_mode",
                null,
                () -> layer.MODE,
                v -> layer.MODE = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.fog_enabled",
                null,
                () -> layer.FOG_ENABLED,
                v -> layer.FOG_ENABLED = v
            ));

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildTypeSpecificCategory(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.placeholder", null);

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildOutlineCategory(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.outline", null);

        category
            .option(backend.booleanOption(
                "cloudtweaks.option.outline_enabled",
                null,
                () -> layer.APPEARANCE.OUTLINE_ENABLED,
                v -> layer.APPEARANCE.OUTLINE_ENABLED = v
            ))
            .option(backend.floatOption(
                "cloudtweaks.option.outline_size",
                null,
                () -> layer.APPEARANCE.OUTLINE_SIZE,
                v -> layer.APPEARANCE.OUTLINE_SIZE = v
            ).range(0.0f, ConfigConstants.MAX_CLOUDS_OUTLINE_SIZE)
                .step(0.01f)
                .slider())
            .option(backend.floatOption(
                "cloudtweaks.option.outline_alpha",
                null,
                () -> layer.APPEARANCE.OUTLINE_ALPHA,
                v -> layer.APPEARANCE.OUTLINE_ALPHA = v
            ).range(0.0f, 1.0f)
                .step(0.01f)
                .slider())
            .option(backend.floatOption(
                "cloudtweaks.option.outline_brightness",
                null,
                () -> layer.APPEARANCE.OUTLINE_BRIGHTNESS,
                v -> layer.APPEARANCE.OUTLINE_BRIGHTNESS = v
            ).range(0.0f, 1.0f)
                .step(0.01f)
                .slider())
            .option(backend.colorOption(
                "cloudtweaks.option.outline_color",
                null,
                () -> new Color(layer.APPEARANCE.OUTLINE_COLOR),
                v -> layer.APPEARANCE.OUTLINE_COLOR = v.getRGB()
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.outline_custom_brightness",
                null,
                () -> layer.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS,
                v -> layer.APPEARANCE.OUTLINE_CUSTOM_BRIGHTNESS = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.outline_override_texture_color",
                null,
                () -> layer.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR,
                v -> layer.APPEARANCE.OUTLINE_OVERRIDE_TEXTURE_COLOR = v
            ));

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildTextureCategory(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.texture", null);

        category
            .option(backend.stringOption(
                "cloudtweaks.option.custom_texture_namespace",
                null,
                () -> layer.APPEARANCE.TEXTURE_NAMESPACE,
                v -> layer.APPEARANCE.TEXTURE_NAMESPACE = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.custom_texture_name",
                null,
                () -> layer.APPEARANCE.TEXTURE_NAME,
                v -> layer.APPEARANCE.TEXTURE_NAME = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.custom_texture_flip_x",
                null,
                () -> layer.APPEARANCE.TEXTURE_HORIZONTAL_FLIP,
                v -> layer.APPEARANCE.TEXTURE_HORIZONTAL_FLIP = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.custom_texture_flip_y",
                null,
                () -> layer.APPEARANCE.TEXTURE_VERTICAL_FLIP,
                v -> layer.APPEARANCE.TEXTURE_VERTICAL_FLIP = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.preserve_original_texture_color",
                null,
                () -> layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR,
                v -> layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR = v
            ));

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerAppearanceSettings(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.appearance", null);

        category
            .option(backend.booleanOption(
                "cloudtweaks.option.shading_enabled",
                null,
                () -> layer.APPEARANCE.SHADING_ENABLED,
                v -> layer.APPEARANCE.SHADING_ENABLED = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.use_custom_alpha",
                null,
                () -> layer.APPEARANCE.USES_CUSTOM_ALPHA,
                v -> layer.APPEARANCE.USES_CUSTOM_ALPHA = v
            ))
            .option(backend.integerOption(
                "cloudtweaks.option.base_alpha",
                null,
                () -> layer.APPEARANCE.BASE_ALPHA,
                v -> layer.APPEARANCE.BASE_ALPHA = v
            ).range(0, 255).field())
            .option(backend.booleanOption(
                "cloudtweaks.option.custom_brightness",
                null,
                () -> layer.APPEARANCE.CUSTOM_BRIGHTNESS,
                v -> layer.APPEARANCE.CUSTOM_BRIGHTNESS = v
            ))
            .option(backend.floatOption(
                "cloudtweaks.option.brightness",
                null,
                () -> layer.APPEARANCE.BRIGHTNESS,
                v -> layer.APPEARANCE.BRIGHTNESS = v
            ).range(0.0f, 1.0f)
                .step(0.05f)
                .slider())
            .option(backend.booleanOption(
                "cloudtweaks.option.use_custom_color",
                null,
                () -> layer.APPEARANCE.USES_CUSTOM_COLOR,
                v -> layer.APPEARANCE.USES_CUSTOM_COLOR = v
            ))
            .option(backend.colorOption(
                "cloudtweaks.option.color",
                null,
                () -> new Color(layer.APPEARANCE.LAYER_COLOR),
                v -> layer.APPEARANCE.LAYER_COLOR = v.getRGB()

            ))
            .option(backend.floatOption(
                "cloudtweaks.option.y_scale",
                null,
                () -> layer.APPEARANCE.CLOUD_Y_SCALE,
                v -> layer.APPEARANCE.CLOUD_Y_SCALE = v
            ).range(0.1f, 10.0f)
                .step(0.1f)
                .slider())
            .option(backend.integerOption(
                "cloudtweaks.option.offset_x",
                null,
                () -> layer.APPEARANCE.LAYER_OFFSET_X,
                v -> layer.APPEARANCE.LAYER_OFFSET_X = v
            ).range(-ConfigConstants.MAX_LAYER_OFFSET, ConfigConstants.MAX_LAYER_OFFSET)
                .field())
            .option(backend.integerOption(
                "cloudtweaks.option.offset_z",
                null,
                () -> layer.APPEARANCE.LAYER_OFFSET_Z,
                v -> layer.APPEARANCE.LAYER_OFFSET_Z = v
            ).range(-ConfigConstants.MAX_LAYER_OFFSET, ConfigConstants.MAX_LAYER_OFFSET)
                .field())
            .option(backend.floatOption(
                "cloudtweaks.option.speed_x",
                null,
                () -> layer.APPEARANCE.LAYER_SPEED_X,
                v -> layer.APPEARANCE.LAYER_SPEED_X = v
            ).range(-ConfigConstants.MAX_AXIS_VELOCITY, ConfigConstants.MAX_AXIS_VELOCITY)
                .step(0.01f)
                .slider())
            .option(backend.floatOption(
                "cloudtweaks.option.speed_z",
                null,
                () -> layer.APPEARANCE.LAYER_SPEED_Z,
                v -> layer.APPEARANCE.LAYER_SPEED_Z = v
            ).range(-ConfigConstants.MAX_AXIS_VELOCITY, ConfigConstants.MAX_AXIS_VELOCITY)
                .step(0.01f)
                .slider())
            .option(backend.floatOption(
                "cloudtweaks.option.height_offset",
                null,
                () -> layer.APPEARANCE.LAYER_HEIGHT_OFFSET,
                v -> layer.APPEARANCE.LAYER_HEIGHT_OFFSET = v
            ).range(ConfigConstants.MIN_LAYER_HEIGHT_OFFSET, ConfigConstants.MAX_LAYER_HEIGHT_OFFSET)
                .step(ConfigConstants.LAYER_HEIGHT_OFFSET_STEP)
                .slider());
                
            return category;
        }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerBevelSettings(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();

        var category = backend.createCategory("cloudtweaks.group.bevel", null);

        category
            .option(backend.floatOption(
                "cloudtweaks.option.bevel_size",
                null,
                () -> layer.BEVEL.BEVEL_SIZE,
                v -> layer.BEVEL.BEVEL_SIZE = v
            ).range(ConfigConstants.MIN_BEVEL_SIZE, ConfigConstants.MAX_BEVEL_SIZE)
                .step(0.05f)
                .slider())
            .option(backend.integerOption(
                "cloudtweaks.option.edge_segments",
                null,
                () -> layer.BEVEL.BEVEL_EDGE_SEGMENTS,
                v -> layer.BEVEL.BEVEL_EDGE_SEGMENTS = v
            ).range(ConfigConstants.MIN_EDGE_SEGMENTS, ConfigConstants.MAX_EDGE_SEGMENTS)
                .step(1)
                .field())
            .option(backend.integerOption(
                "cloudtweaks.option.corner_segments",
                null,
                () -> layer.BEVEL.BEVEL_CORNER_SEGMENTS,
                v -> layer.BEVEL.BEVEL_CORNER_SEGMENTS = v
            ).range(ConfigConstants.MIN_CORNER_SEGMENTS, ConfigConstants.MAX_CORNER_SEGMENTS)
                .step(1)
                .field());

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerFogSettings(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.group.fog", null);
    
        category
            .option(backend.integerOption(
                "cloudtweaks.option.fog_start",
                "cloudtweaks.desc.fog_start",
                () -> (int) (layer.FOG.FOG_START_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                v -> layer.FOG.FOG_START_DISTANCE = (float)v * ConfigConstants.FOG_DISTANCE_CHUNK_SIZE
            ).range(ConfigConstants.MIN_FOG_DISTANCE_CHUNKS, ConfigConstants.MAX_FOG_START_DISTANCE_CHUNKS).field())
            .option(backend.integerOption(
                "cloudtweaks.option.fog_end",
                "cloudtweaks.desc.fog_end",
                () -> (int) (layer.FOG.FOG_END_DISTANCE / ConfigConstants.FOG_DISTANCE_CHUNK_SIZE),
                v -> layer.FOG.FOG_END_DISTANCE = (float)v * ConfigConstants.FOG_DISTANCE_CHUNK_SIZE
            ).range(ConfigConstants.MIN_FOG_DISTANCE_CHUNKS, ConfigConstants.MAX_FOG_END_DISTANCE_CHUNKS)
                .field());

        return category;
    }

    private static ConfigBackend.ConfigCategoryBuilder buildLayerFadeSettings(LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.group.fade", null);

        category
            .option(backend.booleanOption(
                "cloudtweaks.option.fade_enabled",
                null,
                () -> layer.FADE.FADE_ENABLED,
                v -> layer.FADE.FADE_ENABLED = v
            ))
            .option(backend.booleanOption(
                "cloudtweaks.option.inverted_fade",
                null,
                () -> layer.FADE.INVERTED_FADE,
                v -> layer.FADE.INVERTED_FADE = v
            ))
            .option(backend.enumOption(
                "cloudtweaks.option.fade_mode",
                null,
                () -> layer.FADE.FADE_TYPE,
                v -> layer.FADE.FADE_TYPE = v
            ).enumClass(FadeType.class))
            .option(backend.booleanOption(
                "cloudtweaks.option.color_fade",
                null,
                () -> layer.FADE.COLOR_FADE,
                v -> layer.FADE.COLOR_FADE = v
            ))
            .option(backend.integerOption(
                "cloudtweaks.option.fade_alpha",
                null,
                () -> layer.FADE.FADE_ALPHA,
                v -> layer.FADE.FADE_ALPHA = v
            ).range(0, 255).field())
            .option(backend.colorOption(
                "cloudtweaks.option.fade_to_color",
                null,
                () -> new Color(layer.FADE.FADE_TO_COLOR),
                v -> layer.FADE.FADE_TO_COLOR = v.getRGB()
            ))
            .option(backend.floatOption(
                "cloudtweaks.option.transition_range",
                null,
                () -> layer.FADE.TRANSITION_RANGE,
                v -> layer.FADE.TRANSITION_RANGE = v
            ).range(0.1f, 100.0f).step(0.5f).slider())
            .option(backend.floatOption(
                "cloudtweaks.option.static_fade_rel_y",
                "cloudtweaks.desc.static_fade_rel_y",
                () -> layer.FADE.STATIC_FADE_REL_Y,
                v -> layer.FADE.STATIC_FADE_REL_Y = v
            ).range(-50.0f, 50.0f).step(0.5f).slider());

        return category;
    }

    static Screen createLayerSettingsScreenForDimension(
        CloudsConfiguration config, 
        CloudsConfiguration.Dimension dimension,
        Screen backScreen) {

        var backend = BackendHolder.getBackend();

        return backend.createScreen("cloudtweaks.title.layer_settings", CloudsConfiguration::save)
            .category(buildLayersSettingsForDimension(
                config, 
                dimension, 
                "cloudtweaks.category.layers", 
                "cloudtweaks.desc.layers"))
            .build(backScreen);
    }

    static ConfigBackend.ConfigCategoryBuilder buildLayersSettingsForDimension(
        CloudsConfiguration config, 
        CloudsConfiguration.Dimension dimension,
        String categoryNameKey,
        String categoryDescKey) {

        var backend = BackendHolder.getBackend();
        var category = backend.createCategory(categoryNameKey, categoryDescKey);
        
        LayerHolder layerHolder = config.getLayerHolder(dimension);

        category
            .option(backend.booleanOption(
                "cloudtweaks.option.dimension_clouds_rendered",
                null,
                () -> config.getCloudRendered(dimension),
                v -> config.setCloudRendered(dimension, v)
            ))

            .action(backend.createAction(
                "cloudtweaks.title.add_layer",
                "cloudtweaks.desc.add_layer",
                () -> {
                    LayerConfiguration newLayer = new LayerConfiguration(layerHolder.layers.size());
                    layerHolder.addLayer(newLayer);
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.added_layer_prefix").getString() + layerHolder.layers.size() + ComponentWrapper.translatable("cloudtweaks.message.added_layer_to").getString() + dimension.getId())
                    );

                    Screen refreshedScreen = createLayerSettingsScreenForDimension(config, dimension, ClientHelper.getCurrentScreen());
                    if (refreshedScreen != null) {
                        ClientHelper.setScreen(refreshedScreen);
                    } else {
                        ClientHelper.setScreen(null);
                    }
                })
            );

        for (int i = 0; i < layerHolder.layers.size(); i++) {
            LayerConfiguration layer = layerHolder.layers.get(i);
            final int layerIndex = i;

            var group = backend.createGroup(ComponentWrapper.translatable("cloudtweaks.layer.label_prefix").getString() + (i + 1) + ": " + layer.NAME);

            group
                .option(backend.stringOption(
                    "cloudtweaks.option.name",
                    null,
                    () -> layer.NAME,
                    v -> layer.NAME = v
                ))
                .action(backend.createAction(
                    "cloudtweaks.option.edit",
                    "cloudtweaks.option.edit_layer_settings",
                    () -> {
                        Screen editScreen = createLayerEditingScreen(dimension, layerIndex, ClientHelper.getCurrentScreen());
                        if (editScreen != null) {
                            ClientHelper.setScreen(editScreen);
                        }
                    })
                )
                .action(backend.createAction(
                    "cloudtweaks.option.remove_layer",
                    "cloudtweaks.desc.remove_layer",
                    () -> {
                        if (layerHolder.layers.size() > 1) {
                            layerHolder.removeLayer(layerIndex);
                            CloudsConfiguration.save();
                            ClientHelper.sendLocalSystemMessage(
                                ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.removed_layer_prefix").getString() + dimension.getId())
                            );
                            Screen refreshedScreen = createLayerSettingsScreenForDimension(config, dimension, ClientHelper.getCurrentScreen());
                            if (refreshedScreen != null) {
                                ClientHelper.setScreen(refreshedScreen);
                            } else {
                                ClientHelper.setScreen(null);
                            }
                        }
                    })
                );
            
            category.group(group);
        }

        category.action(backend.createAction(
            "cloudtweaks.presets.layer_presets_button",
            "cloudtweaks.presets.layer_presets_button.tooltip",
            () -> {
                Screen presetsScreen = LayerPresetsScreen.createLayerPresetsScreen(dimension, ClientHelper.getCurrentScreen());
                if (presetsScreen != null) {
                    ClientHelper.setScreen(presetsScreen);
                }
            })
        );
        
        return category;
    }

    public static Screen createLayerPresetEditingScreen(LayerConfiguration layer, String presetId, 
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
            .category(buildLayerBasicSettings(layer))
            .category(buildLayerAppearanceSettings(layer))
            .category(buildOutlineCategory(layer))
            .category(buildTextureCategory(layer))
            .category(buildLayerBevelSettings(layer))
            .category(buildLayerFogSettings(layer))
            .category(buildLayerFadeSettings(layer))
            .category(buildLayerPresetSaveCategory(layer, presetId, dimension, backScreen))
            .build(backScreen);
    }

    /**
     * Build a category with options to save/update the layer preset.
     */
    private static ConfigBackend.ConfigCategoryBuilder buildLayerPresetSaveCategory(LayerConfiguration layer, String presetId, 
                                                               CloudsConfiguration.Dimension dimension, Screen backScreen) {


        // what should i do with this
        var saveData = new Object() {
            String displayName = layer.NAME;
            String description = ComponentWrapper.translatable("cloudtweaks.presets.layer_preset_description").getString();
        };

        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.presets.save_as_preset", null);

        category
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc",
                null,
                () -> saveData.description, 
                v -> saveData.description = v
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.update_preset",
                "cloudtweaks.presets.save_changes_description",
                () -> {
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
            );

        return category;
    }

    /**
     * Build a category with option to save the current layer as a preset.
     */
    private static ConfigBackend.ConfigCategoryBuilder buildLayerSaveAsPresetCategory(LayerConfiguration layer, CloudsConfiguration.Dimension dimension) {
        var saveData = new Object() {
            String presetId = "layer_" + System.currentTimeMillis();
            String displayName = layer.NAME;
            String description = ComponentWrapper.translatable("cloudtweaks.presets.custom_layer_preset_description").getString();
        };

        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.presets.save_as_preset", null);

        category
            .option(backend.stringOption(
                "cloudtweaks.option.preset_id", 
                null, 
                () -> saveData.presetId, 
                v -> saveData.presetId = v
            ))
            .option(backend.stringOption(
                "cloudtweaks.option.preset_desc", 
                null, 
                () -> saveData.description, 
                v -> saveData.description = v
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.save_layer_as_preset",
                "cloudtweaks.presets.save_layer_as_preset_description",
                () -> {
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
            );

        return category;
    }
}