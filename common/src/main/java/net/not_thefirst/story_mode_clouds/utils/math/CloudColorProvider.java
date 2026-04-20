package net.not_thefirst.story_mode_clouds.utils.math;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.WeatherColorConfig;
import net.not_thefirst.story_mode_clouds.utils.interp.world.NumberSequence;

public class CloudColorProvider {
    public static final int DAY_DURATION = 24000;

    public record WeatherState(float rain, float thunder) {
        public static WeatherState from(ClientLevel level, float partialTicks) {
            return new WeatherState(
                level.getRainLevel(partialTicks),
                level.getThunderLevel(partialTicks)
            );
        }
    }

    private static double[] customColor = new double[] { 1.0, 1.0, 1.0 };
    private static NumberSequence colorSequence = new NumberSequence(DAY_DURATION);

    public static int getVanillaSkyColor(float partialTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        Vec3 color = level.getCloudColor(partialTicks);
        return ARGB.colorFromFloat(1.0f, (float) color.x, (float) color.y, (float) color.z);
    }


    public static int getVanillaSkyColor() {
        return getVanillaSkyColor(1.0F);
    }

    public static void setCustomColorSequence(NumberSequence sequence) {
        colorSequence = sequence;
    }

    public static NumberSequence getCurrentSequence() {
        return colorSequence;
    }

    public static void putEntry(double time, double r, double g, double b) {
        colorSequence.addKeypoint(time, r, g, b);
    }

    public static void evaluateColor(long timeOfDay) {
        timeOfDay = timeOfDay % DAY_DURATION;
        customColor = colorSequence.evaluate(timeOfDay);
    }

    public static int getCustomCloudColor() {
        return ARGB.colorFromFloat(1.0f,
            (float)customColor[0],
            (float)customColor[1],
            (float)customColor[2]
        );
    }

    private static int applyWeatherTintCustom(
        int baseColor,
        float rain,
        float thunder,
        WeatherColorConfig config
    ) {
        float r = ARGB.redFloat(baseColor);
        float g = ARGB.greenFloat(baseColor);
        float b = ARGB.blueFloat(baseColor);

        if (rain > 0.0f) {
            float rr = ARGB.redFloat(config.rainColor);
            float rg = ARGB.greenFloat(config.rainColor);
            float rb = ARGB.blueFloat(config.rainColor);

            float t = rain * config.rainStrength;
            r = Mth.lerp(t, r, rr);
            g = Mth.lerp(t, g, rg);
            b = Mth.lerp(t, b, rb);
        }

        if (thunder > 0.0f) {
            float tr = ARGB.redFloat(config.thunderColor);
            float tg = ARGB.greenFloat(config.thunderColor);
            float tb = ARGB.blueFloat(config.thunderColor);

            float t = thunder * config.thunderStrength;
            r = Mth.lerp(t, r, tr);
            g = Mth.lerp(t, g, tg);
            b = Mth.lerp(t, b, tb);
        }

        return ARGB.colorFromFloat(1.0f, r, g, b);
    }



    public static int getCloudColor(WeatherState weather) {
        CloudsConfiguration.CloudColorProviderMode mode =
            CloudsConfiguration.getInstance().COLOR_MODE;

        if (mode == CloudsConfiguration.CloudColorProviderMode.VANILLA) {
            return getVanillaSkyColor();
        }

        int baseColor = getCustomCloudColor();
        WeatherColorConfig cfg = CloudsConfiguration.getInstance().WEATHER_COLOR;

        return applyWeatherTintCustom(
            baseColor,
            weather.rain(),
            weather.thunder(),
            cfg
        );
    }

}