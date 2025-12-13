package net.not_thefirst.story_mode_clouds.compat;

public final class Compat {
    private static ModChecker checker;

    public static void init(ModChecker impl) {
        checker = impl;
    }

    public static boolean hasSodium() {
        return checker.isLoaded("sodium");
    }

    public static boolean hasClothConfig() {
        return checker.isLoaded("cloth-config") ||
            checker.isLoaded("cloth_config") ||
            checker.isLoaded("cloth-config2");
    }

    public static boolean hasModMenu() {
        return checker.isLoaded("modmenu");
    }

    private Compat() {}
}