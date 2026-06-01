package net.not_thefirst.story_mode_clouds.utils.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;

public class DimensionProvider {
    private DimensionProvider() {}

    public static boolean isNether() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == Level.NETHER;
    }

    public static boolean isEnd() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == Level.END;
    }

    public static boolean isOverworld() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == Level.OVERWORLD;
    }

    public static Dimension getCurrentDimension() {
        if (Minecraft.getInstance().level == null) return null;
        if (isNether()) return Dimension.NETHER;
        if (isEnd()) return Dimension.END;
        return Dimension.OVERWORLD;
    }
}
