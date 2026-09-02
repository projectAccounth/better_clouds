package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.screens.DataSettings;
import java.util.HashMap;
import java.util.Map;

public class LayerEditingScreenManager extends ScreenManager {
    private static final Map<String, LayerEditingScreenManager> instances = new HashMap<>();
    
    private final CloudsConfiguration.Dimension dimension;
    private final int layerIndex;
    private final CloudsConfiguration config;

    private LayerEditingScreenManager(CloudsConfiguration.Dimension dimension, int layerIndex) {
        super();
        this.dimension = dimension;
        this.layerIndex = layerIndex;
        this.config = CloudsConfiguration.getInstance();
    }

    private static String getKey(CloudsConfiguration.Dimension dimension, int layerIndex) {
        return dimension.getId() + "_" + layerIndex;
    }

    public static LayerEditingScreenManager getInstance(CloudsConfiguration.Dimension dimension, int layerIndex) {
        return instances.computeIfAbsent(getKey(dimension, layerIndex), k -> new LayerEditingScreenManager(dimension, layerIndex));
    }

    public static void clearInstance(CloudsConfiguration.Dimension dimension, int layerIndex) {
        instances.remove(getKey(dimension, layerIndex));
    }

    public static void clearAll() {
        instances.clear();
    }

    @Override
    public Screen buildScreen() {
        CloudsConfiguration.LayerConfiguration layer = config.getLayerInDimension(dimension, layerIndex);
        final Screen[] self = new Screen[1];
        
        self[0] = BackendHolder.getBackend().createScreen("cloudtweaks.layer.settings_title", CloudsConfiguration::save)
            .category(DataSettings.buildLayerBasicSettings(layer))
            .category(DataSettings.buildLayerAppearanceSettings(layer))
            .category(DataSettings.buildOutlineCategory(layer))
            .category(DataSettings.buildTextureCategory(layer))
            .category(DataSettings.buildTypeSpecificCategory(layer, self))
            .category(DataSettings.buildLayerFogSettings(layer))
            .category(DataSettings.buildLayerFadeSettings(layer))
            .category(DataSettings.buildLayerSaveAsPresetCategory(layer, dimension))
            .build(parentScreen);
        
        currentScreen = self[0];
        return currentScreen;
    }
}
