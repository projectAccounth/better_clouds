package net.not_thefirst.story_mode_clouds.utils.minecraft;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;

public class DimensionProvider {
    private static final Set<IdentifierWrapper> dimensions = new HashSet<>();

    private DimensionProvider() {}

    public static void addDimension(IdentifierWrapper id) {
        dimensions.add(id);
    }

    /**
     * Gets all dimensions that have been registered in the game to the point of this call
     * @return A set of all dimensions that have been registered in the game to the point of this call
     */
    public static Set<IdentifierWrapper> getAllDimensions() {
        return dimensions;
    }

    public static boolean isNether() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.NETHER;
    }

    public static boolean isEnd() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.END;
    }

    public static boolean isOverworld() {
        return ClientHelper.getLevel() != null && ClientHelper.getLevel().dimension() == Level.OVERWORLD;
    }

    private static ResourceKey<?> getCurrentDimensionKey() {
        if (ClientHelper.getLevel() == null) return null;
        return ClientHelper.getLevel().dimension();
    }

    public static Dimension getCurrentDimension() {
        if (ClientHelper.getLevel() == null) return null;
        ResourceKey<?> key = getCurrentDimensionKey();

        if (key == null) return null;

        Dimension dim = Dimension.get(key.identifier().toString());
        if (dim == null) {
            dim = Dimension.tryRegister(IdentifierWrapper.tryWrap(key.identifier()));
        }
        return dim;
    }
}
