package net.not_thefirst.story_mode_clouds.config.state;

import net.irisshaders.iris.api.v0.IrisApi;
import net.not_thefirst.story_mode_clouds.compat.Compat;

public class CompatStatus {
    private CompatStatus() {}

    public static boolean isIrisPresent() {
        return Compat.hasIris();
    }

    public static boolean isIrisShaderPackInUse() {
        return isIrisPresent() && IrisApi.getInstance().isShaderPackInUse();
    }
}