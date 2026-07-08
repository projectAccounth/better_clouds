package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.story_mode_clouds.renderer.pipelines.GLPipelines;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

@Mixin(Minecraft.class)
public class ClientMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        GLPipelines.init();
        PipelineManager.setPipelineProvider(PipelineManager.PipelineProvider.DEFAULT);
        PipelineManager.getInstance().init();

        LoggerProvider.get().info("Initialized pipelines");
    }
}
