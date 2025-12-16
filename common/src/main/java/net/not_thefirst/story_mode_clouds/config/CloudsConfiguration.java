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

import me.shedaniel.autoconfig.AutoConfig;
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

    public void applyFromCloth(ClothConfigClass config) {
        if (config == null) return;

        this.CLOUDS_RENDERED = config.global.CLOUDS_RENDERED;
        if (this.LAYERS == null) this.LAYERS = new LayerHolder();

        int needed = config.layers != null ? config.layers.layers.size() : 0;
        if (this.LAYERS.layers.size() > needed) {
            while (this.LAYERS.layers.size() > needed) {
                this.LAYERS.layers.remove(this.LAYERS.layers.size() - 1);
            }
        } else {
            int start = this.LAYERS.layers.size();
            for (int i = start; i < needed; i++) {
                this.LAYERS.layers.add(new LayerConfiguration(i));
            }
        }

        for (int i = 0; i < needed; i++) {
            LayerConfiguration mainLayer = this.LAYERS.layers.get(i);
            LayerConfiguration clothLayer = config.layers.layers.get(i);
            mainLayer.copy(clothLayer);
        }
    }
    public static class LayerConfiguration {
        public LayerConfiguration(int idx) {
            LAYER_IDX = idx; 
        }

        public LayerConfiguration() {
            this(0);
        }

        @ConfigEntry.Gui.Excluded
        // maybe_unused
        private int LAYER_IDX;

        public String  NAME                  = "Minecraft";
        public boolean APPEARS_SHADED        = false; // Whether clouds should appear shaded (darker) when the sun is behind them
        public boolean USES_CUSTOM_ALPHA     = true; // Whether clouds should use custom alpha values
        public boolean CUSTOM_BRIGHTNESS     = true; // Whether clouds should be rendered with custom brightness' brightness (what the hell)
        public boolean USES_CUSTOM_COLOR     = false; // Uses custom color for clouds
        public boolean FADE_ENABLED          = true;
        public boolean FOG_ENABLED           = true;
        public boolean IS_ENABLED            = true;
        public boolean LAYER_RENDERED        = true;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 255)
        public int     FADE_ALPHA            = (int) (0.2f * 255); // The minimum fade alpha (fade at the top)
        public int     LAYER_HEIGHT          = 128;
        public float   CLOUD_Y_SCALE         = 1.5f; // Scale factor for cloud height
        public float   BRIGHTNESS            = 1.0f; // Base brightness color for clouds
        
        @ConfigEntry.BoundedDiscrete(min = 0, max = 255)
        public int     BASE_ALPHA            = (int) (0.8f * 255); // Base transparency of clouds (considering no fade applied)
        public float   TRANSITION_RANGE      = 10.0f;
        public int     LAYER_OFFSET_X        = 0;
        public int     LAYER_OFFSET_Z        = 0;

        @ConfigEntry.ColorPicker
        public int     LAYER_COLOR           = 0xffffff;

        @ConfigEntry.Gui.EnumHandler(option=ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CustomCloudRenderer.CloudMode MODE = CustomCloudRenderer.CloudMode.NORMAL;

        public int GetLayerIndex() { return LAYER_IDX; }

        void copy(CloudsConfiguration.LayerConfiguration layerData) {
            APPEARS_SHADED        = layerData.APPEARS_SHADED;
            USES_CUSTOM_ALPHA     = layerData.USES_CUSTOM_ALPHA;
            CUSTOM_BRIGHTNESS     = layerData.CUSTOM_BRIGHTNESS;
            USES_CUSTOM_COLOR     = layerData.USES_CUSTOM_COLOR;
            FADE_ENABLED          = layerData.FADE_ENABLED;
            FOG_ENABLED           = layerData.FOG_ENABLED;
            LAYER_HEIGHT          = layerData.LAYER_HEIGHT;

            FADE_ALPHA            = layerData.FADE_ALPHA;
            CLOUD_Y_SCALE         = layerData.CLOUD_Y_SCALE;
            BRIGHTNESS            = layerData.BRIGHTNESS;
            BASE_ALPHA            = layerData.BASE_ALPHA;
            TRANSITION_RANGE      = layerData.TRANSITION_RANGE;

            LAYER_COLOR           = layerData.LAYER_COLOR;
            LAYER_IDX             = layerData.GetLayerIndex();
            MODE                  = layerData.MODE;
            IS_ENABLED            = layerData.IS_ENABLED;
            LAYER_OFFSET_X        = layerData.LAYER_OFFSET_X;
            LAYER_OFFSET_Z        = layerData.LAYER_OFFSET_Z;
            LAYER_RENDERED        = layerData.LAYER_RENDERED;
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
            return AutoConfig.getConfigScreen(ClothConfigClass.class, parent).get();
        }
        return null;
    }
}
