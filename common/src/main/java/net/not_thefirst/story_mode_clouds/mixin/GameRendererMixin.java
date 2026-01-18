package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"), cancellable = true)
    protected void reloadShaders(ResourceManager manager, CallbackInfo ci) {
        ModRenderPipelines.getProgramManager().setResourceManager(manager);
        ModRenderPipelines.getProgramManager().reloadAll();
        ModRenderPipelines.postReload();
        LoggerProvider.get().info("Successfully initialized shaders!");
    }
}
