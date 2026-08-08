package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.not_thefirst.lib.gl_render_system.alt.PipelineManager;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.Starter;
import net.not_thefirst.story_mode_clouds.renderer.pipelines.Blaze3DPipelines;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

@Mixin(Minecraft.class)
public class ClientMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Blaze3DPipelines.init();
        PipelineManager.setPipelineProvider(Blaze3DPipelines.BLAZE3D);
        PipelineManager.getInstance().init();

        LoggerProvider.get().info("Initialized pipelines");

        Starter.initialize();

        LoggerProvider.get().info("Successfully initialized mesh builder API");
    }
}
