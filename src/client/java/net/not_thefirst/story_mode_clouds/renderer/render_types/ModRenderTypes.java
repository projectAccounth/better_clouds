package net.not_thefirst.story_mode_clouds.renderer.render_types;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
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
        VertexFormat.Mode.QUADS,
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_CLOUDS_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY) // or your custom
            .setCullState(RenderStateShard.CULL)  // force all faces drawn
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static void initialize() {
        System.out.println(customCloudsFancy);
    }
}
