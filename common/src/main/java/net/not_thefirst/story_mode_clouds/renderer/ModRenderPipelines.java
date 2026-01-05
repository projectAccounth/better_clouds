package net.not_thefirst.story_mode_clouds.renderer;

import java.io.IOException;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.Initializer;

public class ModRenderPipelines {
    public static final ResourceLocation SHADER_LOCATION = 
        ResourceLocation.tryBuild(Initializer.MOD_ID, "rt_clouds");

    public static ShaderInstance CLOUD_SHADER;

    public static void reloadShaders(ResourceProvider provider) throws IOException {
        CLOUD_SHADER = new ShaderInstance(provider, "rt_clouds", DefaultVertexFormat.POSITION_COLOR);
    }

    public static final RenderStateShard.CullStateShard CULL = 
        new RenderStateShard.CullStateShard(true);

    public static final RenderStateShard.WriteMaskStateShard COLOR_NO_DEPTH = 
        new RenderStateShard.WriteMaskStateShard(true, false);

    public static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH = 
        new RenderStateShard.WriteMaskStateShard(true, true);

    public static final RenderStateShard.WriteMaskStateShard DEPTH_ONLY = 
        new RenderStateShard.WriteMaskStateShard(false, true);

    public static final RenderStateShard.DepthTestStateShard DEPTH_LEQUAL =
        new RenderStateShard.DepthTestStateShard("lequal", 515);

    public static final RenderStateShard.DepthTestStateShard DEPTH_ALWAYS =
        new RenderStateShard.DepthTestStateShard("always", 519);

    public static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public static CompositeRenderType CUSTOM_POSITION_COLOR;
    public static CompositeRenderType POSITION_COLOR_NO_DEPTH;
    public static CompositeRenderType POSITION_COLOR_DEPTH_ONLY;

    public static void registerCloudPipelines() {
        POSITION_COLOR_DEPTH_ONLY = RenderType.create(
            "cloud_tweaks_rt_clouds_cdo",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2097152, // custom 2MB
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> CLOUD_SHADER))
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(DEPTH_LEQUAL)
                .setWriteMaskState(DEPTH_ONLY)
                .createCompositeState(true)
        );

        POSITION_COLOR_NO_DEPTH = RenderType.create(
            "cloud_tweaks_rt_clouds_nd",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2097152,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> CLOUD_SHADER))
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_NO_DEPTH)
                .createCompositeState(true)
        );

        CUSTOM_POSITION_COLOR = RenderType.create(
            "cloud_tweaks_rt_clouds_cpc",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            2097152,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> CLOUD_SHADER))
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(DEPTH_LEQUAL)
                .setWriteMaskState(COLOR_DEPTH)
                .createCompositeState(true)
        );
    }
}
