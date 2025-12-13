package net.not_thefirst.story_mode_clouds.compat;

import net.minecraftforge.fml.ModList;

public final class ForgeModChecker implements ModChecker {
    @Override
    public boolean isLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}