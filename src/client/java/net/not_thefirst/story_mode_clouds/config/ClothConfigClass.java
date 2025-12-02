package net.not_thefirst.story_mode_clouds.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LayerHolder;

@Config(name = "cloud_tweaks")
public class ClothConfigClass implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public CloudsConfiguration.LayerHolder layers = new CloudsConfiguration.LayerHolder();

    @ConfigEntry.Gui.CollapsibleObject
    public GlobalConfig global = new GlobalConfig();

    public void applyFromMain(CloudsConfiguration config) {
        this.global.applyFromMain(config);
        if (this.layers == null) this.layers = new LayerHolder();

        int needed = config.getHolder() != null ? config.getHolder().layers.size() : 0;
        if (this.layers.layers.size() > needed) {
            while (this.layers.layers.size() > needed) {
                this.layers.layers.remove(this.layers.layers.size() - 1);
            }
        } else {
            int start = this.layers.layers.size();
            for (int i = start; i < needed; i++) {
                this.layers.layers.add(new CloudsConfiguration.LayerConfiguration(i));
            }
        }

        for (int i = 0; i < needed; i++) {
            CloudsConfiguration.LayerConfiguration clothLayer = this.layers.layers.get(i);
            CloudsConfiguration.LayerConfiguration mainLayer = config.getHolder().layers.get(i);
            clothLayer.copy(mainLayer);
        }
    }

    public static class GlobalConfig {
        public boolean CLOUDS_RENDERED = true;

        public void applyFromMain(CloudsConfiguration config) {
            CLOUDS_RENDERED = config.CLOUDS_RENDERED;
        }
    }

    public ClothConfigClass() {
        super();
    }
}