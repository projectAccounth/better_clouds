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
            .setTitle(Component.literal("Cloud Tweaks"))
            .setSavingRunnable(ClothConfigScreen::saveConfig);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        CloudsConfiguration config = CloudsConfiguration.INSTANCE;
        
        ConfigCategory globalCategory = builder.getOrCreateCategory(Component.literal("Global"));
        globalCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Clouds Rendered"),
                config.CLOUDS_RENDERED)
            .setDefaultValue(true)
            .setSaveConsumer(value -> config.CLOUDS_RENDERED = value)
            .build());
            
        ConfigCategory lightingCategory = builder.getOrCreateCategory(Component.literal("Lighting"));
        lightingCategory.addEntry(entryBuilder.startFloatField(
                Component.literal("Ambient Strength"),
                config.LIGHTING.AMBIENT_LIGHTING_STRENGTH)
            .setDefaultValue(0.5f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value -> config.LIGHTING.AMBIENT_LIGHTING_STRENGTH = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value)))
            .build());
        lightingCategory.addEntry(entryBuilder.startFloatField(
                Component.literal("Max Shading"),
                config.LIGHTING.MAX_LIGHTING_SHADING)
            .setDefaultValue(0.5f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value -> config.LIGHTING.MAX_LIGHTING_SHADING = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value)))
            .build());
        lightingCategory.addEntry(entryBuilder.startEnumSelector(
            Component.literal("Cloud Provider"), 
            CloudsConfiguration.CloudColorProviderMode.class, 
            CloudsConfiguration.INSTANCE.COLOR_MODE
        )
        .setSaveConsumer(value -> CloudsConfiguration.INSTANCE.COLOR_MODE = value).
        build());

        lightingCategory.addEntry(new NestedListListEntry<>(
            Component.literal("Light sources"),
            config.LIGHTING.lights,
            false,
            null,
            newValue -> {
                config.LIGHTING.lights.clear();
                config.LIGHTING.lights.addAll(newValue);
            },
            () -> new ArrayList<>(config.LIGHTING.lights),
            entryBuilder.getResetButtonKey(),
            true,
            false,
            (light, parentEntry) -> buildLightEntry(entryBuilder, light)
        ));

        lightingCategory.addEntry(new NestedListListEntry<>(
            Component.literal("Sky Color"),
            config.CLOUD_COLOR,
            false,
            null,
            newValue -> {
                config.CLOUD_COLOR.clear();
                config.CLOUD_COLOR.addAll(newValue);
                config.CLOUD_COLOR.sort((a, b) -> Integer.compare(a.time, b.time));
                NumberSequence internalSequence = CloudColorProvider.getCurrentSequence();
                internalSequence.clearKeypoints();

                for (SkyColorKeypoint kp : newValue) {
                    int c = kp.color;

                    double r = ((c >> 16) & 0xFF) / 255.0;
                    double g = ((c >> 8) & 0xFF) / 255.0;
                    double b = (c & 0xFF) / 255.0;

                    internalSequence.addKeypoint(
                        kp.time,
                        r,
                        g,
                        b
                    );
                }
            },
            () -> new ArrayList<>(config.CLOUD_COLOR),
            entryBuilder.getResetButtonKey(),
            true,
            true,
            (color, parentEntry) -> buildCloudColorEntries(entryBuilder, color)
        ));

        ConfigCategory layersCategory = builder.getOrCreateCategory(Component.literal("Layers"));
        
        layersCategory.addEntry(new NestedListListEntry<>(
            Component.literal("Layers"),
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
            (layer, parentEntry) -> buildLayerEntries(entryBuilder, layer)
        ));
        
        ConfigCategory templateCategory = builder.getOrCreateCategory(Component.literal("Layer Template"));
        templateCategory.addEntry(buildLayerEntries(entryBuilder, CloudsConfiguration.INSTANCE.template));
        
        return builder.build();
    }

    private static AbstractConfigListEntry<CloudsConfiguration.SkyColorKeypoint> buildCloudColorEntries(
        ConfigEntryBuilder entryBuilder,
        SkyColorKeypoint keyPoint
    ) {
        if (keyPoint == null) {
            keyPoint = new SkyColorKeypoint();
        }

        return new MultiElementListEntry<>(
            Component.literal("Light"),
            keyPoint,
            buildCloudColorEntry(entryBuilder, keyPoint).entries,
            true
        );
    }

    private static ParameterGroup buildCloudColorEntry(
        ConfigEntryBuilder entryBuilder, 
        SkyColorKeypoint keyPoint) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        
        entries.add(entryBuilder.startIntField(
                Component.literal("Time of Day (0-24000)"),
                keyPoint.time)
            .setDefaultValue(6000)
            .setMin(0)
            .setMax(24000)
            .setSaveConsumer(value -> {
                keyPoint.time = value;
            })
            .build(
        ));

        entries.add(entryBuilder.startColorField(
                Component.literal("Cloud Color"),
                keyPoint.color)
            .setDefaultValue(0xffffff)
            .setSaveConsumer(value -> {
                keyPoint.color = value;
            })
            .build(
        ));

        return new ParameterGroup("Cloud Colors", entries);
    }

    private static ParameterGroup buildLightList(
        ConfigEntryBuilder entryBuilder,
        DiffuseLight light
    ) {
        
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        
        entries.add(entryBuilder.startFloatField(
                Component.literal("Direction X"),
                light.direction.x)
            .setDefaultValue(0.0f)
            .setMin(-1.0f)
            .setMax(1.0f)
            .setSaveConsumer(value -> light.direction.x = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Direction Y"),
                light.direction.y)
            .setDefaultValue(1.0f)
            .setMin(-1.0f)
            .setMax(1.0f)
            .setSaveConsumer(value -> light.direction.y = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Direction Z"),
                light.direction.z)
            .setDefaultValue(0.0f)
            .setMin(-1.0f)
            .setMax(1.0f)
            .setSaveConsumer(value -> light.direction.z = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Intensity"),
                light.intensity)
            .setDefaultValue(1.0f)
            .setMin(0.0f)
            .setMax(10.0f)
            .setSaveConsumer(value -> light.intensity = value)
            .build());
        
        return new ParameterGroup("Light", entries);
    }

    private static MultiElementListEntry<DiffuseLight> buildLightEntry(
        ConfigEntryBuilder entryBuilder,
        DiffuseLight light
    ) {
        if (light == null) {
            light = new DiffuseLight();
        }

        return new MultiElementListEntry<>(
            Component.literal("Light"),
            light,
            buildLightList(entryBuilder, light).entries,
            true
        );
    }

    private static MultiElementListEntry<CloudsConfiguration.LayerConfiguration> buildLayerEntries(
        ConfigEntryBuilder entryBuilder,
        CloudsConfiguration.LayerConfiguration layer) {
        
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
        
        entries.add(new NestedListListEntry<>(
            Component.literal("Parameters"),
            parameterGroups,
            false,
            null,
            newValue -> {},
            () -> new ArrayList<>(parameterGroups),
            entryBuilder.getResetButtonKey(),
            false,
            false,
            (group, parent) -> new MultiElementListEntry<>(
                Component.literal(group.name),
                group,
                group.entries,
                true
            )
        ));
        
        return new MultiElementListEntry<>(
            Component.literal("Layer " + (layer.GetLayerIndex() + 1) + ": " + layer.NAME),
            layer,
            entries,
            true
        );
    }
    
    private static ParameterGroup buildBasicSettingsGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startStrField(
                Component.literal("Name"),
                layer.NAME)
            .setDefaultValue("Minecraft")
            .setSaveConsumer(value -> layer.NAME = value)
            .build());
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Enabled"),
                layer.IS_ENABLED)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.IS_ENABLED = value)
            .build());
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Rendered"),
                layer.LAYER_RENDERED)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.LAYER_RENDERED = value)
            .build());
        entries.add(entryBuilder.startIntField(
                Component.literal("Height"),
                layer.LAYER_HEIGHT)
            .setDefaultValue(128)
            .setSaveConsumer(value -> layer.LAYER_HEIGHT = value)
            .build());
        entries.add(entryBuilder
            .startStringDropdownMenu(
                Component.literal("Cloud Mode"),
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
            .build());

        return new ParameterGroup("Basic Settings", entries);
    }
    
    private static ParameterGroup buildAppearanceGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Shading Enabled"),
                layer.APPEARANCE.SHADING_ENABLED)
            .setDefaultValue(false)
            .setSaveConsumer(value -> layer.APPEARANCE.SHADING_ENABLED = value)
            .setTooltip(Component.literal("Enable shading when sun is behind clouds"))
            .build());
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Use Custom Color"),
                layer.APPEARANCE.USES_CUSTOM_COLOR)
            .setDefaultValue(false)
            .setSaveConsumer(value -> layer.APPEARANCE.USES_CUSTOM_COLOR = value)
            .build());
        entries.add(entryBuilder.startColorField(
                Component.literal("Color"),
                layer.APPEARANCE.LAYER_COLOR)
            .setDefaultValue(0xffffff)
            .setSaveConsumer(value -> layer.APPEARANCE.LAYER_COLOR = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Y Scale"),
                layer.APPEARANCE.CLOUD_Y_SCALE)
            .setDefaultValue(1.5f)
            .setMin(MIN_SCALE)
            .setMax(MAX_SCALE)
            .setSaveConsumer(value -> layer.APPEARANCE.CLOUD_Y_SCALE = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value)))
            .build());
        entries.add(entryBuilder.startIntField(
                Component.literal("Offset X"),
                layer.APPEARANCE.LAYER_OFFSET_X)
            .setDefaultValue(0)
            .setSaveConsumer(value -> layer.APPEARANCE.LAYER_OFFSET_X = value)
            .build());
        entries.add(entryBuilder.startIntField(
                Component.literal("Offset Z"),
                layer.APPEARANCE.LAYER_OFFSET_Z)
            .setDefaultValue(0)
            .setSaveConsumer(value -> layer.APPEARANCE.LAYER_OFFSET_Z = value)
            .build());

        entries.add(entryBuilder.startFloatField(
                Component.literal("Speed X"),
                layer.APPEARANCE.LAYER_SPEED_X)
            .setDefaultValue(0.03f)
            .setMin(-100.0f)
            .setMax(100.0f)
            .setTooltip(Component.literal("Set it to a small value, be careful not to fry your CPU!"))
            .setSaveConsumer(value -> layer.APPEARANCE.LAYER_SPEED_X = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Speed Z"),
                layer.APPEARANCE.LAYER_SPEED_Z)
            .setDefaultValue(0.03f)
            .setMin(-100.0f)
            .setMax(100.0f)
            .setTooltip(Component.literal("Set it to a small value, be careful not to fry your CPU!"))
            .setSaveConsumer(value -> layer.APPEARANCE.LAYER_SPEED_Z = value)
            .build());
        return new ParameterGroup("Appearance", entries);
    }
    
    private static ParameterGroup buildAlphaBrightnessGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Use Custom Alpha"),
                layer.APPEARANCE.USES_CUSTOM_ALPHA)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.APPEARANCE.USES_CUSTOM_ALPHA = value)
            .build());
        entries.add(entryBuilder.startIntSlider(
                Component.literal("Base Alpha"),
                layer.APPEARANCE.BASE_ALPHA,
                0, 255)
            .setDefaultValue((int)(0.8f * 255))
            .setSaveConsumer(value -> layer.APPEARANCE.BASE_ALPHA = value)
            .build());
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Custom Brightness"),
                layer.APPEARANCE.CUSTOM_BRIGHTNESS)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.APPEARANCE.CUSTOM_BRIGHTNESS = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Brightness"),
                layer.APPEARANCE.BRIGHTNESS)
            .setDefaultValue(1.0f)
            .setMin(MIN_BRIGHTNESS)
            .setMax(MAX_BRIGHTNESS)
            .setSaveConsumer(value -> layer.APPEARANCE.BRIGHTNESS = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, value)))
            .build());
        return new ParameterGroup("Alpha & Brightness", entries);
    }
    
    private static ParameterGroup buildFadeGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Fade Enabled"),
                layer.FADE.FADE_ENABLED)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.FADE.FADE_ENABLED = value)
            .build());
        entries.add(entryBuilder.startIntSlider(
                Component.literal("Fade Alpha"),
                layer.FADE.FADE_ALPHA,
                0, 255)
            .setDefaultValue((int)(0.2f * 255))
            .setSaveConsumer(value -> layer.FADE.FADE_ALPHA = value)
            .build());
        entries.add(entryBuilder.startFloatField(
                Component.literal("Transition Range"),
                layer.FADE.TRANSITION_RANGE)
            .setDefaultValue(10.0f)
            .setMin(MIN_TRANSITION_RANGE)
            .setMax(MAX_TRANSITION_RANGE)
            .setSaveConsumer(value -> layer.FADE.TRANSITION_RANGE = Math.max(MIN_TRANSITION_RANGE, Math.min(MAX_TRANSITION_RANGE, value)))
            .build());

        return new ParameterGroup("Fade", entries);
    }
    
    private static ParameterGroup buildFogGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startBooleanToggle(
                Component.literal("Fog Enabled"),
                layer.FOG_ENABLED)
            .setDefaultValue(true)
            .setSaveConsumer(value -> layer.FOG_ENABLED = value)
            .build());

        entries.add(entryBuilder.startFloatField(
                Component.literal("Fog Start"),
                layer.FOG.FOG_START_DISTANCE)
            .setDefaultValue(70.0f)
            .setMin(0.0f)
            .setMax(16777216)
            .setSaveConsumer(value -> layer.FOG.FOG_START_DISTANCE = Math.max(0.0f, Math.min(16777216, value)))
            .build());

        entries.add(entryBuilder.startFloatField(
                Component.literal("Fog End"),
                layer.FOG.FOG_END_DISTANCE)
            .setDefaultValue(400.0f)
            .setMin(0.0f)
            .setMax(16777216)
            .setSaveConsumer(value -> layer.FOG.FOG_END_DISTANCE = Math.max(0.0f, Math.min(16777216, value)))
            .build());
        return new ParameterGroup("Fog", entries);
    }
    
    private static ParameterGroup buildBevelGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startFloatField(
                Component.literal("Bevel Size"),
                layer.BEVEL.BEVEL_SIZE)
            .setDefaultValue(0.1f)
            .setMin(MIN_BEVEL_SIZE)
            .setMax(MAX_BEVEL_SIZE)
            .setSaveConsumer(value -> layer.BEVEL.BEVEL_SIZE = Math.max(MIN_BEVEL_SIZE, Math.min(MAX_BEVEL_SIZE, value)))
            .build());
        entries.add(entryBuilder.startIntField(
                Component.literal("Edge Segments"),
                layer.BEVEL.BEVEL_EDGE_SEGMENTS)
            .setDefaultValue(8)
            .setMin(MIN_BEVEL_SEGMENTS)
            .setMax(MAX_BEVEL_SEGMENTS)
            .setSaveConsumer(value -> layer.BEVEL.BEVEL_EDGE_SEGMENTS = Math.max(MIN_BEVEL_SEGMENTS, Math.min(MAX_BEVEL_SEGMENTS, value)))
            .build());
        entries.add(entryBuilder.startIntField(
                Component.literal("Corner Segments"),
                layer.BEVEL.BEVEL_CORNER_SEGMENTS)
            .setDefaultValue(8)
            .setMin(MIN_BEVEL_SEGMENTS)
            .setMax(MAX_BEVEL_SEGMENTS)
            .setSaveConsumer(value -> layer.BEVEL.BEVEL_CORNER_SEGMENTS = Math.max(MIN_BEVEL_SEGMENTS, Math.min(MAX_BEVEL_SEGMENTS, value)))
            .build());
        return new ParameterGroup("Bevel", entries);
    }
    
    private static ParameterGroup buildPerformanceGroup(ConfigEntryBuilder entryBuilder, CloudsConfiguration.LayerConfiguration layer) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startIntField(
                Component.literal("Mesh Rebuild Budget (ms)"),
                layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS)
            .setDefaultValue(2)
            .setMin(MIN_MESH_REBUILD_BUDGET_MS)
            .setMax(MAX_MESH_REBUILD_BUDGET_MS)
            .setSaveConsumer(value -> layer.PERFORMANCE.MESH_REBUILD_BUDGET_MS = Math.max(MIN_MESH_REBUILD_BUDGET_MS, Math.min(MAX_MESH_REBUILD_BUDGET_MS, value)))
            .build());
        return new ParameterGroup("Performance", entries);
    }
    
    private static void saveConfig() {
        CloudsConfiguration.save();
        if (RendererHolder.get() != null) {
            RendererHolder.get().markForRebuild();
        }
    }
    
    /**
     * Immutable wrapper for a parameter group with name and entries.
     */
    private static class ParameterGroup {
        final String name;
        final List<AbstractConfigListEntry<?>> entries;
        
        ParameterGroup(String name, List<AbstractConfigListEntry<?>> entries) {
            this.name = name;
            this.entries = Collections.unmodifiableList(entries);
        }
    }
}