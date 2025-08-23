package net.not_thefirst.story_mode_clouds.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class Compat {
    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    public static boolean hasSodium() { return SODIUM; }
    private Compat() {}
}