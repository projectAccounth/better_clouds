package net.not_thefirst.story_mode_clouds.config.screens;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.config.ConfigConstants;
import net.not_thefirst.story_mode_clouds.config.presets.LayerPresets;
import net.not_thefirst.story_mode_clouds.config.presets.PresetController;
import net.not_thefirst.story_mode_clouds.config.screens.managers.*;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.Starter;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeRegistry;
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

    private static String formatTimestamp(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    /**
     * ==========================================================================
     * Other categories' section
     *
     */

    static ConfigBackend.ConfigCategoryBuilder buildGlobalSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();
        return backend.createCategory("cloudtweaks.category.global", "cloudtweaks.desc.global")
            .option(backend.booleanOption(
                "cloudtweaks.option.clouds_mod_enabled",
                null,
                () -> config.CLOUDS_RENDERED,
                v -> config.CLOUDS_RENDERED = v
            ))
            .option(backend.integerOption(
                "cloudtweaks.option.cloud_distance",
                "cloudtweaks.option.cloud_distance.tooltip",
                config::getCloudDistanceChunks,
                v -> config.CLOUD_DISTANCE_CHUNKS = v
            ).range(ConfigConstants.MIN_CLOUD_DISTANCE_CHUNKS, ConfigConstants.MAX_CLOUD_DISTANCE_CHUNKS)
                .step(ConfigConstants.CLOUD_DISTANCE_STEP)
                .slider())
            .action(backend.createAction(
                "cloudtweaks.option.rebuild_clouds",
                "cloudtweaks.desc.rebuild_clouds",
                () -> {
                    CloudsConfiguration.save();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.clouds_rebuilt"));
                    ClientHelper.setScreen(null);
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.option.recompile_scripts",
                "cloudtweaks.desc.recompile_scripts",
                () -> {
                    CloudsConfiguration.save();
                    Starter.reloadScript();
                    RendererHolder.get().markForRebuild();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.reloaded_scripts"));
                    ClientHelper.setScreen(null);
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.option.refresh_config", 
                "cloudtweaks.desc.refresh_config", 
                () -> {
                    CloudsConfiguration.save();
                    CloudsConfiguration.refreshConfig();
                    ClientHelper.sendLocalSystemMessage(ComponentWrapper.translatable("cloudtweaks.raw.refreshed_config"));
                    ClientHelper.setScreen(null);
                }));
    }

    static ConfigBackend.ConfigCategoryBuilder buildSkyColorSettings(CloudsConfiguration config) {
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
                String presetName = "ColorPreset_" + formatTimestamp(System.currentTimeMillis());
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
    
    static ConfigBackend.ConfigCategoryBuilder buildLightingSettings(CloudsConfiguration config) {
        var backend = BackendHolder.getBackend();
        LightingParameters lighting = config.LIGHTING;

        return backend.createCategory("cloudtweaks.category.lighting", "cloudtweaks.desc.lighting")
            .option(backend.floatOption(
                "cloudtweaks.option.ambient_strength",
                null,
                () -> lighting.AMBIENT_LIGHTING_STRENGTH,
                v -> lighting.AMBIENT_LIGHTING_STRENGTH = v
            ).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS)
                .step(ConfigConstants.BRIGHTNESS_STEP)
                .slider())
            // .option(backend.floatOption(
            //     "cloudtweaks.option.max_shading",
            //     null,
            //     () -> lighting.MAX_LIGHTING_SHADING,
            //     v -> lighting.MAX_LIGHTING_SHADING = v
            // ).range(ConfigConstants.MIN_BRIGHTNESS, ConfigConstants.MAX_BRIGHTNESS)
            //     .step(ConfigConstants.BRIGHTNESS_STEP)
            //     .slider())
            .option(backend.enumOption(
                "cloudtweaks.option.cloud_provider",
                null,
                () -> config.COLOR_MODE,
                v -> config.COLOR_MODE = v
            ).enumClass(CloudColorProviderMode.class))
            .option(backend.enumOption(
                "cloudtweaks.option.lighting_type",
                null,
                () -> lighting.LIGHTING_TYPE,
                v -> lighting.LIGHTING_TYPE = v
            ).enumClass(LightingType.class))
            .option(backend.enumOption(
                "cloudtweaks.option.shading_mode",
                null,
                () -> lighting.SHADING_MODE,
                v -> lighting.SHADING_MODE = v
            ).enumClass(ShadingMode.class))
            
            .option(backend.colorOption(
                "cloudtweaks.option.rain_color",
                null,
                () -> new java.awt.Color(config.WEATHER_COLOR.rainColor | 0xFF000000),
                v -> config.WEATHER_COLOR.rainColor = v.getRGB() & 0xFFFFFF
            ))
            .option(backend.colorOption(
                "cloudtweaks.option.thunder_color",
                null,
                () -> new java.awt.Color(config.WEATHER_COLOR.thunderColor | 0xFF000000),
                v -> config.WEATHER_COLOR.thunderColor = v.getRGB() & 0xFFFFFF
            ))
            .option(backend.floatOption(
                "cloudtweaks.option.rain_strength",
                null,
                () -> config.WEATHER_COLOR.rainStrength,
                v -> config.WEATHER_COLOR.rainStrength = v
            ).range(0.0f, 1.0f)
                .step(0.01f))
            .option(backend.floatOption(
                "cloudtweaks.option.thunder_strength",
                null,
                () -> config.WEATHER_COLOR.thunderStrength,
                v -> config.WEATHER_COLOR.thunderStrength = v
            ).range(0.0f, 1.0f)
                .step(0.01f))
            .action(backend.createAction(
                "cloudtweaks.option.save_lighting_preset",
                "cloudtweaks.desc.save_lighting_preset",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved from Lighting settings", config)) {
                        CloudsConfiguration.save();
                        RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.literal("Saved lighting preset: " + presetName)
                        );
                    }
                }
            ));
    }

    static ConfigBackend.ConfigCategoryBuilder buildLightSourcesSettings(CloudsConfiguration config) {
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
                String presetName = "LightSourcesPreset_" + formatTimestamp(System.currentTimeMillis());
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

    /**
     * ==========================================================================
     * Layer category
     *
     */


    public static Screen createLayerEditingScreen(CloudsConfiguration.Dimension dimension, int layerIndex, Screen backScreen) {
        var manager = LayerEditingScreenManager.getInstance(dimension, layerIndex);
        manager.setParentScreen(backScreen);
        return manager.buildScreen();
    }

    static Screen createLayerSettingsScreenForDimension(
        CloudsConfiguration config, 
        CloudsConfiguration.Dimension dimension,
        Screen backScreen) {
        var manager = LayerSettingsScreenManager.getInstance(dimension);
        manager.setParentScreen(backScreen);
        return manager.buildScreen();
    }

    static ConfigBackend.ConfigCategoryBuilder buildLayersSettingsForDimension(
        CloudsConfiguration config, 
        CloudsConfiguration.Dimension dimension,
        String categoryNameKey,
        String categoryDescKey,
        Screen backScreen,
        Screen[] self) {

        var backend = BackendHolder.getBackend();
        var category = backend.createCategory(categoryNameKey, categoryDescKey);
        
        LayerHolder layerHolder = config.getLayerHolder(dimension);
        if (!CloudsConfiguration.Dimension.isBuiltin(dimension)) {
            category.action(backend.createAction(
                "cloudtweaks.title.delete_dimension",
                "cloudtweaks.desc.delete_dimension",
                () -> {
                    CloudsConfiguration.getInstance().removeLayerHolder(dimension);
                    ClientHelper.setScreen(null);
                }
            ));
        }

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

                    Screen refreshedScreen = createLayerSettingsScreenForDimension(config, dimension, backScreen);
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
                        Screen editScreen = createLayerEditingScreen(dimension, layerIndex, self[0]);
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
                            Screen refreshedScreen = createLayerSettingsScreenForDimension(config, dimension, backScreen);
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
                var presetsManager = LayerPresetsScreenManager.getInstance(dimension);
                presetsManager.setParentScreen(self[0]);
                Screen presetsScreen = presetsManager.buildScreen();
                if (presetsScreen != null) {
                    ClientHelper.setScreen(presetsScreen);
                }
            })
        );
        
        return category;
    }

    

    /**
     * ==========================================================================
     * Layer settings section
     *
     */

    static ConfigBackend.ConfigCategoryBuilder buildLayerSettings(CloudsConfiguration config, Screen[] self) {
        var backend = BackendHolder.getBackend();

        var builder = backend.createCategory("cloudtweaks.category.layers", "cloudtweaks.desc.layers");

        for (CloudsConfiguration.Dimension dimension : CloudsConfiguration.Dimension.values()) {
            builder.action(backend.createAction(
                ComponentWrapper.translatable("cloudtweaks.raw.edit").getString() + " " + dimension.getId(),
                ComponentWrapper.translatable("cloudtweaks.desc.edit_dimension").getString() + " " + dimension.getId(),
                () -> {
                    LayerSettingsScreenManager layerMgr = LayerSettingsScreenManager.getInstance(dimension);
                    layerMgr.setParentScreen(self[0]);
                    ClientHelper.setScreen(layerMgr.buildScreen());
                }
            ));
        }

        return builder;
    }

    public static ConfigBackend.ConfigCategoryBuilder buildLayerBasicSettings(CloudsConfiguration.LayerConfiguration layer) {
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
            ).dropDown(MeshTypeRegistry.getInstance().values().stream().map(mt -> mt.getName()).toList()))
            .option(backend.booleanOption(
                "cloudtweaks.option.fog_enabled",
                null,
                () -> layer.FOG_ENABLED,
                v -> layer.FOG_ENABLED = v
            ));

        return category;
    }

    public static ConfigBackend.ConfigCategoryBuilder buildLayerAppearanceSettings(CloudsConfiguration.LayerConfiguration layer) {
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
            ).range(0.1f, ConfigConstants.MAX_CLOUDS_Y_SCALE)
                .field())
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

    public static ConfigBackend.ConfigCategoryBuilder buildOutlineCategory(CloudsConfiguration.LayerConfiguration layer) {
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

    public static ConfigBackend.ConfigCategoryBuilder buildTextureCategory(CloudsConfiguration.LayerConfiguration layer) {
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

    public static ConfigBackend.ConfigCategoryBuilder buildTypeSpecificCategory(CloudsConfiguration.LayerConfiguration layer, Screen[] self) {
        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.group.type_specific", null);

        var typeConfigs = layer.getAllTypeConfig();
        var mainGroup = backend.createGroup("cloudtweaks.group.type_specific");

        for (var entry : typeConfigs.entrySet()) {
            String typeName = entry.getKey();
            CloudsConfiguration.LayerConfiguration.CustomTypeParameters params = entry.getValue();

            mainGroup.action(backend.createAction(
                typeName,
                null,
                () -> {
                    Screen typeScreen = createTypeSpecificConfigScreen(typeName, params, self[0], layer);
                    if (typeScreen != null) {
                        ClientHelper.setScreen(typeScreen);
                    }
                }
            ));
        }

        category.group(mainGroup);
        return category;
    }

    private static Screen createTypeSpecificConfigScreen(String name, CloudsConfiguration.LayerConfiguration.CustomTypeParameters params, Screen backScreen, CloudsConfiguration.LayerConfiguration layer) {
        var backend = BackendHolder.getBackend();
        var screen = backend.createScreen(name, CloudsConfiguration::save);
        var category = backend.createCategory("cloudtweaks.group.type_specific", null);
        var group = backend.createGroup(name + ComponentWrapper.translatable("cloudtweaks.group.type_specific_suffix").getString());
        
        for (var entry : params.getMap().entrySet()) {
            String entryName = entry.getKey();
            Object value = entry.getValue().value();

            var configInstance = params.getValue(entryName);
            
            if (value instanceof String) {
                group.option(backend.stringOption(
                    entryName,
                    null,
                    () -> (String) configInstance.value(),
                    v -> configInstance.setValue(v)
                ));
            } 
            else if (value instanceof Float) {
                java.util.Optional<Number> fieldMin = params.min(entryName);
                java.util.Optional<Number> fieldMax = params.max(entryName);
                group.option(backend.floatOption(
                    entryName,
                    null,
                    () -> (Float) configInstance.value(),
                    v -> configInstance.setValue(v)
                )
                .field()
                .range(fieldMin.map(Number::floatValue).orElse(Float.NEGATIVE_INFINITY), fieldMax.map(Number::floatValue).orElse(Float.POSITIVE_INFINITY)));
            } 
            else if (value instanceof Integer) {
                java.util.Optional<Number> fieldMin = params.min(entryName);
                java.util.Optional<Number> fieldMax = params.max(entryName);
                group.option(backend.integerOption(
                    entryName,
                    null,
                    () -> (Integer) configInstance.value(),
                    v -> configInstance.setValue(v)
                )
                .field()
                .range(fieldMin.map(Number::intValue).orElse(Integer.MIN_VALUE), fieldMax.map(Number::intValue).orElse(Integer.MAX_VALUE)));
            }
            else if (value instanceof Boolean) {
                group.option(backend.booleanOption(
                    entryName,
                    null,
                    () -> (Boolean) configInstance.value(),
                    v -> configInstance.setValue(v)
                ));
            }
        }

        group.action(backend.createAction(
            "cloudtweaks.raw.remove", 
            "cloudtweaks.action.remove_config", 
            () -> {
                layer.deleteCustomType(name);
                ClientHelper.setScreen(backScreen);
            })
        );

        group.action(backend.createAction(
            "cloudtweaks.raw.reset", 
            "cloudtweaks.action.reset_config", 
            () -> {
                layer.deleteCustomType(name);
                CloudsConfiguration.LayerConfiguration.CustomTypeParameters newParams = layer.resetCustomTypeConfig(name);
                Screen refreshedScreen = createTypeSpecificConfigScreen(name, newParams, backScreen, layer);
                ClientHelper.setScreen(refreshedScreen);
            })
        );

        category.group(group);
        screen.category(category);
        return screen.build(backScreen);
    }

    public static ConfigBackend.ConfigCategoryBuilder buildLayerFogSettings(CloudsConfiguration.LayerConfiguration layer) {
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

    public static ConfigBackend.ConfigCategoryBuilder buildLayerFadeSettings(CloudsConfiguration.LayerConfiguration layer) {
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
            ).enumClass(CloudsConfiguration.LayerConfiguration.FadeType.class))
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



    /**
     * ==========================================================================
     * Presets section
     *
     */

    //
    public static ConfigBackend.ConfigCategoryBuilder buildLayerSaveAsPresetCategory(CloudsConfiguration.LayerConfiguration layer, CloudsConfiguration.Dimension dimension) {
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

    public static ConfigBackend.ConfigCategoryBuilder buildLayerPresetSaveCategory(CloudsConfiguration.LayerConfiguration layer, String presetId, 
                                                               CloudsConfiguration.Dimension dimension, Screen backScreen) {
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

    public static ConfigBackend.ConfigCategoryBuilder createPresetsCategory(Screen[] parentScreen) {
        var backend = BackendHolder.getBackend();
        var builder = backend.createCategory("cloudtweaks.category.presets", "cloudtweaks.category.presets");

        CloudsConfiguration config = CloudsConfiguration.getInstance();

        var quickActions = backend.createGroup("cloudtweaks.presets.quick_actions");

        quickActions
            .action(backend.createAction(
                "cloudtweaks.presets.save_color_preset",
                "cloudtweaks.presets.save_color_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "CloudPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveColorPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_color_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.save_lighting_preset",
                "cloudtweaks.presets.save_lighting_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightingPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightingPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_lighting_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.save_light_sources_preset",
                "cloudtweaks.presets.save_light_sources_preset.tooltip",
                () -> {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String presetName = "LightSourcesPreset_" + formatTimestamp(System.currentTimeMillis());
                    if (PresetController.saveLightSourcesPreset(timestamp, presetName, "Saved via config menu", config)) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.message.saved_light_sources_preset_prefix")
                        );
                    }
                }
            ))
            .action(backend.createAction(
                "cloudtweaks.presets.reset_to_default",
                "cloudtweaks.presets.reset_to_default.tooltip",
                () -> {
                    boolean success = true;
                    success &= PresetController.loadColorPreset("default", config);
                    success &= PresetController.loadLightingPreset("default", config);
                    success &= PresetController.loadLightSourcesPreset("default", config);
                    
                    if (success) {
                        CloudsConfiguration.save();
                        if (RendererHolder.get() != null) RendererHolder.get().markForRebuild();
                        ClientHelper.sendLocalSystemMessage(
                            ComponentWrapper.translatable("cloudtweaks.presets.reset_to_default")
                        );
                    }
                }
            ));

        builder.group(quickActions);

        /**
         * color presets
         **/

        var colorPresets = backend.createGroup("cloudtweaks.presets.color_presets");

        colorPresets
            .action(backend.createAction(
                "cloudtweaks.presets.color_presets",
                "cloudtweaks.presets.color_presets.tooltip",
                () -> {
                    if (parentScreen[0] != null) {
                        PresetBrowserScreenManager browserMgr = PresetBrowserScreenManager.getInstance(PresetController.PresetCategory.COLORS);
                        browserMgr.setParentScreen(parentScreen[0]);
                        ClientHelper.setScreen(browserMgr.buildScreen());
                    }
                }
            ));
        
        builder.group(colorPresets);

        var lightingPresets = backend.createGroup("cloudtweaks.presets.lighting_presets");

        lightingPresets
            .action(backend.createAction(
                "cloudtweaks.presets.lighting_presets",
                "cloudtweaks.presets.lighting_presets.tooltip",
                () -> {
                    if (parentScreen[0] != null) {
                        PresetBrowserScreenManager browserMgr = PresetBrowserScreenManager.getInstance(PresetController.PresetCategory.LIGHTING);
                        browserMgr.setParentScreen(parentScreen[0]);
                        ClientHelper.setScreen(browserMgr.buildScreen());
                    }
                }
            ));
        
        builder.group(lightingPresets);

        /**
         * light sources
         */
        var lightSourcesPresets = backend.createGroup("cloudtweaks.presets.light_sources_presets");

        lightSourcesPresets
            .action(backend.createAction(
                "cloudtweaks.presets.light_sources_presets",
                "cloudtweaks.presets.light_sources_presets.tooltip",
                () -> {
                    if (parentScreen[0] != null) {
                        PresetBrowserScreenManager browserMgr = PresetBrowserScreenManager.getInstance(PresetController.PresetCategory.LIGHT_SOURCES);
                        browserMgr.setParentScreen(parentScreen[0]);
                        ClientHelper.setScreen(browserMgr.buildScreen());
                    }
                }
            ));
        
        builder.group(lightSourcesPresets);

        return builder;
    }
}