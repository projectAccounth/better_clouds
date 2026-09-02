package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.not_thefirst.lib.utils.objects.DynamicEnum;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.Starter;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeDataCache;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.ConfigEntry;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.json.ConfigInstanceAdapterFactory;
import net.not_thefirst.story_mode_clouds.utils.json.DimensionTypeAdapter;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.math.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.math.MathUtils;
import net.not_thefirst.story_mode_clouds.utils.math.NumberSequence;
import net.not_thefirst.story_mode_clouds.utils.minecraft.DimensionProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

public class CloudsConfiguration {

    // ------------------------------------------
    // defs and consts

    private static final Gson GSON = 
        new GsonBuilder()
            .setPrettyPrinting()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Dimension.class, new DimensionTypeAdapter())
            .registerTypeAdapterFactory(new ConfigInstanceAdapterFactory())
            .create();

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILENAME = "cloud_configs.json";
    private static final File CONFIG_FILE = new File(CONFIG_DIR + "/" + CONFIG_FILENAME);
    
    // Backup throttling
    private static long lastBackupTime = 0;
    private static final long BACKUP_THROTTLE_MS = 8000; // main config backup cd
    
    private static CloudsConfiguration instance = new CloudsConfiguration();
    
    // Track when config was last modified for caching purposes
    private long lastModificationTime = System.currentTimeMillis();

    // chatgpt generated color
    public static final List<SkyColorKeypoint> DEFAULT_COLORS = Arrays.asList(
        // Sunrise (6:00 AM)
        new SkyColorKeypoint(0, 0xFFD8A8),        // soft peach

        // Morning (9:00 AM)
        new SkyColorKeypoint(3000, 0xFFFFFF),     // bright white

        // Noon (12:00 PM)
        new SkyColorKeypoint(6000, 0xFFFFFF),     // neutral white

        // Afternoon (3:00 PM)
        new SkyColorKeypoint(9000, 0xF4F6F8),     // slightly cool white

        // Sunset (6:00 PM)
        new SkyColorKeypoint(12000, 0xFF9E5E),    // warm orange glow

        // Late sunset (7:00 PM)
        new SkyColorKeypoint(13000, 0xFFB37A),    // orange-pink highlight

        // Dusk (8:00 PM)
        new SkyColorKeypoint(14000, 0x7A86A8),    // muted bluish gray

        // Night (12:00 AM)
        new SkyColorKeypoint(18000, 0x2A2F45),    // dark desaturated blue

        // Pre-dawn (4:00 AM)
        new SkyColorKeypoint(22000, 0x4A567A),    // faint blue-gray

        // Loop back to sunrise
        new SkyColorKeypoint(23999, 0xFFD8A8)
    );

    public List<SkyColorKeypoint> CLOUD_COLOR = new ArrayList<>(Arrays.asList(
        new SkyColorKeypoint(0, 0xFFD8A8),
        new SkyColorKeypoint(3000, 0xFFFFFF),
        new SkyColorKeypoint(6000, 0xFFFFFF),
        new SkyColorKeypoint(9000, 0xF4F6F8),
        new SkyColorKeypoint(12000, 0xFF9E5E),
        new SkyColorKeypoint(13000, 0xFFB37A),
        new SkyColorKeypoint(14000, 0x7A86A8),
        new SkyColorKeypoint(18000, 0x2A2F45),
        new SkyColorKeypoint(22000, 0x4A567A),
        new SkyColorKeypoint(23999, 0xFFD8A8)
    ));

    // ------------------------------------------
    // global config section
    
    public boolean CLOUDS_RENDERED = true;

    public CloudColorProviderMode COLOR_MODE = CloudColorProviderMode.VANILLA;
    public LightingParameters LIGHTING        = new LightingParameters();
    public WeatherColorConfig WEATHER_COLOR   = new WeatherColorConfig();
    public int                CLOUD_GRID_SIZE = 64; // legacy serialized block half-range, kept for backward compatibility
    public Integer            CLOUD_DISTANCE_CHUNKS = null; // distance in chunks; grid half-range = (chunks * 16) / 12

    private final Map<Dimension, LayerHolder> DIMENSION_LAYERS = new HashMap<>();
    private final Map<Dimension, Boolean> DIMENSION_CLOUD_RENDERED = new HashMap<>();
    private static final transient Set<Dimension> REMOVED_DIMENSIONS = new HashSet<>();
    
    // ------------------------------------------
    // constructor

    public CloudsConfiguration() {
        initializeDimensions();
    }

    // ------------------------------------------
    // misc initialization funcs

    public void initializeDimensions() {
        Set<IdentifierWrapper> dimensions = DimensionProvider.getAllDimensions();

        dimensions.forEach((dim) -> {
            Dimension dimension = Dimension.getOrRegister(dim);

            if (!DIMENSION_LAYERS.containsKey(dimension)) {
                LayerHolder holder = new LayerHolder();
                holder.addLayer(new LayerConfiguration(0));
                DIMENSION_LAYERS.put(dimension, holder);
            }

            if (!DIMENSION_CLOUD_RENDERED.containsKey(dimension)) {
                boolean rendered = dimension.getId().equalsIgnoreCase("minecraft:overworld");
                DIMENSION_CLOUD_RENDERED.put(dimension, rendered);
            }
        });
    }
    // ------------------------------------------
    // getter/setter funcs

    public static CloudsConfiguration getInstance() { return instance; }
    
    @Nullable
    public LayerHolder getLayerHolder(Dimension dimension) {
        if (dimension == null) {
            LoggerProvider.get().warn("Dimension is null when trying to get LayerHolder. Returning null.");
            return null;
        }
        if (REMOVED_DIMENSIONS.contains(dimension)) {
            LoggerProvider.get().warn("LayerHolder for dimension {} has been removed. Refresh to re-generate.", dimension.getId());
            return null;
        }
        if (!DIMENSION_LAYERS.containsKey(dimension)) {
            LoggerProvider.get().warn("LayerHolder for dimension {} not found, hash code: {}. Recreating.", dimension.getId(), dimension.hashCode());
            LayerHolder holder = new LayerHolder();
            holder.addLayer(new LayerConfiguration(0));
            DIMENSION_LAYERS.put(dimension, holder);
            return holder;
        }
        return DIMENSION_LAYERS.get(dimension);
    }
    
    public void setLayerHolder(Dimension dimension, LayerHolder holder) {
        DIMENSION_LAYERS.put(dimension, holder);
    }

    public LayerConfiguration getLayer(int idx) {
        return getLayerInDimension(DimensionProvider.getCurrentDimension(), idx);
    }
    
    public void removeLayerHolder(Dimension dimension) {
        DIMENSION_LAYERS.remove(dimension);
        REMOVED_DIMENSIONS.add(dimension);
    }

    public boolean dimensionRemoved(Dimension dimension) {
        return REMOVED_DIMENSIONS.contains(dimension);
    }

    public LayerConfiguration getLayerInDimension(Dimension dimension, int idx) {
        LayerHolder holder = getLayerHolder(dimension);
        if (holder == null || idx < 0 || idx >= holder.layers.size()) {
            throw new IndexOutOfBoundsException("Layer index out of bounds: " + idx);
        }
        return holder.layers.get(idx);
    }
 
    public int getLayerCount() {
        return getLayerCount(DimensionProvider.getCurrentDimension());
    }
    
    public int getLayerCount(Dimension dimension) {
        LayerHolder holder = getLayerHolder(dimension);
        return holder == null ? 0 : holder.layers.size();
    }

    public long getLastModifiedTime() {
        return lastModificationTime;
    }

    /**
     * Mark this configuration as modified. Called when any setting changes.
     */
    public void markModified() {
        lastModificationTime = System.currentTimeMillis();
    }

    
    public boolean getCloudRendered(Dimension dimension) {
        return DIMENSION_CLOUD_RENDERED.getOrDefault(dimension, false);
    }
    
    public void setCloudRendered(Dimension dimension, boolean value) {
        DIMENSION_CLOUD_RENDERED.put(dimension, value);
    }

    public int getCloudDistanceChunks() {
        if (CLOUD_DISTANCE_CHUNKS != null && CLOUD_DISTANCE_CHUNKS > 0) {
            return CLOUD_DISTANCE_CHUNKS;
        }
        int derived = Math.round(CLOUD_GRID_SIZE * 12f / 16f);
        return derived >= ConfigConstants.MIN_CLOUD_DISTANCE_CHUNKS ? derived : ConfigConstants.DEFAULT_CLOUD_DISTANCE_CHUNKS;
    }

    public void setCloudDistanceChunks(int value) {
        CLOUD_DISTANCE_CHUNKS = value;
        CLOUD_GRID_SIZE = Math.round(value * 16f / 12f);
    }

    public int getCloudGridRange() {
        return Math.round(getCloudDistanceChunks() * 16f / 12f);
    }

    public WeatherColorConfig getWeather() { return WEATHER_COLOR; }

    // ------------------------------------------
    // file management

    /**
     * Force a complete configuration reload from disk
     */
    public static void refreshConfig() {
        LoggerProvider.get().info("Refreshing configuration from disk");
        load();
        REMOVED_DIMENSIONS.clear();
        
        Starter.reloadTypes();

        initializeCustomParameters();
        
        if (RendererHolder.get() != null) {
            RendererHolder.get().markForRebuild();
        }
        
        instance.lastModificationTime = System.currentTimeMillis();
        
        LoggerProvider.get().info("Successfully refreshed configuration");
    }

    private static void initializeCustomParameters() {
        for (LayerHolder holder : instance.DIMENSION_LAYERS.values()) {
            for (LayerConfiguration layer : holder.layers) {
                initializeLayerCustomParameters(layer);
            }
        }
    }

    private static void initializeLayerCustomParameters(LayerConfiguration layer) {
        Map<String, MeshTypeData> schemaMap = MeshTypeDataCache.getData().stream()
            .collect(Collectors.toMap(MeshTypeData::name, Function.identity()));

        layer.CUSTOM_PARAMETERS.keySet().removeIf(name -> !schemaMap.containsKey(name));

        for (var schemaEntry : schemaMap.entrySet()) {
            String typeName = schemaEntry.getKey();
            MeshTypeData schema = schemaEntry.getValue();

            if (schema.config().isEmpty()) {
                layer.CUSTOM_PARAMETERS.remove(typeName);
                continue;
            }

            LayerConfiguration.CustomTypeParameters params = layer.CUSTOM_PARAMETERS.computeIfAbsent(
                typeName,
                k -> new LayerConfiguration.CustomTypeParameters()
            );
            
            params.initializeFromSchema(schema);
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            instance = GSON.fromJson(reader, CloudsConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
            instance = null;
        }

        if (instance == null) {
            instance = new CloudsConfiguration();
        }

        if (instance.CLOUD_COLOR == null || instance.CLOUD_COLOR.isEmpty()) {
            instance.CLOUD_COLOR = new ArrayList<>(DEFAULT_COLORS);
        }

        instance.CLOUD_COLOR.sort(Comparator.comparingInt(kp -> kp.time));

        NumberSequence sequence = CloudColorProvider.getCurrentSequence();
        sequence.clearKeypoints();

        for (SkyColorKeypoint kp : instance.CLOUD_COLOR) {
            int c = kp.color;

            double r = ((c >> 16) & 0xFF) / 255.0;
            double g = ((c >> 8) & 0xFF) / 255.0;
            double b = (c & 0xFF) / 255.0;

            sequence.addKeypoint(
                kp.time,
                r,
                g,
                b
            );
        }

        // cyclic continuity (23999 == 0)
        SkyColorKeypoint first = instance.CLOUD_COLOR.get(0);
        SkyColorKeypoint last = instance.CLOUD_COLOR.get(instance.CLOUD_COLOR.size() - 1);

        if (last.time != 23999) {
            int c = first.color;

            sequence.addKeypoint(
                23999,
                ((c >> 16) & 0xFF) / 255.0,
                ((c >> 8) & 0xFF) / 255.0,
                (c & 0xFF) / 255.0
            );
        }
    }

    public static void save() {
        instance.markModified();
        
        try {
            saveOrThrow();
        } catch (IOException ex) {
            LoggerProvider.get().error("Error while saving cloud configuration.", ex);
        }

        if (RendererHolder.get() != null) {
            RendererHolder.get().markForRebuild();
        }
    }

    /**
     * Save configuration.
     */
    private static void saveOrThrow() throws IOException {
        File dir = new File(CONFIG_DIR, "cloud_tweaks");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File tempFile = new File(dir, CONFIG_FILENAME + ".tmp");

        try {
            try (FileWriter fw = new FileWriter(tempFile)) {
                GSON.toJson(instance, fw);
            }

            try (FileReader fr = new FileReader(tempFile)) {
                GSON.fromJson(fr, CloudsConfiguration.class);
            }

            boolean changed = true;

            if (CONFIG_FILE.exists()) {
                long mismatch = Files.mismatch(
                    CONFIG_FILE.toPath(),
                    tempFile.toPath()
                );

                changed = (mismatch != -1);
            }

            if (!changed) {
                return;
            }

            if (CONFIG_FILE.exists()) {
                createTimestampedBackup(CONFIG_FILE);
            }
            
            Files.move(
                tempFile.toPath(),
                CONFIG_FILE.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );

        } finally {
            try {
                Files.deleteIfExists(tempFile.toPath());
            } catch (IOException e) {
                LoggerProvider.get().error("Error while deleting temporary file.", e);
            }
        }
    }

    /**
     * Create a timestamped backup of the config file.
     */
    private static void createTimestampedBackup(File source) throws IOException {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackupTime < BACKUP_THROTTLE_MS) {
            LoggerProvider.get().debug("Main config backup skipped - throttled (< {}ms)", BACKUP_THROTTLE_MS);
            return;
        }
        
        File backupDir = new File(CONFIG_DIR, "cloud_tweaks/backups");
        backupDir.mkdirs();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = formatter.format(LocalDateTime.now());
        File backupFile = new File(backupDir, "cloud_configs_" + timestamp + ".json");
        Files.copy(source.toPath(), backupFile.toPath(), 
            StandardCopyOption.REPLACE_EXISTING);
        lastBackupTime = currentTime;
    }
    
    public static void copyLayerHolder(LayerHolder dest, LayerHolder src) {
        dest.clear();
        for (LayerConfiguration otherLayer : src.layers) {
            LayerConfiguration newLayer = new LayerConfiguration(otherLayer.getLayerIndex());
            newLayer.copy(otherLayer);
            dest.addLayer(newLayer);
        }
    }

    public Gson getGson() {
        return GSON;
    }

    // ------------------------------------------
    // typedefs

    public static class LayerConfiguration {
        public AppearanceParameters  APPEARANCE  = new AppearanceParameters();
        public FadeParameters        FADE        = new FadeParameters();
        public FogParameters         FOG         = new FogParameters();

        private int LAYER_IDX;

        public String  NAME                  = "Minecraft";
        public boolean FOG_ENABLED           = true;
        public boolean LAYER_RENDERED        = true;
        public int     LAYER_HEIGHT          = 128;

        public String MODE = "NORMAL";

        private Map<String, CustomTypeParameters> CUSTOM_PARAMETERS = new HashMap<>();

        public int getLayerIndex() { return LAYER_IDX; }

        public void copy(LayerConfiguration other) {
            this.NAME = other.NAME;
            this.FOG_ENABLED = other.FOG_ENABLED;
            this.LAYER_RENDERED = other.LAYER_RENDERED;
            this.LAYER_HEIGHT = other.LAYER_HEIGHT;
            this.MODE = other.MODE;
            this.APPEARANCE.copy(other.APPEARANCE);
            this.FADE.copy(other.FADE);
            this.FOG.copy(other.FOG);

            refreshCustomTypeConfig();
        }

        public LayerConfiguration(int idx) {
            LAYER_IDX = idx;
            for (var entry : MeshTypeDataCache.getData()) {
                if (entry.config().isEmpty()) continue;
                CUSTOM_PARAMETERS.put(entry.name(), new CustomTypeParameters());
            }
        }

        public LayerConfiguration() {
            this(0);
        }

        public CustomTypeParameters getTypeConfig(String name) {
            return CUSTOM_PARAMETERS.get(name);
        }

        public void deleteCustomType(String name) {
            CUSTOM_PARAMETERS.remove(name);
        }

        public CustomTypeParameters resetCustomTypeConfig(String name) {
            CustomTypeParameters conf = CUSTOM_PARAMETERS.computeIfAbsent(name, k -> new CustomTypeParameters());

            MeshTypeData meshTypeData = MeshTypeDataCache.get(name);
            if (meshTypeData != null) {
                conf.initializeFromSchema(meshTypeData);
            }

            return conf;
        }

        public boolean hasCustomTypeConfig(String name) {
            return CUSTOM_PARAMETERS.containsKey(name);
        }

        public void refreshCustomTypeConfig() {
            for (var entry : MeshTypeDataCache.getData()) {
                if (entry.config().isEmpty()) continue;
                if (!hasCustomTypeConfig(entry.name())) {
                    resetCustomTypeConfig(entry.name());
                }
                else {
                    CustomTypeParameters conf = getTypeConfig(entry.name());
                    conf.initializeFromSchema(entry);
                }
            }
        }

        public Map<String, CustomTypeParameters> getAllTypeConfig() {
            return CUSTOM_PARAMETERS;
        }

        public static class CustomTypeParameters {
            private final Map<String, ConfigInstance<?>> objects = new HashMap<>();
            private transient MeshTypeData schema;

            CustomTypeParameters() {
            }

            static ConfigInstance<?> createInstanceFromEntry(ConfigEntry entry) {
                switch (entry.type()) {
                    case STRING -> {
                        String v = (String) entry.defaultValue();
                        return new StringConfig(v);
                    }
                    case NUMBER -> {
                        var ne = (MeshTypeData.NumberConfigEntry) entry;
                        Float def = ne.defaultValue();
                        Float min = ne.minimum();
                        Float max = ne.maximum();
                        if (def == null) def = 0f;
                        float clamped = MathUtils.clamp(def, min, max);
                        return new FloatConfig(clamped);
                    }
                    case INTEGER -> {
                        var ie = (MeshTypeData.IntegerConfigEntry) entry;
                        Long def = ie.defaultValue();
                        Long min = ie.minimum();
                        Long max = ie.maximum();
                        if (def == null) def = 0L;
                        long clamped = MathUtils.clamp(def, min, max);
                        return new IntConfig((int) clamped);
                    }
                    case BOOLEAN -> {
                        Boolean b = (Boolean) entry.defaultValue();
                        if (b == null) b = Boolean.FALSE;
                        return new BooleanConfig(b);
                    }
                    default -> throw new IllegalArgumentException("Unsupported config type: " + entry.type());
                }
            }

            /**
             * Initializes/migrates this to match the new schema
             * 
             * <p>
             * Behavior
             * <ul>
             *   <li> If no entries exist, creates all entries from schema
             *   <li> If entries exist, compares against schema
             *   <li> Drops unknown fields not in schema, adds new fields from schema
             * </ul>
             * 
             * @param schema The MeshTypeData schema to synchronize against
             * @throws IllegalArgumentException if schema is null
             */
            public void initializeFromSchema(MeshTypeData schema) {
                if (schema == null) {
                    throw new IllegalArgumentException("Schema cannot be null");
                }
                this.schema = schema;

                Map<String, ConfigEntry> schemaEntries = schema.config();
                
                // Recreate all entries from schema
                if (objects.isEmpty()) {
                    for (var entry : schemaEntries.entrySet()) {
                        objects.put(entry.getKey(), createInstanceFromEntry(entry.getValue()));
                    }
                    return;
                }
                
                // Remove fields not in the schema
                objects.keySet().removeIf(key -> !schemaEntries.containsKey(key));
                
                // Add missing fields from schema
                for (var entry : schemaEntries.entrySet()) {
                    if (!objects.containsKey(entry.getKey())) {
                        objects.put(entry.getKey(), createInstanceFromEntry(entry.getValue()));
                    }
                }
                
                for (var e : objects.entrySet()) {
                    String key = e.getKey();
                    ConfigInstance<?> inst = e.getValue();
                    ConfigEntry def = schemaEntries.get(key);
                    if (def == null) continue;
                    if (def.isFloat() && inst instanceof FloatConfig) {
                        float cur = ((FloatConfig) inst).value();
                        var ne = (MeshTypeData.NumberConfigEntry) def;
                        float clamped = MathUtils.clamp(cur, ne.minimum(), ne.maximum());
                        if (clamped != cur) {
                            objects.put(key, new FloatConfig(clamped));
                        }
                    } else if (def.isInteger() && inst instanceof IntConfig) {
                        int cur = ((IntConfig) inst).value();
                        var ie = (MeshTypeData.IntegerConfigEntry) def;
                        long clamped = MathUtils.clamp(Long.valueOf(cur), ie.minimum(), ie.maximum());
                        int asInt = (int) clamped;
                        if (asInt != cur) {
                            objects.put(key, new IntConfig(asInt));
                        }
                    }
                }
            }

            public ConfigInstance<?> getValue(String configName) {
                return objects.get(configName);
            }

            public boolean hasValue(String configName) {
                return objects.containsKey(configName);
            }

            public Map<String, ConfigInstance<?>> getMap() {
                return objects;
            }

            public Optional<Number> min(String configName) {
                if (schema == null) return Optional.empty();
                ConfigEntry def = schema.config().get(configName);
                if (def == null) return Optional.empty();
                if (def.isFloat()) {
                    var ne = (MeshTypeData.NumberConfigEntry) def;
                    return Optional.ofNullable(ne.minimum());
                } else if (def.isInteger()) {
                    var ie = (MeshTypeData.IntegerConfigEntry) def;
                    return Optional.ofNullable(ie.minimum());
                }
                return Optional.empty();
            }

            public Optional<Number> max(String configName) {
                if (schema == null) return Optional.empty();
                ConfigEntry def = schema.config().get(configName);
                if (def == null) return Optional.empty();
                if (def.isFloat()) {
                    var ne = (MeshTypeData.NumberConfigEntry) def;
                    return Optional.ofNullable(ne.maximum());
                } else if (def.isInteger()) {
                    var ie = (MeshTypeData.IntegerConfigEntry) def;
                    return Optional.ofNullable(ie.maximum());
                }
                return Optional.empty();
            }

            public void setValue(String configName, ConfigInstance<?> value) {
                if (!objects.containsKey(configName)) {
                    throw new IllegalArgumentException("Config name does not exist: " + configName);
                }

                if (!value.assignableTo(objects.get(configName))) {
                    throw new IllegalArgumentException("Config schema mismatch for: " + configName);
                }

                var instance = objects.get(configName);

                if (this.schema != null) {
                    ConfigEntry def = this.schema.config().get(configName);
                    if (def != null) {
                        if (def.isFloat() && value instanceof FloatConfig) {
                            float cur = ((FloatConfig) value).value();
                            var ne = (MeshTypeData.NumberConfigEntry) def;
                            float clamped = MathUtils.clamp(cur, ne.minimum(), ne.maximum());
                            instance.setValue(clamped);
                            return;
                        } else if (def.isInteger() && value instanceof IntConfig) {
                            int cur = ((IntConfig) value).value();
                            var ie = (MeshTypeData.IntegerConfigEntry) def;
                            long clamped = MathUtils.clamp(Long.valueOf(cur), ie.minimum(), ie.maximum());
                            instance.setValue(clamped);
                            return;
                        }
                    }
                }

                instance.setValueFrom(value);
            }
        }

        /**
         * Fog effect parameters for this cloud layer.
         * Controls when fog starts and ends relative to the camera.
         */
        public static class FogParameters {
            private static final float DEFAULT_FOG_START = 30 * 16.0f; // 30 chunks
            private static final float DEFAULT_FOG_END = 60 * 16.0f; // 60 chunks

            public float FOG_START_DISTANCE = DEFAULT_FOG_START;
            public float FOG_END_DISTANCE = DEFAULT_FOG_END;

            void copy(FogParameters other) {
                this.FOG_START_DISTANCE = other.FOG_START_DISTANCE;
                this.FOG_END_DISTANCE = other.FOG_END_DISTANCE;
            }
        }

        public static class AppearanceParameters {
            private static final boolean DEFAULT_SHADING_ENABLED = false;
            private static final boolean DEFAULT_USES_CUSTOM_ALPHA = true;
            private static final boolean DEFAULT_CUSTOM_BRIGHTNESS = false;
            private static final boolean DEFAULT_USES_CUSTOM_COLOR = false;
            private static final int DEFAULT_BASE_ALPHA = (int) (0.8f * 255);
            private static final float DEFAULT_BRIGHTNESS = 1.0f;
            private static final int DEFAULT_LAYER_COLOR = 0xFFFFFF;
            private static final float DEFAULT_CLOUD_Y_SCALE = 1.5f;
            private static final int DEFAULT_OFFSET = 0;
            private static final float DEFAULT_LAYER_SPEED = 0.03f;
            private static final float DEFAULT_LAYER_HEIGHT_OFFSET = 0.23f;
            private static final String DEFAULT_TEXTURE_NAME = "clouds";
            private static final String DEFAULT_TEXTURE_NAMESPACE = "minecraft";
            private static final boolean DEFAULT_TEXTURE_HORIZONTAL_FLIP = false;
            private static final boolean DEFAULT_TEXTURE_VERTICAL_FLIP = false;
            private static final boolean DEFAULT_PRESERVE_ORIGINAL_TEXTURE_COLOR = false;
            private static final float DEFAULT_OUTLINE_SIZE = 0.7f;
            private static final float DEFAULT_OUTLINE_ALPHA = 0.5f;
            private static final float DEFAULT_OUTLINE_BRIGHTNESS = 1.0f;
            private static final int DEFAULT_OUTLINE_COLOR = 0xFFFFFF;
            private static final boolean DEFAULT_OUTLINE_CUSTOM_BRIGHTNESS = false;
            private static final boolean DEFAULT_OUTLINE_OVERRIDE_TEXTURE_COLOR = true;
            private static final boolean DEFAULT_OUTLINE_ENABLED = false;

            public boolean SHADING_ENABLED = DEFAULT_SHADING_ENABLED;
            public boolean USES_CUSTOM_ALPHA = DEFAULT_USES_CUSTOM_ALPHA;
            public boolean CUSTOM_BRIGHTNESS = DEFAULT_CUSTOM_BRIGHTNESS;
            public boolean USES_CUSTOM_COLOR = DEFAULT_USES_CUSTOM_COLOR;
            public float OUTLINE_SIZE = DEFAULT_OUTLINE_SIZE;
            public float OUTLINE_ALPHA = DEFAULT_OUTLINE_ALPHA;
            public float OUTLINE_BRIGHTNESS = DEFAULT_OUTLINE_BRIGHTNESS;
            public int OUTLINE_COLOR = DEFAULT_OUTLINE_COLOR;
            public boolean OUTLINE_CUSTOM_BRIGHTNESS = DEFAULT_OUTLINE_CUSTOM_BRIGHTNESS;
            public boolean OUTLINE_OVERRIDE_TEXTURE_COLOR = DEFAULT_OUTLINE_OVERRIDE_TEXTURE_COLOR;
            public boolean OUTLINE_ENABLED = DEFAULT_OUTLINE_ENABLED;
            public int BASE_ALPHA = DEFAULT_BASE_ALPHA;
            public float BRIGHTNESS = DEFAULT_BRIGHTNESS;
            public int LAYER_COLOR = DEFAULT_LAYER_COLOR;
            public float CLOUD_Y_SCALE = DEFAULT_CLOUD_Y_SCALE;
            public int LAYER_OFFSET_X = DEFAULT_OFFSET;
            public int LAYER_OFFSET_Z = DEFAULT_OFFSET;
            public float LAYER_SPEED_X = DEFAULT_LAYER_SPEED;
            public float LAYER_SPEED_Z = DEFAULT_LAYER_SPEED;
            public float LAYER_HEIGHT_OFFSET = DEFAULT_LAYER_HEIGHT_OFFSET;
            public String TEXTURE_NAME = DEFAULT_TEXTURE_NAME;
            public String TEXTURE_NAMESPACE = DEFAULT_TEXTURE_NAMESPACE;
            public boolean TEXTURE_HORIZONTAL_FLIP = DEFAULT_TEXTURE_HORIZONTAL_FLIP;
            public boolean TEXTURE_VERTICAL_FLIP = DEFAULT_TEXTURE_VERTICAL_FLIP;
            public boolean PRESERVE_ORIGINAL_TEXTURE_COLOR = DEFAULT_PRESERVE_ORIGINAL_TEXTURE_COLOR;

            void copy(AppearanceParameters other) {
                this.SHADING_ENABLED = other.SHADING_ENABLED;
                this.USES_CUSTOM_ALPHA = other.USES_CUSTOM_ALPHA;
                this.CUSTOM_BRIGHTNESS = other.CUSTOM_BRIGHTNESS;
                this.USES_CUSTOM_COLOR = other.USES_CUSTOM_COLOR;
                this.BASE_ALPHA = other.BASE_ALPHA;
                this.BRIGHTNESS = other.BRIGHTNESS;
                this.LAYER_COLOR = other.LAYER_COLOR;
                this.CLOUD_Y_SCALE = other.CLOUD_Y_SCALE;
                this.LAYER_OFFSET_X = other.LAYER_OFFSET_X;
                this.LAYER_OFFSET_Z = other.LAYER_OFFSET_Z;
                this.LAYER_SPEED_X = other.LAYER_SPEED_X;
                this.LAYER_SPEED_Z = other.LAYER_SPEED_Z;
                this.LAYER_HEIGHT_OFFSET = other.LAYER_HEIGHT_OFFSET;
                this.TEXTURE_NAME = other.TEXTURE_NAME;
                this.TEXTURE_NAMESPACE = other.TEXTURE_NAMESPACE;
                this.TEXTURE_HORIZONTAL_FLIP = other.TEXTURE_HORIZONTAL_FLIP;
                this.TEXTURE_VERTICAL_FLIP = other.TEXTURE_VERTICAL_FLIP;
                this.PRESERVE_ORIGINAL_TEXTURE_COLOR = other.PRESERVE_ORIGINAL_TEXTURE_COLOR;
                this.OUTLINE_SIZE = other.OUTLINE_SIZE;
                this.OUTLINE_ALPHA = other.OUTLINE_ALPHA;
                this.OUTLINE_BRIGHTNESS = other.OUTLINE_BRIGHTNESS;
                this.OUTLINE_COLOR = other.OUTLINE_COLOR;
                this.OUTLINE_CUSTOM_BRIGHTNESS = other.OUTLINE_CUSTOM_BRIGHTNESS;
                this.OUTLINE_OVERRIDE_TEXTURE_COLOR = other.OUTLINE_OVERRIDE_TEXTURE_COLOR;
                this.OUTLINE_ENABLED = other.OUTLINE_ENABLED;
            }
        }

        public enum FadeType {
            STATIC,               // Constant fade with vertical gradient from STATIC_FADE_REL_Y
            DYNAMIC_POSITIONAL    // Fade based on camera position relative to layer
        }

        public static class FadeParameters {
            private static final boolean DEFAULT_FADE_ENABLED = true;
            private static final int DEFAULT_FADE_ALPHA = (int) (0.2f * 255);
            private static final float DEFAULT_TRANSITION_RANGE = 10.0f;
            private static final int DEFAULT_FADE_TO_COLOR = 0xFFFFFF;
            private static final boolean DEFAULT_COLOR_FADE = false;
            private static final boolean DEFAULT_INVERTED_FADE = false;
            private static final FadeType DEFAULT_FADE_TYPE = FadeType.DYNAMIC_POSITIONAL;
            private static final float DEFAULT_STATIC_FADE_REL_Y = 20.0f;

            public boolean FADE_ENABLED = DEFAULT_FADE_ENABLED;
            public int FADE_ALPHA = DEFAULT_FADE_ALPHA;
            public float TRANSITION_RANGE = DEFAULT_TRANSITION_RANGE;
            public int FADE_TO_COLOR = DEFAULT_FADE_TO_COLOR;
            public boolean COLOR_FADE = DEFAULT_COLOR_FADE;
            public boolean INVERTED_FADE = DEFAULT_INVERTED_FADE;
            public FadeType FADE_TYPE = DEFAULT_FADE_TYPE;
            public float STATIC_FADE_REL_Y = DEFAULT_STATIC_FADE_REL_Y; // Reference Y for static fade

            void copy(FadeParameters other) {
                this.FADE_ENABLED = other.FADE_ENABLED;
                this.FADE_ALPHA = other.FADE_ALPHA;
                this.TRANSITION_RANGE = other.TRANSITION_RANGE;
                this.FADE_TO_COLOR = other.FADE_TO_COLOR;
                this.COLOR_FADE = other.COLOR_FADE;
                this.INVERTED_FADE = other.INVERTED_FADE;
                this.FADE_TYPE = other.FADE_TYPE;
                this.STATIC_FADE_REL_Y = other.STATIC_FADE_REL_Y;
            }
        }
    }

    public static class LayerHolder {

        public final List<LayerConfiguration> layers = new ArrayList<>();

        LayerHolder() {
        }

        LayerHolder(int layer) {
            this();
            addDefaultLayers(layer, idx -> new LayerConfiguration(idx >= 0 ? idx : -1));
        }

        public void addLayer(LayerConfiguration l) {
            layers.add(l);
        }


        public void addDefaultLayers(int count, IntFunction<LayerConfiguration> factory) {
            for (int i = 0; i < count; i++) {
                layers.add(factory.apply(i));
            }
        }

        public void removeLayer(int index) {
            if (index >= 0 && index < layers.size()) {
                layers.remove(index);
            }
        }

        public void clear() {
            layers.clear();
        }
    }

    public enum ShadingMode {
        FLAT, SMOOTH
    }

    public enum LightingType {
        STATIC, DYNAMIC
    }

    // maybe add cloth back ..? without preset functionalities
    public enum ConfigBackend {
        YACL
    }

    public enum CloudColorProviderMode {
        VANILLA,
        CUSTOM
    }
    public static class LightingParameters {
        public LightingParameters() {
            // no-op
        }
        private static final float DEFAULT_AMBIENT_LIGHTING = 0.7f;
        private static final float DEFAULT_MAX_LIGHTING_SHADING = 1.0f;
        private static final int DEFAULT_DAY_START = 0;
        private static final int DEFAULT_DAY_END = 13000;
        private static final int DEFAULT_DAY_NOON = 6000;
        public static final int DAY_LENGTH = 24000;
        public static final int MAX_LIGHT_COUNT = 32;
        public static final List<DiffuseLight> DEFAULT_LIGHTS = Arrays.asList(
            new DiffuseLight(new Vector3f(0.0f, -1.0f, 1.0f), 0.3f),
            new DiffuseLight(new Vector3f(-1.0f, 0.0f, 0.0f), 0.2f),
            new DiffuseLight(new Vector3f(1.0f, 0.0f, 0.0f), 0.2f),
            new DiffuseLight(new Vector3f(0.0f, 0.0f, 1.0f), 0.1f),
            new DiffuseLight(new Vector3f(0.0f, 0.0f, -1.0f), 0.1f)
        );

        public float AMBIENT_LIGHTING_STRENGTH = DEFAULT_AMBIENT_LIGHTING;
        public float MAX_LIGHTING_SHADING = DEFAULT_MAX_LIGHTING_SHADING;
        public ShadingMode SHADING_MODE = ShadingMode.FLAT;
        public LightingType LIGHTING_TYPE = LightingType.STATIC;
        
        public int DAY_START = DEFAULT_DAY_START;
        public int DAY_END = DEFAULT_DAY_END;
        public int DAY_NOON = DEFAULT_DAY_NOON;

        public List<DiffuseLight> lights = new ArrayList<>(Arrays.asList(
            new DiffuseLight(new Vector3f(0.0f, -1.0f, 1.0f), 0.3f),
            new DiffuseLight(new Vector3f(-1.0f, 0.0f, 0.0f), 0.2f),
            new DiffuseLight(new Vector3f(1.0f, 0.0f, 0.0f), 0.2f),
            new DiffuseLight(new Vector3f(0.0f, 0.0f, 1.0f), 0.1f),
            new DiffuseLight(new Vector3f(0.0f, 0.0f, -1.0f), 0.1f)
        ));

        void copy(LightingParameters other) {
            this.AMBIENT_LIGHTING_STRENGTH = other.AMBIENT_LIGHTING_STRENGTH;
            this.MAX_LIGHTING_SHADING = other.MAX_LIGHTING_SHADING;
            this.SHADING_MODE = other.SHADING_MODE;
            this.LIGHTING_TYPE = other.LIGHTING_TYPE;
            this.DAY_START = other.DAY_START;
            this.DAY_END = other.DAY_END;
            this.DAY_NOON = other.DAY_NOON;
            this.lights = new ArrayList<>(other.lights);
        }
    }

    public static class WeatherColorConfig {
        // Weather color constants
        private static final int DEFAULT_RAIN_COLOR = 0xB0B0B0;       // gray
        private static final int DEFAULT_THUNDER_COLOR = 0x808080;    // darker gray
        private static final float DEFAULT_RAIN_STRENGTH = 0.3f;
        private static final float DEFAULT_THUNDER_STRENGTH = 0.4f;

        public int rainColor = DEFAULT_RAIN_COLOR;
        public int thunderColor = DEFAULT_THUNDER_COLOR;
        public float rainStrength = DEFAULT_RAIN_STRENGTH;
        public float thunderStrength = DEFAULT_THUNDER_STRENGTH;

        void copy(WeatherColorConfig other) {
            this.rainColor = other.rainColor;
            this.thunderColor = other.thunderColor;
            this.rainStrength = other.rainStrength;
            this.thunderStrength = other.thunderStrength;
        }
    }

    public static class SkyColorKeypoint {
        private static final int DEFAULT_TIME = 6000;      // Noon
        private static final int DEFAULT_COLOR = 0xFFFFFF; // White

        public int time;   // 0–24000
        public int color;  // RGB

        public SkyColorKeypoint(int time, int color) {
            this.time = time;
            this.color = color;
        }

        public SkyColorKeypoint() {
            this(DEFAULT_TIME, DEFAULT_COLOR);
        }
    }

    public static final class Dimension extends DynamicEnum<Dimension> {

        private final String id;

        private Dimension(String id, int ord) {
            super(id, ord);
            this.id = id;
        }

        public static final Dimension OVERWORLD = register( "minecraft:overworld");
        public static final Dimension NETHER = register("minecraft:the_nether");
        public static final Dimension END = register("minecraft:the_end");

        /**
         * Gets the ID that was used to construct the enum.
         * This is the exact original string used to register the enum, which may differ from the enum's name (enum's name is all upper-case, and this is case-sensitive).
         * @return The ID string used to register this enum.
         */
        public String getId() {
            return id;
        }

        public static boolean isBuiltin(Dimension dim) {
            return dim.equals(OVERWORLD) || dim.equals(NETHER) || dim.equals(END);
        }

        static Dimension register(String id) {
            return register(Dimension.class, id, Dimension::new);
        }

        public static Dimension[] values() {
            return values(Dimension.class).toArray(new Dimension[0]);
        }

        public static Dimension tryRegister(IdentifierWrapper id) {
            return getOrRegister(id);
        }

        public static Dimension getOrRegister(IdentifierWrapper id) {
            String idStr = id.toString();
            Dimension existing = fromId(idStr);
            if (existing != null) {
                return existing;
            }
            return register(idStr);
        }

        public static Dimension getOrRegister(String id) {
            Dimension existing = fromId(id);
            if (existing != null) {
                return existing;
            }
            return register(id);
        }

        public static Dimension get(String id) {
            return fromId(id);
        }
        
        public static Dimension fromId(String id) {
            try {
                return valueOf(Dimension.class, id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public static Dimension fromOrdinal(int ordinal) {
            Dimension[] dims = values();
            if (ordinal >= 0 && ordinal < dims.length) {
                return dims[ordinal];
            }
            return null;
        }
    }
}
