package net.not_thefirst.story_mode_clouds.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    public float   FADE_ALPHA            = 0.2f; // The maximum fade alpha.
    public float   CLOUD_Y_SCALE         = 1.5f; // Scale factor for cloud height
    public float   BRIGHTNESS            = 1.0f; // Base brightness color for clouds
    public float   BASE_ALPHA            = 0.8f; // Base transparency of clouds

    public int     CLOUD_COLOR           = 0xFFFFFF; // Color of the clouds in RGB format (default white)

    public int     CLOUD_LAYERS          = 1;      // Layers
    public float   CLOUD_LAYERS_SPACING  = 14.0f;  // Vertical spacing
    public boolean CLOUD_RANDOM_LAYERS   = true;

    public static int MAX_LAYER_COUNT = 10;

    public static CloudsConfiguration INSTANCE = new CloudsConfiguration();

    private static CloudsConfiguration LAST_STATE = null;

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, CloudsConfiguration.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkForChanges(Runnable rebuildFunction) {
        if (LAST_STATE == null) {
            LAST_STATE = INSTANCE.copy();
            return;
        }
        if (!INSTANCE.equals(LAST_STATE)) {
            LAST_STATE = INSTANCE.copy();
            rebuildFunction.run(); // <-- call your rebuild code here
        }
    }

    // --- Helpers ---
    private CloudsConfiguration copy() {
        // Deep copy via JSON (safe & quick since config is small)
        return GSON.fromJson(GSON.toJson(this), CloudsConfiguration.class);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CloudsConfiguration other)) return false;
        return GSON.toJson(this).equals(GSON.toJson(other));
    }
}
