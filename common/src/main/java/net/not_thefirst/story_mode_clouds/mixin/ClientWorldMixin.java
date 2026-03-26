package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.multiplayer.ClientLevel;
import net.not_thefirst.story_mode_clouds.utils.math.CloudColorProvider;

@Mixin(ClientLevel.class)
public class ClientWorldMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        long currentDayTime = ((ClientLevel)(Object)this).getOverworldClockTime();
        CloudColorProvider.evaluateColor(currentDayTime);
    }
}
