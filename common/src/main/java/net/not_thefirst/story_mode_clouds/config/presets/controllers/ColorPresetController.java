package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.io.File;
import java.util.List;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.CloudColorPreset;

public final class ColorPresetController extends BasePresetController<CloudColorPreset> {
    private static final ColorPresetController INSTANCE = new ColorPresetController();

    private ColorPresetController() {
        super(new File("config/cloud_presets/colors"), new File("config/cloud_presets/backups"), CloudColorPreset.class);
    }

    public static ColorPresetController getInstance() {
        return INSTANCE;
    }

    public void initialize() { ensureInitialized(); }

    public boolean savePreset(String id, String displayName, String description, CloudsConfiguration config) {
        return save(CloudColorPreset.fromConfiguration(id, displayName, description, config));
    }

    public boolean applyPreset(String id, CloudsConfiguration config) {
        CloudColorPreset preset = get(id);
        if (preset == null) return false;
        backupCurrentConfig("color_preset_load");
        preset.applyTo(config);
        return true;
    }

    public CloudColorPreset getPreset(String id) { return get(id); }
    public List<CloudColorPreset> listPresets() { return list(); }
    public boolean deletePreset(String id) { return delete(id); }
    public String exportPresetAsBase64(String id) { return exportAsBase64(id); }
    public boolean importPresetFromPayload(String id, String name, String description, String payload) {
        return importFromPayload(id, name, description, payload);
    }

    @Override
    protected void setMetadata(CloudColorPreset preset, PresetMetadata metadata) {
        preset.id = metadata.id;
        preset.displayName = metadata.displayName;
        preset.description = metadata.description;
        preset.lastModified = metadata.lastModified;
    }

    @Override
    protected boolean isValid(CloudColorPreset preset) { return preset.isValid(); }

    @Override
    protected CloudColorPreset createEmptyPreset() { return new CloudColorPreset(); }
}
