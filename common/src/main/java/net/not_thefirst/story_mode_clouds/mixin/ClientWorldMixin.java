package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.multiplayer.ClientLevel;
import net.not_thefirst.story_mode_clouds.utils.CloudColorProvider;

@Mixin(ClientLevel.class)
public class ClientWorldMixin {
    @Inject(method = "tickTime", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        long currentDayTime = ((ClientLevel)(Object)this).getDayTime();
        CloudColorProvider.evaluateColor(currentDayTime);
    }
}
