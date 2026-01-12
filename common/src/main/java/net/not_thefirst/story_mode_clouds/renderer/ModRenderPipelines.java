package net.not_thefirst.story_mode_clouds.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.GLProgram;
import net.not_thefirst.story_mode_clouds.renderer.render_system.shader.ProgramManager;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.BlendState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.CullState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.DepthTestState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.MaskState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.RenderStateBuilder;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.RenderType;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.ShadeState;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.ShaderRenderType;
import net.not_thefirst.story_mode_clouds.renderer.render_system.state.ShaderState;

public class ModRenderPipelines {
    private static ProgramManager programManager;
    public static GLProgram CLOUDS_SHADER;

    public static ShaderRenderType CUSTOM_POSITION_COLOR;
    public static ShaderRenderType POSITION_COLOR_NO_DEPTH;
    public static ShaderRenderType POSITION_COLOR_DEPTH_ONLY;
    public static RenderType CLOUDS_GENERAL = new RenderType(
        "cloud_tweaks_clouds_general",
        new RenderStateBuilder()
            .blend(BlendState.TRANSLUCENT)
            .cull(CullState.CULL)
            .mask(MaskState.COLOR_DEPTH)
            .depthTest(DepthTestState.LEQUAL)
            .shade(ShadeState.SMOOTH)
            .build()
    );

    public static final ResourceLocation CLOUD_SHADER_VERT = 
        new ResourceLocation(Initializer.MOD_ID, "shaders/rt_clouds.vert");
    public static final ResourceLocation CLOUD_SHADER_FRAG = 
        new ResourceLocation(Initializer.MOD_ID, "shaders/rt_clouds.frag");

    public static final ResourceLocation CLOUD_SHADER_ID = new ResourceLocation(Initializer.MOD_ID, "clouds");
    public static ProgramManager getProgramManager() {
        return programManager;
    }

    public static void postReload() {
        CLOUDS_SHADER = programManager.get(CLOUD_SHADER_ID);

        CLOUDS_SHADER.bindUniformBlock("Transforms", 0);
        CLOUDS_SHADER.bindUniformBlock("CloudInfo", 1);
        CLOUDS_SHADER.bindUniformBlock("Lighting", 2);
        CLOUDS_SHADER.bindUniformBlock("Camera", 3);

        POSITION_COLOR_DEPTH_ONLY = new ShaderRenderType(
            "cloud_tweaks_rt_clouds_cdo",
            new ShaderState(CLOUDS_SHADER),
            new RenderStateBuilder()
                .blend(BlendState.TRANSLUCENT)
                .cull(CullState.CULL)
                .mask(MaskState.DEPTH_ONLY)
                .build()
        );

        POSITION_COLOR_NO_DEPTH = new ShaderRenderType(
            "cloud_tweaks_rt_clouds_nd",
            new ShaderState(CLOUDS_SHADER),
            new RenderStateBuilder()
                .blend(BlendState.TRANSLUCENT)
                .cull(CullState.CULL)
                .mask(MaskState.COLOR_NO_DEPTH)
                .build()
        );

        CUSTOM_POSITION_COLOR = new ShaderRenderType(
            "cloud_tweaks_rt_clouds_cpc",
            new ShaderState(CLOUDS_SHADER),
            new RenderStateBuilder()
                .blend(BlendState.TRANSLUCENT)
                .cull(CullState.CULL)
                .mask(MaskState.COLOR_DEPTH)
                .build()
        );
    }

    public static void registerCloudPipelines() {
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
