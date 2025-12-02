package net.not_thefirst.story_mode_clouds.config;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.api.SimpleConfigScreen;

public class CloudsConfigScreen extends SimpleConfigScreen {
    public CloudsConfigScreen(Screen parent) {
        super(parent, "cloud_tweaks.cloud_config");
    }

    @Override
    protected void init() {
        super.init();
        CloudsConfiguration cfg = CloudsConfiguration.INSTANCE;
        
        addCategory("option.cloud_tweaks.category.general", HorizontalAlignment.CENTER);
        addToggle("option.cloud_tweaks.clouds_rendered", cfg.CLOUDS_RENDERED, (v) -> cfg.CLOUDS_RENDERED = v);

        addCategory("option.cloud_tweaks.category.appearance", HorizontalAlignment.CENTER);
        addCategory("option.cloud_tweaks.category.shape", HorizontalAlignment.CENTER);
        addCategory("option.cloud_tweaks.category.layers", HorizontalAlignment.CENTER);

        onCloseSave(CloudsConfiguration::save);
    }
}