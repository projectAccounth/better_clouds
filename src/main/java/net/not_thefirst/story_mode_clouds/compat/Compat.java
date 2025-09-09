package net.not_thefirst.story_mode_clouds.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class Compat {
    public static boolean hasSodium() { 
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    public static boolean hasClothConfig() {
        return FabricLoader.getInstance().isModLoaded("cloth-config2");
    }

    public static boolean hasModMenu() {
        return FabricLoader.getInstance().isModLoaded("modmenu");
    }

    private Compat() {}
}