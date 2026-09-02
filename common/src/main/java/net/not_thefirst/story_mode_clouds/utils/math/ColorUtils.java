package net.not_thefirst.story_mode_clouds.utils.math;

import net.not_thefirst.lib.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class ColorUtils {
    public static int getCloudShaderColor(int layer, int skyColor) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(layer);
        int color = ARGB.WHITE;

        boolean useColor = layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR;
        int customColor  = layerConfiguration.APPEARANCE.LAYER_COLOR;

        if (useColor) {
            color = ARGB.multiply(color, customColor);
        }

        boolean customBrightness = layerConfiguration.APPEARANCE.CUSTOM_BRIGHTNESS;
        float brightness = layerConfiguration.APPEARANCE.BRIGHTNESS;

        if (customBrightness) {
            color = ARGB.multiply(color, brightness);
        }
        else {
            color = ARGB.multiply(color, skyColor);
        }

        return color;
    }
}
