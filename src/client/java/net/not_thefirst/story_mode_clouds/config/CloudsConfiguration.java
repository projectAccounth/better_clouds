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

    public boolean APPEARS_SHADED = false;
    public boolean USES_CUSTOM_ALPHA = true;
    public boolean FULLBRIGHT = true;

    public float FADE_RANGE = 6.0F;
    public float CLOUD_Y_SCALE = 1.5f;
    public float BRIGHTNESS = 1.0f;
    public float BASE_ALPHA = 0.8f;

    public static CloudsConfiguration INSTANCE = new CloudsConfiguration();

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
}
