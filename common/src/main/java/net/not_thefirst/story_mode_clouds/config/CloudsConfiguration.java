package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;

public class CloudsConfiguration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/cloud_configs.json");

    public boolean CLOUDS_RENDERED = true;

    public static CloudsConfiguration INSTANCE = new CloudsConfiguration();

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
        };

        public static class LightingParameters {
            public float AMBIENT_OCCLUSION_FACTOR = 0.5f;
            public float DIFFUSE_LIGHTING_FACTOR  = 0.5f;
            public float RIM_LIGHTING_FACTOR      = 0.5f;
        };

        public static class PerformanceParameters {
            public int MAX_CELLS_RENDERED_PER_FRAME = 16;
            public int MESH_REBUILD_BUDGET_MS       = 2;
        };

        public static class AppearanceParameters {
            // copy over
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
        }

        public static class FadeParameters {
            // copying the below over
            public boolean FADE_ENABLED     = true;
            public int     FADE_ALPHA       = (int) (0.2f * 255); // The minimum fade alpha (fade at the top)
            public float   TRANSITION_RANGE = 10.0f;
        }

        public BevelParameters       BEVEL       = new BevelParameters();
        public LightingParameters    LIGHTING    = new LightingParameters();
        public PerformanceParameters PERFORMANCE = new PerformanceParameters();
        public AppearanceParameters  APPEARANCE  = new AppearanceParameters();
        public FadeParameters        FADE        = new FadeParameters();

        // maybe_unused
        private int LAYER_IDX;

        public String  NAME                  = "Minecraft";
        public boolean FOG_ENABLED           = true;
        public boolean IS_ENABLED            = true;
        public boolean LAYER_RENDERED        = true;
        
        public int     LAYER_HEIGHT          = 128;

        @ConfigEntry.Gui.EnumHandler(option=ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CustomCloudRenderer.CloudMode MODE = CustomCloudRenderer.CloudMode.NORMAL;

        public int GetLayerIndex() { return LAYER_IDX; }
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
