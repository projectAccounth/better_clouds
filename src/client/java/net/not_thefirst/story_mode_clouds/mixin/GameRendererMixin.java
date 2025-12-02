package net.not_thefirst.story_mode_clouds.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.not_thefirst.story_mode_clouds.renderer.render_types.ModRenderTypes;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "reloadShaders", at = @At("TAIL"))
    private void afterReloadShaders(ResourceProvider provider, CallbackInfo ci) throws IOException {
        // ModRenderTypes.reloadShaders(provider);
    }
}
