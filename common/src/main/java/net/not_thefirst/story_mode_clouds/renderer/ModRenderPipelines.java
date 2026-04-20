package net.not_thefirst.story_mode_clouds.renderer;

import java.io.IOException;
import java.io.InputStream;

import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.ProgramManager;
import net.not_thefirst.lib.gl_render_system.shader.ToStreamProvider;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.state.RenderState;
import net.not_thefirst.lib.gl_render_system.state.RenderStateBuilder;
import net.not_thefirst.lib.gl_render_system.state.RenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderRenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderState;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class ModRenderPipelines {
    private ModRenderPipelines() {}

    private static ProgramManager<ResourceLocation> programManager;
    public static GLProgram CLOUDS_SHADER;

    public static ShaderRenderType CUSTOM_POSITION_COLOR;
    public static ShaderRenderType POSITION_COLOR_NO_DEPTH;
    public static ShaderRenderType POSITION_COLOR_DEPTH_ONLY;

    // enabled on render end to reset 
    public static final RenderState DEFAULT_RESET_STATE = new RenderStateBuilder()
        .blend(BlendState.TRANSLUCENT)
        .cull(CullState.CULL)
        .depthTest(DepthTestState.LEQUAL)
        .mask(MaskState.COLOR_DEPTH)
        .build();
    
    public static final IdentifierWrapper CLOUD_SHADER_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/rt_clouds.vert");
    public static final IdentifierWrapper CLOUD_SHADER_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "shaders/rt_clouds.frag");

    public static final IdentifierWrapper CLOUD_SHADER_ID = IdentifierWrapper.of(Initializer.MOD_ID, "clouds");
    public static ProgramManager<ResourceLocation> getProgramManager() {
        return programManager;
    }

    public static void postReload() {
        CLOUDS_SHADER = programManager.get(CLOUD_SHADER_ID.getDelegate());

        if (CLOUDS_SHADER == null)
            throw new IllegalStateException("Shader is null");

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

        LoggerProvider.get().info("Cloud render pipelines reloaded");
    }

    static class ShaderSourceProvider implements ToStreamProvider {
        private ResourceManager manager;

        public ShaderSourceProvider(ResourceManager manager) {
            this.manager = manager;
        }

        @Override
        public InputStream toStream(String path) {
            IdentifierWrapper loc = IdentifierWrapper.tryParse(path);
            try {
                return manager.getResource(loc.getDelegate()).orElseThrow(() -> 
                    new IllegalStateException("Failed to find shader resource: " + loc)
                ).open();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        
    }

    public static void registerCloudPipelines() {
        LoggerProvider.get().info("Reloading cloud render pipelines");
        if (programManager == null) {
            programManager = new ProgramManager<>(
                new ShaderSourceProvider(Minecraft.getInstance().getResourceManager()));
        }

        programManager.register(
            CLOUD_SHADER_ID.getDelegate(),
            CLOUD_SHADER_VERT.getDelegate().toString(),
            CLOUD_SHADER_FRAG.getDelegate().toString()
        );
    }
}