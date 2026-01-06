package net.not_thefirst.story_mode_clouds.renderer;

import org.lwjgl.opengl.GL20;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.renderer.shader.ProgramManager;
import net.minecraft.client.renderer.RenderType;

public class ModRenderPipelines {
    private static ProgramManager programManager;

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

    public static RenderType CUSTOM_POSITION_COLOR;
    public static RenderType POSITION_COLOR_NO_DEPTH;
    public static RenderType POSITION_COLOR_DEPTH_ONLY;

    public static ResourceLocation CLOUD_SHADER_VERT = 
        new ResourceLocation(Initializer.MOD_ID, "shaders/rt_clouds.vert");
    public static ResourceLocation CLOUD_SHADER_FRAG = 
        new ResourceLocation(Initializer.MOD_ID, "shaders/rt_clouds.frag");

    public static final ResourceLocation CLOUD_SHADER_ID = new ResourceLocation(Initializer.MOD_ID, "clouds");
    public static ProgramManager getProgramManager() {
        return programManager;
    }

    public static VertexFormatElement POSITION = new VertexFormatElement(
        0,
        VertexFormatElement.Type.FLOAT,
        VertexFormatElement.Usage.GENERIC,
        3
    );

    public static VertexFormatElement COLOR = new VertexFormatElement(
        1,
        VertexFormatElement.Type.FLOAT,
        VertexFormatElement.Usage.GENERIC,
        4
    );

    public static VertexFormat POSITION_COLOR_CUSTOM =
        new VertexFormat(ImmutableList.of(POSITION, COLOR));

    public static void registerCloudPipelines() {
        POSITION_COLOR_DEPTH_ONLY = RenderType.create(
            "cloud_tweaks_rt_clouds_cdo",
            POSITION_COLOR_CUSTOM,
            GL20.GL_QUADS,
            2097152, // 2MB size 
            false,
            false,
            RenderType.CompositeState.builder()
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(DEPTH_LEQUAL)
                .setWriteMaskState(DEPTH_ONLY)
                .createCompositeState(true)
        );

        POSITION_COLOR_NO_DEPTH = RenderType.create(
            "cloud_tweaks_rt_clouds_nd",
            POSITION_COLOR_CUSTOM,
            GL20.GL_QUADS,
            2097152,
            false,
            false,
            RenderType.CompositeState.builder()
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_NO_DEPTH)
                .createCompositeState(true)
        );

        CUSTOM_POSITION_COLOR = RenderType.create(
            "cloud_tweaks_rt_clouds_cpc",
            POSITION_COLOR_CUSTOM,
            GL20.GL_QUADS,
            2097152,
            false,
            false,
            RenderType.CompositeState.builder()
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(DEPTH_LEQUAL)
                .setWriteMaskState(COLOR_DEPTH)
                .createCompositeState(true)
        );

        if (programManager == null) {
            programManager = new ProgramManager(Minecraft.getInstance().getResourceManager());
        }

        programManager.register(
            CLOUD_SHADER_ID,
            CLOUD_SHADER_VERT,
            CLOUD_SHADER_FRAG
        );
    }
}
