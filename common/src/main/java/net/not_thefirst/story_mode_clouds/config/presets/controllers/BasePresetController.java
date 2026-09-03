package net.not_thefirst.story_mode_clouds.config.presets.controllers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.resources.ConfigSerializer;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.config.presets.Preset;

/** Common metadata-indexed persistence for one preset category. */
public abstract class BasePresetController<P extends Preset> {
    private static final DateTimeFormatter BACKUP_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    protected final Gson gson = CloudsConfiguration.getInstance().getGson();
    protected final File presetsDirectory;
    protected final File backupsDirectory;
    protected final File presetsIndex;
    private final Class<P> presetType;
    private final Map<String, PresetMetadata> metadata = new HashMap<>();
    private boolean initialized;

    protected BasePresetController(File presetsDirectory, File backupsDirectory, Class<P> presetType) {
        this.presetsDirectory = presetsDirectory;
        this.backupsDirectory = backupsDirectory;
        this.presetsIndex = new File(presetsDirectory, "presets.json");
        this.presetType = presetType;
    }

    protected abstract void setMetadata(P preset, PresetMetadata metadata);
    protected abstract boolean isValid(P preset);
    protected abstract P createEmptyPreset();

    protected PresetMetadata createMetadata(P preset) {
        return new PresetMetadata(
            preset.getId(), preset.getDisplayName(), preset.getDescription(), System.currentTimeMillis());
    }

    protected PresetMetadata readMetadata(JsonObject object) {
        return gson.fromJson(object, PresetMetadata.class);
    }

    protected final Map<String, PresetMetadata> metadataEntries() {
        return new HashMap<>(metadata);
    }

    protected P readPreset(FileReader reader, PresetMetadata entry) {
        return gson.fromJson(reader, presetType);
    }

    protected final synchronized void ensureInitialized() {
        if (initialized) return;
        presetsDirectory.mkdirs();
        backupsDirectory.mkdirs();
        loadIndex();
        initialized = true;
    }

    public final synchronized boolean save(P preset) {
        ensureInitialized();
        if (preset == null || preset.getId() == null || preset.getId().isBlank()) return false;

        PresetMetadata entry = createMetadata(preset);
        setMetadata(preset, entry);
        File presetFile = fileFor(entry);
        try {
            safeWriteFile(presetFile, () -> gson.toJson(preset));
            metadata.put(entry.id, entry);
            saveIndex();
            return true;
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to save preset {}: {}", entry.id, e.getMessage());
            return false;
        }
    }

    public final synchronized P get(String presetId) {
        ensureInitialized();
        PresetMetadata entry = metadata.get(presetId);
        if (entry == null) return null;
        File presetFile = fileFor(entry);
        if (!presetFile.exists()) return null;
        try (FileReader reader = new FileReader(presetFile)) {
            P preset = readPreset(reader, entry);
            if (preset != null) setMetadata(preset, entry);
            return preset;
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to load preset {}: {}", presetId, e.getMessage());
            return null;
        }
    }

    public final synchronized List<P> list() {
        ensureInitialized();
        List<P> result = new ArrayList<>();
        for (String id : metadata.keySet()) {
            P preset = get(id);
            if (preset != null) result.add(preset);
        }
        return result;
    }

    public final synchronized boolean delete(String presetId) {
        ensureInitialized();
        PresetMetadata entry = metadata.remove(presetId);
        if (entry == null) return false;
        try {
            backupFile(fileFor(entry), "preset_delete");
            Files.deleteIfExists(fileFor(entry).toPath());
            saveIndex();
            return true;
        } catch (IOException e) {
            metadata.put(presetId, entry);
            LoggerProvider.get().error("Failed to delete preset {}: {}", presetId, e.getMessage());
            return false;
        }
    }

    public final synchronized String exportAsBase64(String presetId) {
        P preset = get(presetId);
        if (preset == null) return null;
        return java.util.Base64.getEncoder().encodeToString(
            gson.toJson(preset).getBytes(StandardCharsets.UTF_8));
    }

    public final synchronized boolean importFromPayload(String presetId, String displayName,
                                                         String description, String payload) {
        ensureInitialized();
        try {
            String json = decodePayload(payload);
            P preset = json == null ? null : gson.fromJson(json, presetType);
            if (preset == null || !isValid(preset)) return false;
            setMetadata(preset, new PresetMetadata(presetId, displayName, description,
                                                   System.currentTimeMillis()));
            return save(preset);
        } catch (Exception e) {
            LoggerProvider.get().error("Failed to import preset {}: {}", presetId, e.getMessage());
            return false;
        }
    }

    public final synchronized boolean validatePayload(String payload) {
        if (payload == null || payload.isBlank()) return false;
        return createEmptyPreset().validate(payload);
    }

    public final synchronized boolean updateMetadata(String presetId, String displayName, String description) {
        P preset = get(presetId);
        if (preset == null) return false;
        setMetadata(preset, new PresetMetadata(presetId, displayName, description,
                                                System.currentTimeMillis()));
        return save(preset);
    }

    protected final void backupCurrentConfig(String prefix) {
        File backup = new File(backupsDirectory, prefix + "_" + timestamp() + ".json");
        try (FileWriter writer = new FileWriter(backup)) {
            writer.write(ConfigSerializer.toJson(CloudsConfiguration.getInstance()));
        } catch (IOException e) {
            LoggerProvider.get().error("Failed to backup current configuration: {}", e.getMessage());
        }
    }

    private void loadIndex() {
        if (!presetsIndex.exists()) return;
        boolean migrated = false;
        try (FileReader reader = new FileReader(presetsIndex)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("presets")) return;
            for (var element : root.getAsJsonArray("presets")) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("configFile")) {
                    PresetMetadata entry = readMetadata(object);
                    if (entry != null && entry.id != null) metadata.put(entry.id, entry);
                    continue;
                }

                // Legacy indexes embedded the complete preset in this array.
                P legacyPreset = gson.fromJson(object, presetType);
                if (legacyPreset == null || legacyPreset.getId() == null) continue;
                PresetMetadata entry = createMetadata(legacyPreset);
                metadata.put(entry.id, entry);
                File file = fileFor(entry);
                if (!file.exists()) safeWriteFile(file, () -> gson.toJson(legacyPreset));
                migrated = true;
            }
            if (migrated) saveIndex();
        } catch (Exception e) {
            LoggerProvider.get().warn("Failed to load preset index: {}", e.getMessage());
        }
    }

    private void saveIndex() throws IOException {
        safeWriteFile(presetsIndex, () -> {
            JsonObject root = new JsonObject();
            root.add("presets", gson.toJsonTree(metadata.values().toArray(new PresetMetadata[0])));
            return gson.toJson(root);
        });
    }

    private File fileFor(PresetMetadata entry) {
        return new File(presetsDirectory, entry.configFile);
    }

    private static String decodePayload(String payload) {
        if (payload == null || payload.isBlank()) return null;
        String trimmed = payload.trim();
        if (trimmed.startsWith("{")) return trimmed;
        try {
            return new String(java.util.Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void safeWriteFile(File target, FileContentProvider provider) throws IOException {
        File tempFile = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(provider.getContent());
        }
        Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void backupFile(File source, String prefix) throws IOException {
        if (source.exists()) {
            Files.copy(source.toPath(), new File(backupsDirectory, prefix + "_" + timestamp() + ".json").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String timestamp() {
        return BACKUP_DATE_FORMAT.format(LocalDateTime.now());
    }

    public static class PresetMetadata {
        public String id;
        public String displayName;
        public String description;
        public long lastModified;
        public String configFile;

        public PresetMetadata(String id, String displayName, String description, long lastModified) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.lastModified = lastModified;
            this.configFile = id + ".json";
        }

        public PresetMetadata() {}
    }

    @FunctionalInterface
    private interface FileContentProvider {
        String getContent() throws IOException;
    }
}