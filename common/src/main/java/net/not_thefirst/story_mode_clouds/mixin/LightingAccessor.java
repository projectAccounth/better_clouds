package net.not_thefirst.story_mode_clouds.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.Lighting;

@Mixin(Lighting.class)
public interface LightingAccessor {
	@Accessor("DIFFUSE_LIGHT_0")
	public static Vector3f getLight0Direction() {
		throw new UnsupportedOperationException();
	}

	@Accessor("DIFFUSE_LIGHT_1")
	public static Vector3f getLight1Direction() {
		throw new UnsupportedOperationException();
	}
}
