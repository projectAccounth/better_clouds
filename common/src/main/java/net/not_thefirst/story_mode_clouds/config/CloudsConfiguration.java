package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3f;

import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.interp.world.NumberSequence;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;

public class CloudsConfiguration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILENAME = "cloud_configs.json";
    private static final File CONFIG_FILE = new File(CONFIG_DIR + "/" + CONFIG_FILENAME);
    
    // Backup throttling
    private static long lastBackupTime = 0;
    private static final long BACKUP_THROTTLE_MS = 5000; // Minimum 5 seconds between main config backups

    public boolean CLOUDS_RENDERED = true;
    private static CloudsConfiguration INSTANCE = new CloudsConfiguration();
    
    // Track when config was last modified for caching purposes
    private long lastModificationTime = System.currentTimeMillis();

    public enum ShadingMode {
        GOURAUD, PHONG
    };

    public enum LightingType {
        STATIC, DYNAMIC
    };

    public enum ConfigBackend {
        YACL
    };

    public static class LightingParameters {
        private static final float DEFAULT_AMBIENT_LIGHTING = 0.7f;
        private static final float DEFAULT_MAX_LIGHTING_SHADING = 0.8f;
        private static final int DEFAULT_DAY_START = 0;
        private static final int DEFAULT_DAY_END = 13000;
        private static final int DEFAULT_DAY_NOON = 6000;
        public static final int DAY_LENGTH = 24000;
        public static final int MAX_LIGHT_COUNT = 32;

        public float AMBIENT_LIGHTING_STRENGTH = DEFAULT_AMBIENT_LIGHTING;
        public float MAX_LIGHTING_SHADING = DEFAULT_MAX_LIGHTING_SHADING;
        public ShadingMode SHADING_MODE = ShadingMode.GOURAUD;
        public LightingType LIGHTING_TYPE = LightingType.STATIC;
        
        public int DAY_START = DEFAULT_DAY_START;
        public int DAY_END = DEFAULT_DAY_END;
        public int DAY_NOON = DEFAULT_DAY_NOON;

        public List<DiffuseLight> lights = new ArrayList<>(Arrays.asList(
            new DiffuseLight(new Vector3f(0.5f, -0.8f, 1.0f), 0.6f),
            new DiffuseLight(new Vector3f(-0.3f, -0.3f, 0.6f), 0.5f)
        ));

        public LightingParameters() {
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

    public enum CloudColorProviderMode {
        VANILLA,
        CUSTOM
    }

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
  
    public CloudColorProviderMode COLOR_MODE = CloudColorProviderMode.VANILLA;

    public LightingParameters LIGHTING        = new LightingParameters();
    public WeatherColorConfig WEATHER_COLOR   = new WeatherColorConfig();
    public int                CLOUD_GRID_SIZE = 64;

    private LayerHolder LAYERS = new LayerHolder();
    public LayerHolder getHolder() { return LAYERS; }

    public LayerConfiguration getLayer(int idx) {
        if (LAYERS == null || idx < 0 || idx >= LAYERS.layers.size()) {
            throw new IndexOutOfBoundsException("Layer index out of bounds: " + idx);
        }
        return LAYERS.layers.get(idx);
    }
 
    public int getLayerCount() {
        return LAYERS == null ? 0 : LAYERS.layers.size();
    }

    public LayerConfiguration template = new LayerConfiguration();
    
    /**
     * Configuration for a single cloud rendering layer.
     * Each layer can have independent appearance, fog, fade, and performance settings.
     * Multiple layers can be stacked to create complex cloud effects.
     */
    public static class LayerConfiguration {
        public LayerConfiguration(int idx) {
            LAYER_IDX = idx; 
        }

        public LayerConfiguration() {
            this(0);
        }

        /**
         * Bevel parameters for cloud mesh geometry.
         * Controls the smoothing/beveling of edges in the cloud mesh for better appearance.
         */
        public static class BevelParameters {
            private static final float DEFAULT_BEVEL_SIZE = 0.1f;
            private static final int DEFAULT_EDGE_SEGMENTS = 8;
            private static final int DEFAULT_CORNER_SEGMENTS = 8;

            public float BEVEL_SIZE = DEFAULT_BEVEL_SIZE;
            public int BEVEL_EDGE_SEGMENTS = DEFAULT_EDGE_SEGMENTS;
            public int BEVEL_CORNER_SEGMENTS = DEFAULT_CORNER_SEGMENTS;

            /**
             * Copy values from another BevelParameters instance.
             */
            void copy(BevelParameters other) {
                this.BEVEL_SIZE = other.BEVEL_SIZE;
                this.BEVEL_EDGE_SEGMENTS = other.BEVEL_EDGE_SEGMENTS;
                this.BEVEL_CORNER_SEGMENTS = other.BEVEL_CORNER_SEGMENTS;
            }
        }

        /**
         * Fog effect parameters for this cloud layer.
         * Controls when fog starts and ends relative to the camera.
         */
        public static class FogParameters {
            private static final float DEFAULT_FOG_START = 50.0f;
            private static final float DEFAULT_FOG_END = 200.0f;

            public float FOG_START_DISTANCE = DEFAULT_FOG_START;
            public float FOG_END_DISTANCE = DEFAULT_FOG_END;

            void copy(FogParameters other) {
                this.FOG_START_DISTANCE = other.FOG_START_DISTANCE;
                this.FOG_END_DISTANCE = other.FOG_END_DISTANCE;
            }
        };

        /**
         * Visual appearance parameters for the cloud layer.
         * Controls color, alpha, brightness, shading, and animation propertie.
         */
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

            public boolean SHADING_ENABLED = DEFAULT_SHADING_ENABLED;
            public boolean USES_CUSTOM_ALPHA = DEFAULT_USES_CUSTOM_ALPHA;
            public boolean CUSTOM_BRIGHTNESS = DEFAULT_CUSTOM_BRIGHTNESS;
            public boolean USES_CUSTOM_COLOR = DEFAULT_USES_CUSTOM_COLOR;
            public int BASE_ALPHA = DEFAULT_BASE_ALPHA;
            public float BRIGHTNESS = DEFAULT_BRIGHTNESS;
            public int LAYER_COLOR = DEFAULT_LAYER_COLOR;
            public float CLOUD_Y_SCALE = DEFAULT_CLOUD_Y_SCALE;
            public int LAYER_OFFSET_X = DEFAULT_OFFSET;
            public int LAYER_OFFSET_Z = DEFAULT_OFFSET;
            public float LAYER_SPEED_X = DEFAULT_LAYER_SPEED;
            public float LAYER_SPEED_Z = DEFAULT_LAYER_SPEED;

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

        public BevelParameters       BEVEL       = new BevelParameters();
        public AppearanceParameters  APPEARANCE  = new AppearanceParameters();
        public FadeParameters        FADE        = new FadeParameters();
        public FogParameters         FOG         = new FogParameters();

        private int LAYER_IDX;

        public String  NAME                  = "Minecraft";
        public boolean FOG_ENABLED           = true;
        public boolean IS_ENABLED            = true;
        public boolean LAYER_RENDERED        = true;
        
        public int     LAYER_HEIGHT          = 128;

        public String MODE = "NORMAL";

        public int getLayerIndex() { return LAYER_IDX; }

        void copy(LayerConfiguration other) {
            this.NAME = other.NAME;
            this.FOG_ENABLED = other.FOG_ENABLED;
            this.IS_ENABLED = other.IS_ENABLED;
            this.LAYER_RENDERED = other.LAYER_RENDERED;
            this.LAYER_HEIGHT = other.LAYER_HEIGHT;
            this.MODE = other.MODE;
            this.BEVEL.copy(other.BEVEL);
            this.APPEARANCE.copy(other.APPEARANCE);
            this.FADE.copy(other.FADE);
            this.FOG.copy(other.FOG);
        }
    };

    public static class LayerHolder {

        public final List<LayerConfiguration> layers = new ArrayList<>();

        public LayerHolder() {
        }

        public LayerHolder(int layer) {
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

    public CloudsConfiguration() {
        LAYERS.addLayer(new LayerConfiguration(0));
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            INSTANCE = GSON.fromJson(reader, CloudsConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE = null;
        }

        if (INSTANCE == null) {
            INSTANCE = new CloudsConfiguration();
        }

        if (INSTANCE.CLOUD_COLOR == null || INSTANCE.CLOUD_COLOR.isEmpty()) {
            INSTANCE.CLOUD_COLOR = new ArrayList<>(DEFAULT_COLORS);
        }

        INSTANCE.CLOUD_COLOR.sort(Comparator.comparingInt(kp -> kp.time));

        NumberSequence sequence = CloudColorProvider.getCurrentSequence();
        sequence.clearKeypoints();

        for (SkyColorKeypoint kp : INSTANCE.CLOUD_COLOR) {
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
        SkyColorKeypoint first = INSTANCE.CLOUD_COLOR.get(0);
        SkyColorKeypoint last = INSTANCE.CLOUD_COLOR.get(INSTANCE.CLOUD_COLOR.size() - 1);

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
        INSTANCE.markModified();
        
        try {
            saveSafely();
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
    private static void saveSafely() throws IOException {
        File dir = new File(CONFIG_DIR, "cloud_tweaks");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File tempFile = new File(dir, CONFIG_FILENAME + ".tmp");

        try {
            try (FileWriter fw = new FileWriter(tempFile)) {
                GSON.toJson(INSTANCE, fw);
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
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Create a timestamped backup of the config file. Includes throttling to prevent backup spam.
     */
    private static void createTimestampedBackup(File source) throws IOException {
        // Check throttling: only backup if enough time has passed since last backup
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackupTime < BACKUP_THROTTLE_MS) {
            LoggerProvider.get().debug("Main config backup skipped - throttled (< " + BACKUP_THROTTLE_MS + "ms)");
            return;
        }
        
        File backupDir = new File(CONFIG_DIR, "cloud_tweaks/backups");
        backupDir.mkdirs();
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        File backupFile = new File(backupDir, "cloud_configs_" + timestamp + ".json");
        Files.copy(source.toPath(), backupFile.toPath(), 
            StandardCopyOption.REPLACE_EXISTING);
        lastBackupTime = currentTime;
    }

    /**
     * Copy all settings from another configuration to this one.
     * Useful for applying presets.
     */
    public void copyFrom(CloudsConfiguration other) {
        this.CLOUDS_RENDERED = other.CLOUDS_RENDERED;
        this.CLOUD_COLOR = new ArrayList<>(other.CLOUD_COLOR);
        this.COLOR_MODE = other.COLOR_MODE;
        this.LIGHTING = new LightingParameters();
        this.LIGHTING.AMBIENT_LIGHTING_STRENGTH = other.LIGHTING.AMBIENT_LIGHTING_STRENGTH;
        this.LIGHTING.MAX_LIGHTING_SHADING = other.LIGHTING.MAX_LIGHTING_SHADING;
        this.LIGHTING.SHADING_MODE = other.LIGHTING.SHADING_MODE;
        this.LIGHTING.LIGHTING_TYPE = other.LIGHTING.LIGHTING_TYPE;
        this.LIGHTING.DAY_START = other.LIGHTING.DAY_START;
        this.LIGHTING.DAY_END = other.LIGHTING.DAY_END;
        this.LIGHTING.DAY_NOON = other.LIGHTING.DAY_NOON;
        this.LIGHTING.lights = new ArrayList<>(other.LIGHTING.lights);
        this.WEATHER_COLOR = new WeatherColorConfig();
        this.WEATHER_COLOR.rainColor = other.WEATHER_COLOR.rainColor;
        this.WEATHER_COLOR.thunderColor = other.WEATHER_COLOR.thunderColor;
        this.WEATHER_COLOR.rainStrength = other.WEATHER_COLOR.rainStrength;
        this.WEATHER_COLOR.thunderStrength = other.WEATHER_COLOR.thunderStrength;
        this.CLOUD_GRID_SIZE = other.CLOUD_GRID_SIZE;
        
        this.LAYERS.clear();
        for (LayerConfiguration otherLayer : other.LAYERS.layers) {
            LayerConfiguration newLayer = new LayerConfiguration(otherLayer.getLayerIndex());
            newLayer.copy(otherLayer);
            this.LAYERS.addLayer(newLayer);
        }
    }

    public static CloudsConfiguration getInstance() { return INSTANCE; }

    /**
     * Get the last modification time of this configuration.
     * Used for caching purposes to avoid regenerating expensive exports.
     */
    public long getLastModifiedTime() {
        return lastModificationTime;
    }

    /**
     * Mark this configuration as modified. Called when any setting changes.
     */
    public void markModified() {
        lastModificationTime = System.currentTimeMillis();
    }

    // Getters and setters for config binding (planning to also use other backends)
    public boolean getCloudsRendered() { return CLOUDS_RENDERED; }
    public void setCloudsRendered(boolean value) { CLOUDS_RENDERED = value; }

    public int getCloudGridSize() { return CLOUD_GRID_SIZE; }
    public void setCloudGridSize(int value) { CLOUD_GRID_SIZE = value; }

    public WeatherColorConfig getWeather() { return WEATHER_COLOR; }
}
