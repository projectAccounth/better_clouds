package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

@Mixin(DimensionSpecialEffects.class)
public class DimensionMixin {
    @Inject(method = "getCloudHeight()F", at = @At("HEAD"), cancellable = true)
    private void onGetCloudHeight(CallbackInfoReturnable<Float> cir) {
        if (CloudsConfiguration.getInstance().CLOUDS_RENDERED) {
            cir.setReturnValue(0.0f);
        }
    }
}
