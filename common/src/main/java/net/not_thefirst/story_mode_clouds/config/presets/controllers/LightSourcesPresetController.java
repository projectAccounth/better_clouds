package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.io.File;
import java.util.List;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LightSourcesPreset;

public final class LightSourcesPresetController extends BasePresetController<LightSourcesPreset> {
    private static final LightSourcesPresetController INSTANCE = new LightSourcesPresetController();

    private LightSourcesPresetController() {
        super(new File("config/cloud_presets/light_sources"), new File("config/cloud_presets/backups"), LightSourcesPreset.class);
    }

    public static LightSourcesPresetController getInstance() {
        return INSTANCE;
    }

    public void initialize() { ensureInitialized(); }

    public boolean savePreset(String id, String displayName, String description, CloudsConfiguration config) {
        return save(LightSourcesPreset.fromConfiguration(id, displayName, description, config));
    }

    public boolean applyPreset(String id, CloudsConfiguration config) {
        LightSourcesPreset preset = get(id);
        if (preset == null) return false;
        backupCurrentConfig("light_sources_preset_load");
        preset.applyTo(config);
        return true;
    }

    public LightSourcesPreset getPreset(String id) { return get(id); }
    public List<LightSourcesPreset> listPresets() { return list(); }
    public boolean deletePreset(String id) { return delete(id); }
    public String exportPresetAsBase64(String id) { return exportAsBase64(id); }
    public boolean importPresetFromPayload(String id, String name, String description, String payload) {
        return importFromPayload(id, name, description, payload);
    }

    @Override
    protected void setMetadata(LightSourcesPreset preset, PresetMetadata metadata) {
        preset.id = metadata.id;
        preset.displayName = metadata.displayName;
        preset.description = metadata.description;
        preset.lastModified = metadata.lastModified;
    }

    @Override
    protected boolean isValid(LightSourcesPreset preset) { return preset.isValid(); }

    @Override
    protected LightSourcesPreset createEmptyPreset() { return new LightSourcesPreset(); }
}
