package net.not_thefirst.story_mode_clouds.utils.math;

import net.minecraft.util.Mth;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

public class ColorUtils {
    public static int recolor(int color, int currentLayer, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration =
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        if (!layerConfiguration.IS_ENABLED) {
            return color;
        }

        boolean shaded   = layerConfiguration.APPEARANCE.SHADING_ENABLED;
        boolean useAlpha = layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA;
        boolean useColor = layerConfiguration.APPEARANCE.USES_CUSTOM_COLOR;

        float baseAlpha = useAlpha
                ? layerConfiguration.APPEARANCE.BASE_ALPHA / 255.0f
                : ARGB.alphaFloat(color);

        int customColor = layerConfiguration.APPEARANCE.LAYER_COLOR;

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        }

        return ARGB.colorFromFloat(baseAlpha, r, g, b);
    }

    public static int recolor(int color, float vertexY, float relY, int currentLayer, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration =
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        if (!layerConfiguration.IS_ENABLED) {
            return color;
        }

        boolean useAlpha = layerConfiguration.APPEARANCE.USES_CUSTOM_ALPHA;

        float baseAlpha = useAlpha
                ? layerConfiguration.APPEARANCE.BASE_ALPHA / 255.0f
                : ARGB.alphaFloat(color);

        float fadeAlpha = layerConfiguration.FADE.FADE_ALPHA / 255.0f;

        float r = 1;
        float g = 1;
        float b = 1;

        if (!layerConfiguration.FADE.FADE_ENABLED) {
            return ARGB.colorFromFloat(baseAlpha, r, g, b);
        }

        float cloudHeight = 4 * layerConfiguration.APPEARANCE.CLOUD_Y_SCALE;
        float normalizedY = Mth.clamp(vertexY / cloudHeight, 0.0f, 1.0f);

        float transitionRange = layerConfiguration.FADE.TRANSITION_RANGE;
        float dir = Mth.clamp(relY / transitionRange, -1.0f, 1.0f);

        float fadeBelow = Mth.lerp(normalizedY, 1.0f, fadeAlpha);
        float fadeAbove = Mth.lerp(1.0f - normalizedY, 1.0f, fadeAlpha);

        float mix = (dir + 1.0f) / 2.0f;
        float fade = Mth.lerp(mix, fadeBelow, fadeAbove);

        float finalAlpha = baseAlpha * (1.0f - fade);

        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }

    public static int getCloudShaderColor(int layer, int skyColor) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(layer);
        int color = ARGB.WHITE;

        if (!layerConfiguration.IS_ENABLED) {
            return ARGB.multiply(color, skyColor);
        }

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
