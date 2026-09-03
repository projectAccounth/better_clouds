package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.io.File;
import java.util.List;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.presets.LightingPreset;

public final class LightingPresetController extends BasePresetController<LightingPreset> {
    private static final LightingPresetController INSTANCE = new LightingPresetController();

    private LightingPresetController() {
        super(new File("config/cloud_presets/lighting"), new File("config/cloud_presets/backups"), LightingPreset.class);
    }

    public static LightingPresetController getInstance() {
        return INSTANCE;
    }

    public void initialize() { ensureInitialized(); }

    public boolean savePreset(String id, String displayName, String description, CloudsConfiguration config) {
        return save(LightingPreset.fromConfiguration(id, displayName, description, config));
    }

    public boolean applyPreset(String id, CloudsConfiguration config) {
        LightingPreset preset = get(id);
        if (preset == null) return false;
        backupCurrentConfig("lighting_preset_load");
        preset.applyTo(config);
        return true;
    }

    public LightingPreset getPreset(String id) { return get(id); }
    public List<LightingPreset> listPresets() { return list(); }
    public boolean deletePreset(String id) { return delete(id); }
    public String exportPresetAsBase64(String id) { return exportAsBase64(id); }
    public boolean importPresetFromPayload(String id, String name, String description, String payload) {
        return importFromPayload(id, name, description, payload);
    }

    @Override
    protected void setMetadata(LightingPreset preset, PresetMetadata metadata) {
        preset.id = metadata.id;
        preset.displayName = metadata.displayName;
        preset.description = metadata.description;
        preset.lastModified = metadata.lastModified;
    }

    @Override
    protected boolean isValid(LightingPreset preset) { return preset.isValid(); }

    @Override
    protected LightingPreset createEmptyPreset() { return new LightingPreset(); }
}
