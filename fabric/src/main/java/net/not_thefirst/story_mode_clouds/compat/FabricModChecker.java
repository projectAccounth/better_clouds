package net.not_thefirst.story_mode_clouds.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricModChecker implements ModChecker {
    @Override
    public boolean isLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}