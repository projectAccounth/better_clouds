package net.not_thefirst.story_mode_clouds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderProgram;
import net.not_thefirst.story_mode_clouds.renderer.ModRenderPipelines;

@Mixin(CoreShaders.class)
public abstract class CoreShadersMixin {

    @Shadow
    private static List<ShaderProgram> PROGRAMS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onClinit(CallbackInfo ci) {
        PROGRAMS.add(ModRenderPipelines.CLOUD_SHADER);
    }
}