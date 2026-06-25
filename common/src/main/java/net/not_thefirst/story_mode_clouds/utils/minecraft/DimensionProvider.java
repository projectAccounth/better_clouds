package net.not_thefirst.story_mode_clouds.utils.minecraft;

import net.minecraft.world.level.Level;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;

public class DimensionProvider {
    private DimensionProvider() {}

    public static boolean isNether() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.NETHER;
    }

    public static boolean isEnd() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.END;
    }

    public static boolean isOverworld() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.OVERWORLD;
    }

    public static Dimension getCurrentDimension() {
        if (ClientHelper.getLevel() == null) return null;
        if (isNether()) return Dimension.NETHER;
        if (isEnd()) return Dimension.END;
        return Dimension.OVERWORLD;
    }
}
