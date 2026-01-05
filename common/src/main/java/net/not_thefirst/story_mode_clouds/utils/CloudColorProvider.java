package net.not_thefirst.story_mode_clouds.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.interp.world.NumberSequence;
import net.not_thefirst.story_mode_clouds.utils.math.ARGB;

public class CloudColorProvider {
    public static int DAY_DURATION = 24000;

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

    public static int getCloudColor() {
        return CloudsConfiguration.INSTANCE.COLOR_MODE 
            == CloudsConfiguration.CloudColorProviderMode.VANILLA
            ? getVanillaSkyColor() : getCustomCloudColor();
    }
}
