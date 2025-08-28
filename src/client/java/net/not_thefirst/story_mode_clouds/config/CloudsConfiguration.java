package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.not_thefirst.story_mode_clouds.api.ClothConfigScreen;
import net.not_thefirst.story_mode_clouds.compat.Compat;
import net.not_thefirst.story_mode_clouds.utils.CloudRendererHolder;

public class CloudsConfiguration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/cloud_configs.json");

    public boolean IS_ENABLED = true;

    public boolean APPEARS_SHADED        = false; // Whether clouds should appear shaded (darker) when the sun is behind them
    public boolean USES_CUSTOM_ALPHA     = true; // Whether clouds should use custom alpha values
    public boolean CUSTOM_BRIGHTNESS     = true; // Whether clouds should be rendered with custom brightness brightness (even at night)
    public boolean USES_CUSTOM_COLOR     = false; // Uses custom color for clouds
    public boolean DEBUG_v0              = true;
    public boolean RANDOMIZED_Y          = true;
    public boolean FADE_ENABLED          = true;
    public boolean FOG_ENABLED           = true;

    public float   FADE_ALPHA            = 0.2f; // The maximum fade alpha.
    public float   CLOUD_Y_SCALE         = 1.5f; // Scale factor for cloud height
    public float   BRIGHTNESS            = 1.0f; // Base brightness color for clouds
    public float   BASE_ALPHA            = 0.8f; // Base transparency of clouds
    public float   TRANSITION_RANGE      = 10.0f;

    public int     CLOUD_COLOR           = 0xFFFFFF; // Color of the clouds in RGB format (default white)
    public int[]   CLOUD_COLORS          = new int[MAX_LAYER_COUNT];

    public int     CLOUD_LAYERS          = 1;      // Layers
    public float   CLOUD_LAYERS_SPACING  = 14.0f;  // Vertical spacing
    public boolean CLOUD_RANDOM_LAYERS   = true;

    public static int MAX_LAYER_COUNT = 10; // Constant for now

    public static CloudsConfiguration INSTANCE = new CloudsConfiguration();

    CloudsConfiguration() {
        Arrays.fill(CLOUD_COLORS, 0xFFFFFF);
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            INSTANCE = GSON.fromJson(reader, CloudsConfiguration.class);

            if (INSTANCE.CLOUD_COLORS == null || INSTANCE.CLOUD_COLORS.length < MAX_LAYER_COUNT) {
                int[] newArr = new int[MAX_LAYER_COUNT];
                Arrays.fill(newArr, 0xFFFFFF);
                if (INSTANCE.CLOUD_COLORS != null) {
                    System.arraycopy(INSTANCE.CLOUD_COLORS, 0, newArr, 0, INSTANCE.CLOUD_COLORS.length);
                }
                INSTANCE.CLOUD_COLORS = newArr;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();

        Minecraft client = Minecraft.getInstance();
        LevelRenderer renderer = client.levelRenderer;
        if (renderer != null && ((CloudRendererHolder) renderer).getCloudRenderer() != null) {
            ((CloudRendererHolder) renderer).getCloudRenderer().markForRebuild();
        }

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Screen createConfigScreen(Screen parent) {
        if (Compat.hasClothConfig()) {
            return ClothConfigScreen.create(parent);
        } else {
            return new CloudsConfigScreen(parent);
        }
    }
}
