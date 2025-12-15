package net.not_thefirst.story_mode_clouds.utils;

import net.minecraft.util.Mth;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.renderer.CustomCloudRenderer.RelativeCameraPos;

public class ColorUtils {
    public static int recolor(int color, int currentLayer, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        if (!layerConfiguration.IS_ENABLED) {
            return ARGB.multiply(color, skyColor);
        }

        boolean shaded   = layerConfiguration.APPEARS_SHADED;
        boolean useAlpha = layerConfiguration.USES_CUSTOM_ALPHA;
        boolean useColor = layerConfiguration.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? layerConfiguration.BASE_ALPHA / 255.0f : ARGB.alphaFloat(color);
        int customColor  = layerConfiguration.LAYER_COLOR;

        float r = ARGB.redFloat(color);
        float g = ARGB.greenFloat(color);
        float b = ARGB.blueFloat(color);

        // Apply custom tinting
        if (!shaded && useColor) {
            r = ARGB.redFloat(customColor);
            g = ARGB.greenFloat(customColor);
            b = ARGB.blueFloat(customColor);
        } else if (useColor) {
            r *= ARGB.redFloat(customColor);
            g *= ARGB.greenFloat(customColor);
            b *= ARGB.blueFloat(customColor);
        }

        if (!layerConfiguration.CUSTOM_BRIGHTNESS) {
            float cloudR = ARGB.redFloat(skyColor);
            float cloudG = ARGB.greenFloat(skyColor);
            float cloudB = ARGB.blueFloat(skyColor);

            r *= cloudR;
            g *= cloudG;
            b *= cloudB;
        }

        return ARGB.colorFromFloat(baseAlpha, r, g, b);
    }

    public static int recolor(int color, float vertexY, RelativeCameraPos pos, float relY, int currentLayer, int skyColor) {

        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.INSTANCE.getLayer(currentLayer);

        if (!layerConfiguration.IS_ENABLED) {
            return ARGB.multiply(color, skyColor);
        }

        boolean shaded   = layerConfiguration.APPEARS_SHADED;
        boolean useAlpha = layerConfiguration.USES_CUSTOM_ALPHA;
        boolean useColor = layerConfiguration.USES_CUSTOM_COLOR;

        float baseAlpha  = useAlpha ? layerConfiguration.BASE_ALPHA / 255.0f : ARGB.alphaFloat(color);
        float fadeAlpha  = layerConfiguration.FADE_ALPHA / 255.0f;
        int customColor  = layerConfiguration.LAYER_COLOR;

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
        } else if (!shaded) {
            r = g = b = 1.0f;
        }

        if (!layerConfiguration.CUSTOM_BRIGHTNESS) {
            float cloudR = ARGB.redFloat(skyColor);
            float cloudG = ARGB.greenFloat(skyColor);
            float cloudB = ARGB.blueFloat(skyColor);

            r *= cloudR;
            g *= cloudG;
            b *= cloudB;
        }

        if (!layerConfiguration.FADE_ENABLED) {
            return ARGB.colorFromFloat(baseAlpha, r, g, b);
        }

        float cloudHeight = 4 * layerConfiguration.CLOUD_Y_SCALE;
        float normalizedY = Mth.clamp(vertexY / cloudHeight, 0.0f, 1.0f);

        float transitionRange = layerConfiguration.TRANSITION_RANGE;
        float dir = Mth.clamp(relY / transitionRange, -1.0f, 1.0f);

        float fadeBelow = Mth.lerp(normalizedY, 1.0f, fadeAlpha);
        float fadeAbove = Mth.lerp(1.0f - normalizedY, 1.0f, fadeAlpha);

        float mix = (dir + 1.0f) / 2.0f; // [-1..1] → [0..1]
        float fade = Mth.lerp(mix, fadeBelow, fadeAbove);

        float finalAlpha = baseAlpha * (1.0f - fade);
        
        return ARGB.colorFromFloat(finalAlpha, r, g, b);
    }
}
