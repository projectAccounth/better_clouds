package net.not_thefirst.story_mode_clouds.mixin;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {

    private static final float FADE_RANGE = 3.0F; // how tall the fade band is
    private static final float CLOUD_Y_SCALE = 1.5f;
    private static final boolean APPEARS_SHADED = false;
    private static final boolean USES_CUSTOM_ALPHA = true;
    private static final float BRIGHTNESS = 1.0f;
    private static final float BASE_ALPHA = 0.4f;

    @Redirect(
        method = "buildCloudCellFancy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumer;color(I)Lnet/minecraft/client/render/VertexConsumer;"
        )
    )
    private VertexConsumer applyVerticalFade(VertexConsumer builder, int originalColor) {
        float r = 0, g = 0, b = 0;

        if (!APPEARS_SHADED) {
            r = g = b = BRIGHTNESS;
        } 
        else {
            r = ColorHelper.getRedFloat(originalColor);
            g = ColorHelper.getGreenFloat(originalColor);
            b = ColorHelper.getBlueFloat(originalColor);
        }
        float a = USES_CUSTOM_ALPHA ? BASE_ALPHA : ColorHelper.getAlphaFloat(originalColor);

        if (isAboveClouds()) {
            return builder.color(r, g, b, a); // no fade
        }

        float y = lastVertexY();
        float fade = MathHelper.clamp(1.0F - (Math.abs(y) / FADE_RANGE), 0.0F, 1.0F);

        return builder.color(r, g, b, a * fade);
    }

    // Capture vertex position before color() is called
    private static final ThreadLocal<Float> VERTEX_Y = ThreadLocal.withInitial(() -> 0f);

    @Redirect(
        method = "buildCloudCellFancy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/BufferBuilder;vertex(FFF)Lnet/minecraft/client/render/VertexConsumer;"
        )
    )
    private VertexConsumer scaleCloudY(BufferBuilder builder, float x, float y, float z) {
        float adjustedHeight = y * CLOUD_Y_SCALE;
        VERTEX_Y.set(adjustedHeight);
        return builder.vertex(x, adjustedHeight, z);
    }

    private float lastVertexY() {
        return VERTEX_Y.get();
    }

    private static final ThreadLocal<Object> CURRENT_VIEWMODE = new ThreadLocal<>();

    @Inject(
        method = "buildCloudCellFancy",
        at = @At("HEAD")
    )
    private void captureViewMode(@Coerce Object viewMode, BufferBuilder builder,
                                 int bottomColor, int topColor,
                                 int northSouthColor, int eastWestColor,
                                 int x, int z, long cell,
                                 CallbackInfo ci) {
        CURRENT_VIEWMODE.set(viewMode);
    }

    @Inject(
        method = "buildCloudCellFancy",
        at = @At("RETURN")
    )
    private void clearViewMode(CallbackInfo ci) {
        CURRENT_VIEWMODE.remove();
    }

    private boolean isAboveClouds() {
        return CURRENT_VIEWMODE.get() != null
            && CURRENT_VIEWMODE.get().toString().equals("ABOVE_CLOUDS");
    }
}