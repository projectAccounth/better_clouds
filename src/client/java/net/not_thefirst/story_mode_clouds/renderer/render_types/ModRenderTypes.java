package net.not_thefirst.story_mode_clouds.renderer.render_types;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.CullStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;

// experimenting around
public class ModRenderTypes {
    public static final RenderStateShard.WriteMaskStateShard COLOR_ONLY_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, false);

    public static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = 
        new RenderStateShard.WriteMaskStateShard(true, true);

    private static TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
		"translucent_transparency",
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
			);
		},
		() -> {
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
		}
	);

    private static CullStateShard CULL = new CullStateShard(true);
    private static CullStateShard NO_CULL = new CullStateShard(false);

    public static RenderType.CompositeRenderType customCloudsFancy = RenderType.create(
        "clouds",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> GameRenderer.getPositionColorShader()))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(CULL)
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
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> GameRenderer.getPositionColorShader()))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOutputState(RenderStateShard.CLOUDS_TARGET)
            .createCompositeState(true)
    );

    public static void initialize() {
    }
}
