package net.not_thefirst.story_mode_clouds.renderer.render_types;

import java.io.IOException;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.phys.Vec3;
import net.not_thefirst.story_mode_clouds.utils.ARGB;
import net.minecraft.client.renderer.RenderType.CompositeState;

// experimenting around
public class ModRenderTypes {
    public static final RenderStateShard.WriteMaskStateShard COLOR_ONLY_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, false);

    public static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, true);
        
    @Nullable
    public static ShaderInstance CLOUDS_SHADER;
    private static Uniform uModelOffset;

    public static void reloadShaders(ResourceProvider provider) throws IOException {
        // Load your shader JSON from assets/<modid>/shaders/core/custom_clouds.json
        ModRenderTypes.CLOUDS_SHADER = new ShaderInstance(provider, "custom_clouds", DefaultVertexFormat.POSITION_COLOR);
    }

    public static RenderType.CompositeRenderType customCloudsFancy = RenderType.create(
        "clouds",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModRenderTypes.CLOUDS_SHADER))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY) // or your custom
            .setCullState(RenderStateShard.CULL)  // force all faces drawn
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static RenderType.CompositeRenderType customCloudsFast = RenderType.create(
        "clouds",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModRenderTypes.CLOUDS_SHADER))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY) // or your custom
            .setCullState(RenderStateShard.NO_CULL)  // force all faces drawn
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static void initialize() {
        System.out.println(customCloudsFancy);
    }
}
