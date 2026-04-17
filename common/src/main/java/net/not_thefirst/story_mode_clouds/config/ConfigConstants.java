package net.not_thefirst.story_mode_clouds.config;

/**
 * UI consts
 */
public class ConfigConstants {
    
    public static final int MIN_GRID_SIZE = 16;
    public static final int MAX_GRID_SIZE = 512;
    public static final int GRID_SIZE_STEP = 16;
    
    public static final float MIN_BRIGHTNESS = 0.0f;
    public static final float MAX_BRIGHTNESS = 1.0f;
    public static final float BRIGHTNESS_STEP = 0.01f;
    
    public static final float DEFAULT_AMBIENT_LIGHTING = 0.7f;
    public static final float DEFAULT_MAX_LIGHTING_SHADING = 0.8f;
    
    public static final int DAY_LENGTH = 24000;
    public static final int DEFAULT_DAY_START = 0;
    public static final int DEFAULT_DAY_END = 13000;
    public static final int DEFAULT_DAY_NOON = 6000;
    
    public static final int MAX_LIGHT_COUNT = 32;
    public static final int MIN_LIGHT_COUNT = 0;
    public static final float MIN_LIGHT_INTENSITY = 0.0f;
    public static final float MAX_LIGHT_INTENSITY = 1.0f;
    
    public static final int DEFAULT_RAIN_COLOR = 0xB0B0B0;       // dark gray
    public static final int DEFAULT_THUNDER_COLOR = 0x808080;    // darker dark gray
    public static final float DEFAULT_RAIN_STRENGTH = 0.3f;
    public static final float DEFAULT_THUNDER_STRENGTH = 0.4f;
    public static final float MIN_WEATHER_STRENGTH = 0.0f;
    public static final float MAX_WEATHER_STRENGTH = 1.0f;
    
    public static final int DEFAULT_KEYPOINT_TIME = 6000;      // Noon
    public static final int DEFAULT_KEYPOINT_COLOR = 0xFFFFFF; // White
    public static final int MIN_TIME = 0;
    public static final int MAX_TIME = 23999;
    public static final int MIN_COLOR = 0x000000;
    public static final int MAX_COLOR = 0xFFFFFF;
    
    public static final int MIN_LAYERS = 0;
    public static final int MAX_LAYERS = 20;
    public static final int MIN_LAYER_HEIGHT = 0;
    public static final int MAX_LAYER_HEIGHT = 256;
    public static final int MIN_ALPHA = 0;
    public static final int MAX_ALPHA = 255;
    public static final float MIN_LAYER_SCALE = 0.1f;
    public static final float MAX_LAYER_SCALE = 10.0f;
    public static final float MIN_LAYER_SPEED = -0.1f;
    public static final float MAX_LAYER_SPEED = 0.1f;
    
    public static final float MIN_FOG_DISTANCE = 0.0f;
    public static final float MAX_FOG_START_DISTANCE = 800.0f;
    public static final float MAX_FOG_END_DISTANCE = 5000.0f;
    public static final float DEFAULT_FOG_START = 50.0f;
    public static final float DEFAULT_FOG_END = 200.0f;
    
    public static final float MIN_BEVEL_SIZE = 0.0f;
    public static final float MAX_BEVEL_SIZE = 10.0f;
    public static final int MIN_EDGE_SEGMENTS = 1;
    public static final int MAX_EDGE_SEGMENTS = 32;
    public static final int MIN_CORNER_SEGMENTS = 1;
    public static final int MAX_CORNER_SEGMENTS = 32;
    public static final float DEFAULT_BEVEL_SIZE = 0.1f;
    public static final int DEFAULT_EDGE_SEGMENTS = 8;
    public static final int DEFAULT_CORNER_SEGMENTS = 8;
    public static final int MIN_REBUILD_BUDGET_MS = 1;
    public static final int MAX_REBUILD_BUDGET_MS = 100;
    public static final int DEFAULT_REBUILD_BUDGET_MS = 2;

    public static final int MIN_CLOUD_GRID_SIZE = 16;
    public static final int MAX_CLOUD_GRID_SIZE = 512;
    public static final int CLOUD_GRID_SIZE_STEP = 2;

    public static final int MAX_LAYER_OFFSET = 1000;

    public static final float MAX_AXIS_VELOCITY = 20;

    
    public static final String PRESET_DIRECTORY = "config/cloud_tweaks/cloud_presets";
    public static final String PRESET_EXTENSION = ".json";
    
    private ConfigConstants() {
        throw new AssertionError("h");
    }
}
