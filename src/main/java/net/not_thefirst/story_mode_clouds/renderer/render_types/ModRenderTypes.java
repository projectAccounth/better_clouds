package net.not_thefirst.story_mode_clouds.renderer.render_types;

import java.io.IOException;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShadeModelStateShard;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.minecraft.client.renderer.RenderType.CompositeState;

// experimenting around
public class ModRenderTypes {
    public static final RenderStateShard.WriteMaskStateShard COLOR_ONLY_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, false);

    public static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, true);

    public static RenderType.CompositeRenderType customCloudsFancy = RenderType.create(
        "clouds",
        DefaultVertexFormat.POSITION_COLOR,
      7,
        262144,
        false,
        false,
        CompositeState.builder()
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static RenderType.CompositeRenderType customCloudsFast = RenderType.create(
        "clouds",
        DefaultVertexFormat.POSITION_COLOR,
        7,
        262144,
        false,
        false,
        CompositeState.builder()
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static void initialize() {
    }
}
