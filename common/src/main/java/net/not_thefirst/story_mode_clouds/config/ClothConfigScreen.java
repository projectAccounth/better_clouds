package net.not_thefirst.story_mode_clouds.config;

import me.shedaniel.clothconfig2.ClothConfigDemo;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.SkyColorKeypoint;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.renderer.mesh_builders.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.interp.world.NumberSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.math.Vector3f;

public class ClothConfigScreen {

    static final int MIN_BEVEL_SEGMENTS = 1;
    static final int MAX_BEVEL_SEGMENTS = 32;

    static final int MIN_CELLS_PER_FRAME = 1;
    static final int MAX_CELLS_PER_FRAME = 1024;

    static final int MIN_MESH_REBUILD_BUDGET_MS = 1;
    static final int MAX_MESH_REBUILD_BUDGET_MS = 200;

    static final float MIN_SCALE = 0.1f;
    static final float MAX_SCALE = 10.0f;

    static final float MIN_TRANSITION_RANGE = 0.1f;
    static final float MAX_TRANSITION_RANGE = 100.0f;

    static final float MIN_BRIGHTNESS = 0.0f;
    static final float MAX_BRIGHTNESS = 1.0f;

    static final float MIN_BEVEL_SIZE = 0.0f;
    static final float MAX_BEVEL_SIZE = 15.0f;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(ComponentWrapper.translatable("cloudtweaks.title"))
            .setSavingRunnable(ClothConfigScreen::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        CloudsConfiguration config = CloudsConfiguration.INSTANCE;

        ConfigCategory globalCategory =
            builder.getOrCreateCategory(
                ComponentWrapper.translatable("cloudtweaks.category.global")
            );

        globalCategory.addEntry(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable("cloudtweaks.option.clouds_rendered"),
                config.CLOUDS_RENDERED
            )
            .setDefaultValue(true)
            .setSaveConsumer(value -> config.CLOUDS_RENDERED = value)
            .build()
        );

        globalCategory.addEntry(
            entryBuilder.startIntField(
                ComponentWrapper.translatable("cloudtweaks.option.grid_size"),
                config.CLOUD_GRID_SIZE
            )
            .setDefaultValue(48)
            .setMin(32)
            .setMax(256)
            .setSaveConsumer(value -> config.CLOUD_GRID_SIZE = value)
            .build()
        );

        ConfigCategory lightingCategory =
            builder.getOrCreateCategory(
                ComponentWrapper.translatable("cloudtweaks.category.lighting")
            );

        lightingCategory.addEntry(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.ambient_strength"),
                config.LIGHTING.AMBIENT_LIGHTING_STRENGTH
            )
            .setDefaultValue(0.5f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value ->
                config.LIGHTING.AMBIENT_LIGHTING_STRENGTH =
                    Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value))
            )
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.max_shading"),
                config.LIGHTING.MAX_LIGHTING_SHADING
            )
            .setDefaultValue(0.5f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value ->
                config.LIGHTING.MAX_LIGHTING_SHADING =
                    Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value))
            )
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startEnumSelector(
                ComponentWrapper.translatable("cloudtweaks.option.cloud_provider"),
                CloudsConfiguration.CloudColorProviderMode.class,
                CloudsConfiguration.INSTANCE.COLOR_MODE
            )
            .setSaveConsumer(value -> CloudsConfiguration.INSTANCE.COLOR_MODE = value)
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startEnumSelector(
                ComponentWrapper.translatable("cloudtweaks.option.shading_mode"),
                CloudsConfiguration.ShadingMode.class,
                config.LIGHTING.SHADING_MODE
            )
            .setSaveConsumer(value -> config.LIGHTING.SHADING_MODE = value)
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startColorField(
                ComponentWrapper.translatable("cloudtweaks.option.rain_color"), 
                config.WEATHER_COLOR.rainColor
            )
            .setSaveConsumer(value -> config.WEATHER_COLOR.rainColor = value)
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startColorField(
                ComponentWrapper.translatable("cloudtweaks.option.thunder_color"), 
                config.WEATHER_COLOR.thunderColor
            )
            .setSaveConsumer(value -> config.WEATHER_COLOR.thunderColor = value)
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.rain_strength"), 
                config.WEATHER_COLOR.rainStrength
            )
            .setSaveConsumer(value -> config.WEATHER_COLOR.rainStrength = value)
            .setMin(0)
            .setMax(1)
            .build()
        );

        lightingCategory.addEntry(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.thunder_strength"), 
                config.WEATHER_COLOR.thunderStrength
            )
            .setSaveConsumer(value -> config.WEATHER_COLOR.thunderStrength = value)
            .setMin(0)
            .setMax(1)
            .build()
        );

        lightingCategory.addEntry(
            new NestedListListEntry<>(
                ComponentWrapper.translatable("cloudtweaks.option.light_sources"),
                config.LIGHTING.lights,
                false,
                null,
                newValue -> {
                    config.LIGHTING.lights.clear();
                    config.LIGHTING.lights.addAll(newValue);
                    final int maxLightCount =
                        CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT;
                    if (config.LIGHTING.lights.size() > maxLightCount) {
                        config.LIGHTING.lights
                            .subList(maxLightCount, config.LIGHTING.lights.size())
                            .clear();
                    }
                },
                () -> new ArrayList<>(config.LIGHTING.lights),
                entryBuilder.getResetButtonKey(),
                true,
                false,
                (light, parentEntry) -> buildLightEntry(entryBuilder, light)
            )
        );

        lightingCategory.addEntry(
            new NestedListListEntry<>(
                ComponentWrapper.translatable("cloudtweaks.option.sky_color"),
                config.CLOUD_COLOR,
                false,
                null,
                newValue -> {
                    config.CLOUD_COLOR.clear();
                    config.CLOUD_COLOR.addAll(newValue);
                    config.CLOUD_COLOR.sort((a, b) -> Integer.compare(a.time, b.time));
                    NumberSequence internalSequence =
                        CloudColorProvider.getCurrentSequence();
                    internalSequence.clearKeypoints();

                    for (SkyColorKeypoint kp : newValue) {
                        int c = kp.color;

                        double r = ((c >> 16) & 0xFF) / 255.0;
                        double g = ((c >> 8) & 0xFF) / 255.0;
                        double b = (c & 0xFF) / 255.0;

                        internalSequence.addKeypoint(kp.time, r, g, b);
                    }
                },
                () -> new ArrayList<>(config.CLOUD_COLOR),
                entryBuilder.getResetButtonKey(),
                true,
                true,
                (color, parentEntry) ->
                    buildCloudColorEntries(entryBuilder, color)
            )
        );

        ConfigCategory layersCategory =
            builder.getOrCreateCategory(
                ComponentWrapper.translatable("cloudtweaks.category.layers")
            );

        layersCategory.addEntry(
            new NestedListListEntry<>(
                ComponentWrapper.translatable("cloudtweaks.option.layers"),
                config.getHolder().layers,
                false,
                null,
                newValue -> {
                    config.getHolder().layers.clear();
                    config.getHolder().layers.addAll(newValue);
                    for (int i = 0; i < newValue.size(); i++) {
                        // newValue.get(i).LAYER_IDX = i;
                    }
                },
                () -> new ArrayList<>(config.getHolder().layers),
                entryBuilder.getResetButtonKey(),
                true,
                false,
                (layer, parentEntry) ->
                    buildLayerEntries(entryBuilder, layer)
            )
        );

        ConfigCategory templateCategory =
            builder.getOrCreateCategory(
                ComponentWrapper.translatable("cloudtweaks.category.layer_template")
            );

        templateCategory.addEntry(
            buildLayerEntries(
                entryBuilder,
                CloudsConfiguration.INSTANCE.template
            )
        );

        return builder.build();
    }

    private static AbstractConfigListEntry<CloudsConfiguration.SkyColorKeypoint>
    buildCloudColorEntries(
        ConfigEntryBuilder entryBuilder,
        SkyColorKeypoint keyPoint
    ) {
        if (keyPoint == null) {
            keyPoint = new SkyColorKeypoint();
        }

        return new MultiElementListEntry<>(
            ComponentWrapper.translatable("cloudtweaks.entry.light"),
            keyPoint,
            buildCloudColorEntry(entryBuilder, keyPoint).entries,
            true
        );
    }

    private static ParameterGroup buildCloudColorEntry(
        ConfigEntryBuilder entryBuilder,
        SkyColorKeypoint keyPoint
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.time_of_day"
                ),
                keyPoint.time
            )
            .setDefaultValue(6000)
            .setMin(0)
            .setMax(24000)
            .setSaveConsumer(value -> keyPoint.time = value)
            .build()
        );

        entries.add(
            entryBuilder.startColorField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.cloud_color"
                ),
                keyPoint.color
            )
            .setDefaultValue(0xffffff)
            .setSaveConsumer(value -> keyPoint.color = value)
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.cloud_colors",
            entries
        );
    }

    private static ParameterGroup buildLightList(
        ConfigEntryBuilder entryBuilder,
        DiffuseLight light
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.position_x"),
                light.location.x()
            )
            .setDefaultValue(0.0f)
            .setMin(-32767.0f)
            .setMax(32767.0f)
            .setSaveConsumer(value ->
                light.location =
                    new Vector3f(
                        value,
                        light.location.y(),
                        light.location.z()
                    )
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.position_y"),
                light.location.y()
            )
            .setDefaultValue(1.0f)
            .setMin(-32767.0f)
            .setMax(32767.0f)
            .setSaveConsumer(value ->
                light.location =
                    new Vector3f(
                        light.location.x(),
                        value,
                        light.location.z()
                    )
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.position_z"),
                light.location.z()
            )
            .setDefaultValue(0.0f)
            .setMin(-32767.0f)
            .setMax(32767.0f)
            .setSaveConsumer(value ->
                light.location =
                    new Vector3f(
                        light.location.x(),
                        light.location.y(),
                        value
                    )
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.intensity"),
                light.intensity
            )
            .setDefaultValue(1.0f)
            .setMin(0.0f)
            .setMax(10.0f)
            .setSaveConsumer(value -> light.intensity = value)
            .build()
        );

        return new ParameterGroup("cloudtweaks.group.light", entries);
    }

    private static MultiElementListEntry<DiffuseLight> buildLightEntry(
        ConfigEntryBuilder entryBuilder,
        DiffuseLight light
    ) {
        if (light == null) {
            light = new DiffuseLight();
        }

        return new MultiElementListEntry<>(
            ComponentWrapper.translatable("cloudtweaks.entry.light"),
            light,
            buildLightList(entryBuilder, light).entries,
            true
        );
    }

    private static MultiElementListEntry<CloudsConfiguration.LayerConfiguration>
    buildLayerEntries(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        if (layer == null) {
            layer = new CloudsConfiguration.LayerConfiguration();
            layer.copy(CloudsConfiguration.INSTANCE.template);
        }

        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        List<ParameterGroup> parameterGroups = new ArrayList<>();
        parameterGroups.add(buildBasicSettingsGroup(entryBuilder, layer));
        parameterGroups.add(buildAppearanceGroup(entryBuilder, layer));
        parameterGroups.add(buildAlphaBrightnessGroup(entryBuilder, layer));
        parameterGroups.add(buildFadeGroup(entryBuilder, layer));
        parameterGroups.add(buildFogGroup(entryBuilder, layer));
        parameterGroups.add(buildBevelGroup(entryBuilder, layer));
        parameterGroups.add(buildPerformanceGroup(entryBuilder, layer));

        entries.add(
            new NestedListListEntry<>(
                ComponentWrapper.translatable("cloudtweaks.option.parameters"),
                parameterGroups,
                false,
                null,
                newValue -> {},
                () -> new ArrayList<>(parameterGroups),
                entryBuilder.getResetButtonKey(),
                false,
                false,
                (group, parent) ->
                    new MultiElementListEntry<>(
                        ComponentWrapper.translatable(group.name),
                        group,
                        group.entries,
                        true
                    )
            )
        );

        return new MultiElementListEntry<>(
            ComponentWrapper.literal(
                "Layer " + (layer.GetLayerIndex() + 1) + ": " + layer.NAME
            ),
            layer,
            entries,
            true
        );
    }

    private static ParameterGroup buildBasicSettingsGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startStrField(
                ComponentWrapper.translatable("cloudtweaks.option.name"),
                layer.NAME
            )
            .setDefaultValue("Minecraft")
            .setSaveConsumer(value -> layer.NAME = value)
            .build()
        );

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable("cloudtweaks.option.enabled"),
                layer.IS_ENABLED
            )
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.IS_ENABLED = value)
            .build()
        );

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable("cloudtweaks.option.rendered"),
                layer.LAYER_RENDERED
            )
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.LAYER_RENDERED = value)
            .build()
        );

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable("cloudtweaks.option.height"),
                layer.LAYER_HEIGHT
            )
            .setDefaultValue(128)
            .setSaveConsumer(value -> layer.LAYER_HEIGHT = value)
            .build()
        );

        entries.add(
            entryBuilder.startStringDropdownMenu(
                ComponentWrapper.translatable("cloudtweaks.option.cloud_mode"),
                layer.MODE
            )
            .setDefaultValue("NORMAL")
            .setSelections(
                MeshBuilderRegistry.getInstance()
                    .keys()
                    .stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            )
            .setSaveConsumer(value -> layer.MODE = value)
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.basic",
            entries
        );
    }

    private static ParameterGroup buildAppearanceGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.shading_enabled"
                ),
                layer.APPEARANCE.SHADING_ENABLED
            )
            .setDefaultValue(false)
            .setSaveConsumer(
                value -> layer.APPEARANCE.SHADING_ENABLED = value
            )
            .setTooltip(
                ComponentWrapper.translatable(
                    "cloudtweaks.tooltip.shading_enabled"
                )
            )
            .build()
        );

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.use_custom_color"
                ),
                layer.APPEARANCE.USES_CUSTOM_COLOR
            )
            .setDefaultValue(false)
            .setSaveConsumer(
                value -> layer.APPEARANCE.USES_CUSTOM_COLOR = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startColorField(
                ComponentWrapper.translatable("cloudtweaks.option.color"),
                layer.APPEARANCE.LAYER_COLOR
            )
            .setDefaultValue(0xffffff)
            .setSaveConsumer(
                value -> layer.APPEARANCE.LAYER_COLOR = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.y_scale"),
                layer.APPEARANCE.CLOUD_Y_SCALE
            )
            .setDefaultValue(1.5f)
            .setMin(MIN_SCALE)
            .setMax(MAX_SCALE)
            .setSaveConsumer(value ->
                layer.APPEARANCE.CLOUD_Y_SCALE =
                    Math.max(MIN_SCALE, Math.min(MAX_SCALE, value))
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable("cloudtweaks.option.offset_x"),
                layer.APPEARANCE.LAYER_OFFSET_X
            )
            .setDefaultValue(0)
            .setSaveConsumer(
                value -> layer.APPEARANCE.LAYER_OFFSET_X = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable("cloudtweaks.option.offset_z"),
                layer.APPEARANCE.LAYER_OFFSET_Z
            )
            .setDefaultValue(0)
            .setSaveConsumer(
                value -> layer.APPEARANCE.LAYER_OFFSET_Z = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.speed_x"),
                layer.APPEARANCE.LAYER_SPEED_X
            )
            .setDefaultValue(0.03f)
            .setMin(-100.0f)
            .setMax(100.0f)
            .setTooltip(
                ComponentWrapper.translatable(
                    "cloudtweaks.tooltip.speed_warning"
                )
            )
            .setSaveConsumer(
                value -> layer.APPEARANCE.LAYER_SPEED_X = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable("cloudtweaks.option.speed_z"),
                layer.APPEARANCE.LAYER_SPEED_Z
            )
            .setDefaultValue(0.03f)
            .setMin(-100.0f)
            .setMax(100.0f)
            .setTooltip(
                ComponentWrapper.translatable(
                    "cloudtweaks.tooltip.speed_warning"
                )
            )
            .setSaveConsumer(
                value -> layer.APPEARANCE.LAYER_SPEED_Z = value
            )
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.appearance",
            entries
        );
    }

    private static ParameterGroup buildAlphaBrightnessGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.use_custom_alpha"
                ),
                layer.APPEARANCE.USES_CUSTOM_ALPHA
            )
            .setDefaultValue(true)
            .setSaveConsumer(
                value -> layer.APPEARANCE.USES_CUSTOM_ALPHA = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntSlider(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.base_alpha"
                ),
                layer.APPEARANCE.BASE_ALPHA,
                0,
                255
            )
            .setDefaultValue((int)(0.8f * 255))
            .setSaveConsumer(
                value -> layer.APPEARANCE.BASE_ALPHA = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.custom_brightness"
                ),
                layer.APPEARANCE.CUSTOM_BRIGHTNESS
            )
            .setDefaultValue(true)
            .setSaveConsumer(
                value -> layer.APPEARANCE.CUSTOM_BRIGHTNESS = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.brightness"
                ),
                layer.APPEARANCE.BRIGHTNESS
            )
            .setDefaultValue(1.0f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value ->
                layer.APPEARANCE.BRIGHTNESS =
                    Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value))
            )
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.alpha_brightness",
            entries
        );
    }

    private static ParameterGroup buildFadeGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.fade_enabled"
                ),
                layer.FADE.FADE_ENABLED
            )
            .setDefaultValue(true)
            .setSaveConsumer(
                value -> layer.FADE.FADE_ENABLED = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntSlider(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.fade_alpha"
                ),
                layer.FADE.FADE_ALPHA,
                0,
                255
            )
            .setDefaultValue((int)(0.2f * 255))
            .setSaveConsumer(
                value -> layer.FADE.FADE_ALPHA = value
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.transition_range"
                ),
                layer.FADE.TRANSITION_RANGE
            )
            .setDefaultValue(10.0f)
            .setMin(MIN_TRANSITION_RANGE)
            .setMax(MAX_TRANSITION_RANGE)
            .setSaveConsumer(value ->
                layer.FADE.TRANSITION_RANGE =
                    Math.max(
                        MIN_TRANSITION_RANGE,
                        Math.min(MAX_TRANSITION_RANGE, value)
                    )
            )
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.fade",
            entries
        );
    }

    private static ParameterGroup buildFogGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startBooleanToggle(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.fog_enabled"
                ),
                layer.FOG_ENABLED
            )
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.FOG_ENABLED = value)
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.fog_start"
                ),
                layer.FOG.FOG_START_DISTANCE
            )
            .setDefaultValue(70.0f)
            .setMin(0.0f)
            .setMax(16777216)
            .setSaveConsumer(value ->
                layer.FOG.FOG_START_DISTANCE =
                    Math.max(0.0f, Math.min(16777216, value))
            )
            .build()
        );

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.fog_end"
                ),
                layer.FOG.FOG_END_DISTANCE
            )
            .setDefaultValue(400.0f)
            .setMin(0.0f)
            .setMax(16777216)
            .setSaveConsumer(value ->
                layer.FOG.FOG_END_DISTANCE =
                    Math.max(0.0f, Math.min(16777216, value))
            )
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.fog",
            entries
        );
    }

    private static ParameterGroup buildBevelGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(
            entryBuilder.startFloatField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.bevel_size"
                ),
                layer.BEVEL.BEVEL_SIZE
            )
            .setDefaultValue(0.1f)
            .setMin(MIN_BEVEL_SIZE)
            .setMax(MAX_BEVEL_SIZE)
            .setSaveConsumer(value ->
                layer.BEVEL.BEVEL_SIZE =
                    Math.max(
                        MIN_BEVEL_SIZE,
                        Math.min(MAX_BEVEL_SIZE, value)
                    )
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.edge_segments"
                ),
                layer.BEVEL.BEVEL_EDGE_SEGMENTS
            )
            .setDefaultValue(8)
            .setMin(MIN_BEVEL_SEGMENTS)
            .setMax(MAX_BEVEL_SEGMENTS)
            .setSaveConsumer(value ->
                layer.BEVEL.BEVEL_EDGE_SEGMENTS =
                    Math.max(
                        MIN_BEVEL_SEGMENTS,
                        Math.min(MAX_BEVEL_SEGMENTS, value)
                    )
            )
            .build()
        );

        entries.add(
            entryBuilder.startIntField(
                ComponentWrapper.translatable(
                    "cloudtweaks.option.corner_segments"
                ),
                layer.BEVEL.BEVEL_CORNER_SEGMENTS
            )
            .setDefaultValue(8)
            .setMin(MIN_BEVEL_SEGMENTS)
            .setMax(MAX_BEVEL_SEGMENTS)
            .setSaveConsumer(value ->
                layer.BEVEL.BEVEL_CORNER_SEGMENTS =
                    Math.max(
                        MIN_BEVEL_SEGMENTS,
                        Math.min(MAX_BEVEL_SEGMENTS, value)
                    )
            )
            .build()
        );

        return new ParameterGroup(
            "cloudtweaks.group.bevel",
            entries
        );
    }

    private static ParameterGroup buildPerformanceGroup(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer
    ) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        return new ParameterGroup(
            "cloudtweaks.group.performance",
            entries
        );
    }

    private static void saveConfig() {
        CloudsConfiguration.save();
        if (RendererHolder.get() != null) {
            RendererHolder.get().markForRebuild();
        }
    }

    private static class ParameterGroup {
        final String name;
        final List<AbstractConfigListEntry<?>> entries;

        ParameterGroup(String name, List<AbstractConfigListEntry<?>> entries) {
            this.name = name;
            this.entries = Collections.unmodifiableList(entries);
        }
    }
}
