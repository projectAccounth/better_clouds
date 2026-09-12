package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;

import net.not_thefirst.story_mode_clouds.config.state.CompatStatus;
import net.not_thefirst.story_mode_clouds.config.state.RenderStatus;
import net.not_thefirst.story_mode_clouds.renderer.RendererHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
@Mixin(CloudRenderer.class)
public class CloudRendererMixin {

    // Manual override for F/NF since lambda injection fails
    @Inject(method = "render", at = @At("INVOKE"), cancellable = true)
    public void onRender(CallbackInfo ci) {
        if (!RenderStatus.shouldRender()) 
            return;
        ci.cancel();

        boolean irisShaderPack = CompatStatus.isIrisShaderPackInUse();
        boolean previousBypass = false;
        boolean previousExtendedFormat = false;
        boolean previousIgnorePass = false;

        if (irisShaderPack) {
            previousBypass = ImmediateState.bypass;
            previousExtendedFormat = ImmediateState.renderWithExtendedVertexFormat;
            previousIgnorePass = ImmediateState.temporarilyIgnorePass;

            ImmediateState.bypass = true;
            ImmediateState.renderWithExtendedVertexFormat = false;
            ImmediateState.temporarilyIgnorePass = true;
        }

        try {
            float partialTicks = ClientHelper.getClient().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 camPos = ClientHelper.CameraHelper.getCamera().position();
            RendererHolder.renderCloud(camPos, partialTicks);
        }
        finally {
            if (irisShaderPack) {
                ImmediateState.bypass = previousBypass;
                ImmediateState.renderWithExtendedVertexFormat = previousExtendedFormat;
                ImmediateState.temporarilyIgnorePass = previousIgnorePass;
            }
        }
    }
}
