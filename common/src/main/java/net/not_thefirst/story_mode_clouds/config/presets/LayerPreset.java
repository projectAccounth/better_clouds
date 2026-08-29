package net.not_thefirst.story_mode_clouds.config.presets;

import com.google.gson.Gson;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

/**
 * Represents a saved cloud layer preset.
 */
public class LayerPreset implements Preset {
    public String id;
    public String displayName;
    public String description;
    public long lastModified;
    public CloudsConfiguration.LayerConfiguration layer;
    public CloudsConfiguration.Dimension dimension;

    public LayerPreset(String id, String displayName, String description, CloudsConfiguration.LayerConfiguration layer,
                       CloudsConfiguration.Dimension dimension) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.lastModified = System.currentTimeMillis();
        this.layer = copyLayer(layer, dimension);
        this.dimension = dimension;
    }

    public LayerPreset() {
        this("", "", "", new CloudsConfiguration.LayerConfiguration(), CloudsConfiguration.Dimension.OVERWORLD);
    }

    public static LayerPreset fromLayer(String id, String displayName, String description,
                                        CloudsConfiguration.LayerConfiguration layer,
                                        CloudsConfiguration.Dimension dimension) {
        return new LayerPreset(id, displayName, description, layer, dimension);
    }

    private static CloudsConfiguration.LayerConfiguration copyLayer(CloudsConfiguration.LayerConfiguration source,
                                                                     CloudsConfiguration.Dimension dimension) {
        int index = 0;
        CloudsConfiguration.LayerHolder holder = CloudsConfiguration.getInstance().getLayerHolder(dimension);
        if (holder != null) {
            index = holder.layers.size();
        }
        CloudsConfiguration.LayerConfiguration copy = new CloudsConfiguration.LayerConfiguration(index);
        copy.copy(source);
        return copy;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void applyTo(CloudsConfiguration config) {
        CloudsConfiguration.LayerHolder holder = config.getLayerHolder(dimension);
        if (holder == null) {
            LoggerProvider.get().error("Unable to apply layer preset to unknown dimension: {}", dimension.getId());
            return;
        }
        CloudsConfiguration.LayerConfiguration newLayer = new CloudsConfiguration.LayerConfiguration(holder.layers.size());
        newLayer.copy(layer);
        holder.addLayer(newLayer);
    }

    @Override
    public boolean validate(String payload) {
        String json = Preset.decodePayload(payload);
        if (json == null) return false;

        try {
            Gson gson = CloudsConfiguration.getInstance().getGson();
            CloudsConfiguration.LayerConfiguration layerConfig = gson.fromJson(json, CloudsConfiguration.LayerConfiguration.class);
            return LayerPresets.validateLayerConfiguration(layerConfig);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Preset copy(String newId, String newDisplayName) {
        return new LayerPreset(newId, newDisplayName, this.description, this.layer, this.dimension);
    }
}
