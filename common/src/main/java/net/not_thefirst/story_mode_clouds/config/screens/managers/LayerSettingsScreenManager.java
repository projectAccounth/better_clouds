package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;
import java.util.HashMap;
import java.util.Map;

public class LayerSettingsScreenManager extends ScreenManager {
    private static final Map<CloudsConfiguration.Dimension, LayerSettingsScreenManager> instances = new HashMap<>();
    
    private final CloudsConfiguration.Dimension dimension;
    private final CloudsConfiguration config;

    private LayerSettingsScreenManager(CloudsConfiguration.Dimension dimension) {
        super();
        this.dimension = dimension;
        this.config = CloudsConfiguration.getInstance();
    }

    public static LayerSettingsScreenManager getInstance(CloudsConfiguration.Dimension dimension) {
        return instances.computeIfAbsent(dimension, d -> new LayerSettingsScreenManager(d));
    }

    public static void clearInstance(CloudsConfiguration.Dimension dimension) {
        instances.remove(dimension);
    }

    public static void clearAll() {
        instances.clear();
    }

    @Override
    public Screen buildScreen() {
        var backend = BackendHolder.getBackend();
        var builder = backend.createScreen("cloudtweaks.title.layer_settings", CloudsConfiguration::save);
        
        var category = buildLayersCategory();
        builder.category(category);
        
        currentScreen = builder.build(parentScreen);
        return currentScreen;
    }

    private ConfigBackend.ConfigCategoryBuilder buildLayersCategory() {
        var backend = BackendHolder.getBackend();
        var category = backend.createCategory("cloudtweaks.category.layers", "cloudtweaks.desc.layers");
        
        CloudsConfiguration.LayerHolder layerHolder = config.getLayerHolder(dimension);

        if (!CloudsConfiguration.Dimension.isBuiltin(dimension)) {
            category.action(backend.createAction(
                "cloudtweaks.title.delete_dimension",
                "cloudtweaks.desc.delete_dimension",
                () -> {
                    ClientHelper.setScreen(buildDeleteConfirmationScreen(
                        dimension.getId(),
                        currentScreen,
                        () -> {
                            CloudsConfiguration.getInstance().removeLayerHolder(dimension);
                            ClientHelper.setScreen(parentScreen);
                        }
                    ));
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
                    CloudsConfiguration.LayerConfiguration newLayer = new CloudsConfiguration.LayerConfiguration(layerHolder.layers.size());
                    layerHolder.addLayer(newLayer);
                    CloudsConfiguration.save();
                    ClientHelper.sendLocalSystemMessage(
                        ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.added_layer_prefix").getString() + layerHolder.layers.size() + ComponentWrapper.translatable("cloudtweaks.message.added_layer_to").getString() + dimension.getId())
                    );
                    ClientHelper.setScreen(refreshScreen());
                })
            );

        for (int i = 0; i < layerHolder.layers.size(); i++) {
            CloudsConfiguration.LayerConfiguration layer = layerHolder.layers.get(i);
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
                        LayerEditingScreenManager editManager = LayerEditingScreenManager.getInstance(dimension, layerIndex);
                        editManager.setParentScreen(currentScreen);
                        ClientHelper.setScreen(editManager.buildScreen());
                    })
                )
                .action(backend.createAction(
                    "cloudtweaks.option.remove_layer",
                    "cloudtweaks.desc.remove_layer",
                    () -> {
                        if (layerHolder.layers.size() > 1) {
                            ClientHelper.setScreen(buildDeleteConfirmationScreen(
                                layer.NAME,
                                currentScreen,
                                () -> {
                                    layerHolder.removeLayer(layerIndex);
                                    CloudsConfiguration.save();
                                    ClientHelper.sendLocalSystemMessage(
                                        ComponentWrapper.literal(ComponentWrapper.translatable("cloudtweaks.message.removed_layer_prefix").getString() + dimension.getId())
                                    );
                                    ClientHelper.setScreen(refreshScreen());
                                }
                            ));
                        }
                    })
                );
            
            category.group(group);
        }

        category.action(backend.createAction(
            "cloudtweaks.presets.layer_presets_button",
            "cloudtweaks.presets.layer_presets_button.tooltip",
            () -> {
                LayerPresetsScreenManager presetsManager = LayerPresetsScreenManager.getInstance(dimension);
                presetsManager.setParentScreen(currentScreen);
                ClientHelper.setScreen(presetsManager.buildScreen());
            })
        );
        
        return category;
    }
}
