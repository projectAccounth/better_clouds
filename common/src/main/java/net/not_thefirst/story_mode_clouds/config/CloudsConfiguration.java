package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshType;
import net.not_thefirst.story_mode_clouds.renderer.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.renderer.utils.DiffuseLight;
import net.not_thefirst.story_mode_clouds.utils.CloudColorProvider;
import net.not_thefirst.story_mode_clouds.utils.interp.world.NumberSequence;

public class CloudsConfiguration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/cloud_configs.json");

    public boolean CLOUDS_RENDERED = true;

    public static CloudsConfiguration INSTANCE = new CloudsConfiguration();

    public static class LightingParameters {
        public float AMBIENT_LIGHTING_STRENGTH  = 0.7f;
        public float MAX_LIGHTING_SHADING       = 0.8f;

        public List<DiffuseLight> lights = new ArrayList<>(List.of(
            new DiffuseLight(new Vector3f(0.5f, 0.8f, 1.0f), 0.6f),
            new DiffuseLight(new Vector3f(-0.3f, 0.3f, 0.6f), 0.5f)
        ));

        public LightingParameters() {
        }
    }

    public static class SkyColorKeypoint {
        public int time;   // 0–24000
        public int color;  // RGB

        public SkyColorKeypoint(int time, int color) {
            this.time = time;
            this.color = color;
        }

        public SkyColorKeypoint() {
            this(6000, 0xffffff);
        }
    }

    public enum CloudColorProviderMode {
        VANILLA,
        CUSTOM
    }

    // chatgpt generated color
    public static final List<SkyColorKeypoint> DEFAULT_COLORS = List.of(
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

    public List<SkyColorKeypoint> CLOUD_COLOR = new ArrayList<>(List.of(
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

    public LightingParameters LIGHTING = new LightingParameters();
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
    public static class LayerConfiguration {
        public LayerConfiguration(int idx) {
            LAYER_IDX = idx; 
        }

        public LayerConfiguration() {
            this(0);
        }

        public static class BevelParameters {
            public float BEVEL_SIZE            = 0.1f;
            public int   BEVEL_EDGE_SEGMENTS   = 8;
            public int   BEVEL_CORNER_SEGMENTS = 8;

            void copy(BevelParameters other) {
                this.BEVEL_SIZE = other.BEVEL_SIZE;
                this.BEVEL_EDGE_SEGMENTS = other.BEVEL_EDGE_SEGMENTS;
                this.BEVEL_CORNER_SEGMENTS = other.BEVEL_CORNER_SEGMENTS;
            }
        };

        public static class PerformanceParameters {
            public int MESH_REBUILD_BUDGET_MS       = 2;

            void copy(PerformanceParameters other) {
                this.MESH_REBUILD_BUDGET_MS = other.MESH_REBUILD_BUDGET_MS;
            }
        };

        public static class FogParameters {
            public float FOG_START_DISTANCE = 50.0f;
            public float FOG_END_DISTANCE   = 200.0f;

            void copy(FogParameters other) {
                this.FOG_START_DISTANCE = other.FOG_START_DISTANCE;
                this.FOG_END_DISTANCE = other.FOG_END_DISTANCE;
            }
        };

        public static class AppearanceParameters {
            public boolean SHADING_ENABLED    = false; // Whether clouds should appear shaded (darker) when the sun is behind them
            public boolean USES_CUSTOM_ALPHA  = true;  // Whether clouds should use custom alpha
            public boolean CUSTOM_BRIGHTNESS  = true;  // Whether clouds should be rendered with custom brightness' brightness (what the hell)
            public boolean USES_CUSTOM_COLOR  = false; // Uses custom color for clouds

            public int     BASE_ALPHA         = (int) (0.8f * 255); // Base transparency of clouds (considering no fade applied)
            public float   BRIGHTNESS         = 1.0f; // Base brightness color
            public int     LAYER_COLOR        = 0xffffff;
            public float   CLOUD_Y_SCALE      = 1.5f; // Scale factor for cloud height
            
            public int     LAYER_OFFSET_X        = 0;
            public int     LAYER_OFFSET_Z        = 0;

            public float   LAYER_SPEED_X         = 0.03f;
            public float   LAYER_SPEED_Z         = 0.03f;

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

        public static class FadeParameters {
            public boolean FADE_ENABLED     = true;
            public int     FADE_ALPHA       = (int) (0.2f * 255); // The minimum fade alpha (fade at the top)
            public float   TRANSITION_RANGE = 10.0f;

            void copy(FadeParameters other) {
                this.FADE_ENABLED = other.FADE_ENABLED;
                this.FADE_ALPHA = other.FADE_ALPHA;
                this.TRANSITION_RANGE = other.TRANSITION_RANGE;
            }
        }

        public BevelParameters       BEVEL       = new BevelParameters();
        public PerformanceParameters PERFORMANCE = new PerformanceParameters();
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

        public int GetLayerIndex() { return LAYER_IDX; }

        void copy(LayerConfiguration other) {
            this.NAME = other.NAME;
            this.FOG_ENABLED = other.FOG_ENABLED;
            this.IS_ENABLED = other.IS_ENABLED;
            this.LAYER_RENDERED = other.LAYER_RENDERED;
            this.LAYER_HEIGHT = other.LAYER_HEIGHT;
            this.MODE = other.MODE;
            this.BEVEL.copy(other.BEVEL);
            this.PERFORMANCE.copy(other.PERFORMANCE);
            this.APPEARANCE.copy(other.APPEARANCE);
            this.FADE.copy(other.FADE);
            this.FOG.copy(other.FOG);
        }
    };

    public static class LayerHolder {

        public List<LayerConfiguration> layers = new ArrayList<>();

        public LayerHolder() {
        }

        public LayerHolder(int layer) {
            this();
            addDefaultLayers(layer, (idx) -> { return new LayerConfiguration(idx != null ? idx : -1); });
        }

        public void addLayer(LayerConfiguration l) {
            layers.add(l);
        }


        public void addDefaultLayers(int count, Function<Integer, LayerConfiguration> factory) {
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
            return;
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
        CONFIG_FILE.getParentFile().mkdirs();

        if (RendererHolder.get() != null) {
            RendererHolder.get().markForRebuild();
        }

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Screen createConfigScreen(Screen parent) {
        if (Compat.hasClothConfig()) {
            return ClothConfigScreen.createConfigScreen(parent);
        }
        return null;
    }
}
