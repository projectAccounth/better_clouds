package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.story_mode_clouds.utils.rendering.gl.GLResourceHandler;
@Mixin(Minecraft.class)
public class ClientMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onResourcePacksReloaded(CallbackInfo ci) {
        GLResourceHandler.init(Minecraft.getInstance().getResourceManager());
        GLResourceHandler.getProgramManager().reloadAll();
        PipelineManager.getInstance().init();
    }
}
