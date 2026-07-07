package net.not_thefirst.story_mode_clouds.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.dimension.DimensionType;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

@Mixin(DimensionType.class)
public class DimensionMixin {
    @Inject(method = "cloudHeight()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void onGetCloudHeight(CallbackInfoReturnable<Optional<?>> cir) {
        if (CloudsConfiguration.getInstance().CLOUDS_RENDERED) {
            cir.setReturnValue(Optional.of(0));
        }
    }
}
