package net.not_thefirst.story_mode_clouds.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.*;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;

import java.awt.Color;
import java.util.List;

/**
 * sdjsdjsjdsjd
 */
public class YACLDataSettings {

    public static ConfigCategory buildSkyColorSettings(CloudsConfiguration config) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.sky_color"));
        
        List<SkyColorKeypoint> colors = config.CLOUD_COLOR;
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.option.add_keypoint"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.add_keypoint.description")))
            .action((yacl, btn) -> {
                if (colors.size() < 32) {
                    SkyColorKeypoint newKeypoint = new SkyColorKeypoint();
                    newKeypoint.time = colors.isEmpty() ? 0 : colors.get(colors.size() - 1).time + 1000;
                    newKeypoint.color = 0xFFFFFF;
                    colors.add(newKeypoint);
                    CloudsConfiguration.save();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.player != null) {
                        mc.player.displayClientMessage(
                            ComponentWrapper.literal("Added keypoint at time " + newKeypoint.time),
                            true
                        );
                    }
                    if (mc != null) {
                        mc.setScreen(null);
                    }
                }
            })
            .build());
        
        for (int i = 0; i < colors.size(); i++) {
            SkyColorKeypoint kp = colors.get(i);
            
            var groupBuilder = OptionGroup.createBuilder()
                .name(ComponentWrapper.literal("Keypoint " + (i + 1) + " (Time: " + kp.time + ")"))
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.time"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.time_of_day")))
                    .binding(kp.time, () -> kp.time, v -> kp.time = v)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 24000).step(100))
                    .build())
                
                .option(Option.<Color>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.color"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.option.cloud_color")))
                    .binding(
                        new Color(kp.color),
                        () -> new Color(kp.color),
                        v -> kp.color = v.getRGB()
                    )
                    .controller(opt -> ColorControllerBuilder.create(opt))
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.raw.remove"))
                    .description(OptionDescription.of(ComponentWrapper.literal("Delete this keypoint")))
                    .action((yacl, btn) -> {
                        if (colors.size() > 1 && colors.remove(kp)) {
                            CloudsConfiguration.save();
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("Removed keypoint"),
                                    true
                                );
                            }
                            if (mc != null) {
                                mc.setScreen(null);
                            }
                        }
                    })
                    .build());
            
            builder.group(groupBuilder.build());
        }
        
        return builder.build();
    }

    public static ConfigCategory buildLightSourcesSettings(CloudsConfiguration config) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.lighting"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.tooltip.light"));
        
        List<DiffuseLight> lights = config.LIGHTING.lights;
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.raw.add_light"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.add_light")))
            .action((yacl, btn) -> {
                DiffuseLight newLight = new DiffuseLight();
                lights.add(newLight);
                CloudsConfiguration.save();
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    mc.player.displayClientMessage(
                        ComponentWrapper.literal("Added light source"),
                        true
                    );
                }
                if (mc != null) {
                    mc.setScreen(null);
                }
            })
            .build());
        
        for (int i = 0; i < lights.size(); i++) {
            DiffuseLight light = lights.get(i);
            
            var groupBuilder = OptionGroup.createBuilder()
                .name(ComponentWrapper.literal("Light " + (i + 1)))
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
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("Removed light source"),
                                    true
                                );
                            }
                            if (mc != null) {
                                mc.setScreen(null);
                            }
                        }
                    })
                    .build());
            
            builder.group(groupBuilder.build());
        }
        
        return builder.build();
    }

    public static ConfigCategory buildLayersSettings(CloudsConfiguration config) {
        var builder = ConfigCategory.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.category.layers"))
            .tooltip(ComponentWrapper.translatable("cloudtweaks.desc.layers"));
        
        builder.option(ButtonOption.createBuilder()
            .name(ComponentWrapper.translatable("cloudtweaks.title.add_layer"))
            .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.add_layer")))
            .action((yacl, btn) -> {
                LayerConfiguration newLayer = new LayerConfiguration(config.getLayerCount());
                config.getHolder().addLayer(newLayer);
                CloudsConfiguration.save();
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    mc.player.displayClientMessage(
                        ComponentWrapper.literal("Added layer " + config.getLayerCount()),
                        true
                    );
                }
                if (mc != null) {
                    mc.setScreen(null);
                }
            })
            .build());
        
        int layerCount = config.getLayerCount();
        for (int i = 0; i < layerCount; i++) {
            LayerConfiguration layer = config.getLayer(i);
            final int layerIndex = i;
            
            var layerGroup = OptionGroup.createBuilder()
                .name(ComponentWrapper.literal("Layer " + (i + 1) + ": " + layer.NAME))
                
                .option(Option.<String>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.name"))
                    .binding(layer.NAME, () -> layer.NAME, v -> layer.NAME = v)
                    .controller(opt -> StringControllerBuilder.create(opt))
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
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 256).step(1))
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
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.05f))
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
                    .controller(opt -> ColorControllerBuilder.create(opt))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.y_scale"))
                    .binding(layer.APPEARANCE.CLOUD_Y_SCALE, () -> layer.APPEARANCE.CLOUD_Y_SCALE, v -> layer.APPEARANCE.CLOUD_Y_SCALE = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 10.0f).step(0.1f))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.offset_x"))
                    .binding(layer.APPEARANCE.LAYER_OFFSET_X, () -> layer.APPEARANCE.LAYER_OFFSET_X, v -> layer.APPEARANCE.LAYER_OFFSET_X = v)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-1000, 1000).step(1))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.offset_z"))
                    .binding(layer.APPEARANCE.LAYER_OFFSET_Z, () -> layer.APPEARANCE.LAYER_OFFSET_Z, v -> layer.APPEARANCE.LAYER_OFFSET_Z = v)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-1000, 1000).step(1))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.speed_x"))
                    .binding(layer.APPEARANCE.LAYER_SPEED_X, () -> layer.APPEARANCE.LAYER_SPEED_X, v -> layer.APPEARANCE.LAYER_SPEED_X = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 0.5f).step(0.01f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.speed_z"))
                    .binding(layer.APPEARANCE.LAYER_SPEED_Z, () -> layer.APPEARANCE.LAYER_SPEED_Z, v -> layer.APPEARANCE.LAYER_SPEED_Z = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 0.5f).step(0.01f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.bevel_size"))
                    .binding(layer.BEVEL.BEVEL_SIZE, () -> layer.BEVEL.BEVEL_SIZE, v -> layer.BEVEL.BEVEL_SIZE = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 1.0f).step(0.05f))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.edge_segments"))
                    .binding(layer.BEVEL.BEVEL_EDGE_SEGMENTS, () -> layer.BEVEL.BEVEL_EDGE_SEGMENTS, v -> layer.BEVEL.BEVEL_EDGE_SEGMENTS = v)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 32).step(1))
                    .build())
                
                .option(Option.<Integer>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.corner_segments"))
                    .binding(layer.BEVEL.BEVEL_CORNER_SEGMENTS, () -> layer.BEVEL.BEVEL_CORNER_SEGMENTS, v -> layer.BEVEL.BEVEL_CORNER_SEGMENTS = v)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 32).step(1))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.fog_start"))
                    .binding(layer.FOG.FOG_START_DISTANCE, () -> layer.FOG.FOG_START_DISTANCE, v -> layer.FOG.FOG_START_DISTANCE = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(1.0f, 500.0f).step(5.0f))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.fog_end"))
                    .binding(layer.FOG.FOG_END_DISTANCE, () -> layer.FOG.FOG_END_DISTANCE, v -> layer.FOG.FOG_END_DISTANCE = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(1.0f, 500.0f).step(5.0f))
                    .build())
                
                .option(Option.<Boolean>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.fade_enabled"))
                    .binding(layer.FADE.FADE_ENABLED, () -> layer.FADE.FADE_ENABLED, v -> layer.FADE.FADE_ENABLED = v)
                    .controller(BooleanControllerBuilder::create)
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
                    .controller(opt -> ColorControllerBuilder.create(opt))
                    .build())
                
                .option(Option.<Float>createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.transition_range"))
                    .binding(layer.FADE.TRANSITION_RANGE, () -> layer.FADE.TRANSITION_RANGE, v -> layer.FADE.TRANSITION_RANGE = v)
                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 100.0f).step(0.5f))
                    .build())
                
                .option(ButtonOption.createBuilder()
                    .name(ComponentWrapper.translatable("cloudtweaks.option.remove_layer"))
                    .description(OptionDescription.of(ComponentWrapper.translatable("cloudtweaks.desc.remove_layer")))
                    .action((yacl, btn) -> {
                        if (config.getLayerCount() > 1) {
                            config.getHolder().removeLayer(layerIndex);
                            CloudsConfiguration.save();
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.displayClientMessage(
                                    ComponentWrapper.literal("Removed layer"),
                                    true
                                );
                            }
                            if (mc != null) {
                                mc.setScreen(null);
                            }
                        }
                    })
                    .build());
            
            builder.group(layerGroup.build());
        }
        
        return builder.build();
    }
}
