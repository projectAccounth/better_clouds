package net.not_thefirst.story_mode_clouds.renderer;

import java.io.IOException;
import java.io.InputStream;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.ProgramManager;
import net.not_thefirst.lib.gl_render_system.shader.ToStreamProvider;
import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.CullState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.state.MaskState;
import net.not_thefirst.lib.gl_render_system.state.RenderStateBuilder;
import net.not_thefirst.lib.gl_render_system.state.RenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderRenderType;
import net.not_thefirst.lib.gl_render_system.state.ShaderState;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.IdentifierWrapper;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;

public class ModRenderPipelines {
    private ModRenderPipelines() {}

    private static ProgramManager<ResourceLocation> programManager;
    public static GLProgram CLOUDS_SHADER;

    @SuppressWarnings("unused")
    private static ShaderRenderType POSITION_COLOR_DEPTH_ONLY;
    private static ShaderRenderType POSITION_COLOR_NO_DEPTH;
    private static ShaderRenderType CUSTOM_POSITION_COLOR;

    public static final IdentifierWrapper CLOUD_SHADER_VERT = 
        IdentifierWrapper.of(Initializer.MOD_ID, "core/rt_clouds");
    public static final IdentifierWrapper CLOUD_SHADER_FRAG = 
        IdentifierWrapper.of(Initializer.MOD_ID, "core/rt_clouds");

    public static final IdentifierWrapper CLOUD_SHADER_ID = IdentifierWrapper.of(Initializer.MOD_ID, "clouds");
    public static final IdentifierWrapper CLOUD_DEPTH_SHADER_ID = IdentifierWrapper.of(Initializer.MOD_ID, "clouds_depth");
    public static final IdentifierWrapper CLOUD_COLOR_SHADER_ID = IdentifierWrapper.of(Initializer.MOD_ID, "clouds_color");

    private static RenderPipeline.Builder applyCloudDefaults(RenderPipeline.Builder builder) {
        builder.withUniform("u_ProjMat", UniformType.MATRIX4X4)
            .withUniform("u_ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("u_ModelOffset", UniformType.VEC4)
            .withUniform("u_CloudsInfo0", UniformType.VEC4)
            .withUniform("u_CloudsInfo1", UniformType.VEC4)
            .withUniform("u_CloudColor", UniformType.VEC4)
            .withUniform("u_CameraPos", UniformType.VEC4)
            .withUniform("u_LightMeta", UniformType.VEC4)
            .withUniform("u_FadeToColor", UniformType.VEC4)
            .withUniform("u_CloudHeight", UniformType.VEC2);

        for (int i = 0; i < CloudsConfiguration.LightingParameters.MAX_LIGHT_COUNT; i++) {
            builder.withUniform("u_LightPos[" + i + "]", UniformType.VEC4);
            builder.withUniform("u_LightColor[" + i + "]", UniformType.VEC4);
        }
        return builder;
    }

    static final RenderPipeline.Snippet CUSTOM_CLOUDS = applyCloudDefaults(
        RenderPipeline.builder()
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(CLOUD_SHADER_VERT.getDelegate())
            .withFragmentShader(CLOUD_SHADER_FRAG.getDelegate())
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withCull(true)
    ).buildSnippet();

    public static final RenderPipeline NORMAL_CLOUDS = 
        RenderPipeline.builder(CUSTOM_CLOUDS)
            .withLocation(CLOUD_SHADER_ID.getDelegate())
            .withColorWrite(true)
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline DEPTH_ONLY_CLOUDS =
        RenderPipeline.builder(CUSTOM_CLOUDS)
            .withLocation(CLOUD_DEPTH_SHADER_ID.getDelegate())
            .withColorWrite(false)
            .withDepthWrite(true)
            .build();

    public static final RenderPipeline COLOR_ONLY_CLOUDS =
        RenderPipeline.builder(CUSTOM_CLOUDS)
            .withLocation(CLOUD_COLOR_SHADER_ID.getDelegate())
            .withDepthWrite(false)
            .withColorWrite(true)
            .build();


    // reserved for older versions of Minecraft while backporting
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