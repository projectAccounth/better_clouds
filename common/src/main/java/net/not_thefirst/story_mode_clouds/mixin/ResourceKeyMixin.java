package net.not_thefirst.story_mode_clouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.DimensionProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

@Mixin(ResourceKey.class)
public class ResourceKeyMixin {
    private static final IdentifierWrapper DIMENSION_KEY = IdentifierWrapper.of("minecraft", "dimension");

    @Inject(
        method = {
            "create(Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/ResourceKey;", 
            "create(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;"
        },
        at = @At("HEAD")
    )
    private static void onCreate(Identifier registryId, Identifier valueId, CallbackInfoReturnable<ResourceKey<?>> cir) {
        // registryId is internally a Identifier so the comparison must work, right?
        if (registryId.equals(DIMENSION_KEY.getDelegate())) {
            DimensionProvider.addDimension(IdentifierWrapper.tryWrap(valueId));
            LoggerProvider.get().info("Captured dimension registration: {}", valueId.toString());
        }
    }
}
