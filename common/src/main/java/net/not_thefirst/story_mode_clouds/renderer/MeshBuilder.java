package net.not_thefirst.story_mode_clouds.renderer;

import net.minecraft.client.CloudStatus;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.compiler.ScriptRegistry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.natives.MeshBuilderRegistry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.natives.MeshTypeBuilder;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.runner.MeshBuilderExecutor;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshType;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeDataCache;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.memory.AssetHandle;

public class MeshBuilder {
    public static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    public static final float CELL_SIZE = CELL_SIZE_IN_BLOCKS;
    public static final float HEIGHT_IN_BLOCKS = 4.0F;
    public static final float HEIGHT = HEIGHT_IN_BLOCKS;

    private static final int MESH_BUILD_TIMEOUT_MS = 4000;

    public static final CloudShape SHAPE     = CloudShape.CUBE;
    public static final PuffMode   PUFF_MODE = PuffMode.SCATTERED;

    public enum CloudShape {
        CUBE,
        CROSS
    }

    public enum PuffMode {
        SCATTERED,
        COMPACT
    }

    @FunctionalInterface
    public static interface MeshCallback {
        AssetHandle<AbstractStaticMesh.Builder<?, ?>> apply(
            AbstractStaticMesh.Builder<?, ?> mesh, 
            MeshType meshType, 
            int cx, int cz, 
            int currentLayer,
            CustomCloudRenderer.LayerState state,
            int colorModifier);
    }

    public static final record MeshBuilderParameters(
        CustomCloudRenderer.LayerState state,
        int baseCx, int baseCz,
        CloudStatus status,
        int currentLayer,
        int colorModifier) {}

    public static AssetHandle<AbstractStaticMesh.Builder<?, ?>> buildMesh(
        AbstractStaticMesh.Builder<?, ?> mesh, 
        MeshType meshType, 
        int cx, int cz, int currentLayer,
        CustomCloudRenderer.LayerState state,
        int colorModifier
    ) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        if (meshType.usesNativeBuilder()) {
            try {
                MeshTypeBuilder builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);

                return MeshBuilderExecutor.wrapTask(
                    mesh, (bb) -> {
                        builder.build(
                            mesh, 
                            state, 
                            cx, cz,
                            currentLayer, 
                            colorModifier);
                    }, MESH_BUILD_TIMEOUT_MS, true);
            }
            catch (Exception exception) {
                LoggerProvider.get().error("Exception getting native mesh builder: {}", exception);
            }            
        }
        else {
            MeshTypeData data = MeshTypeDataCache.get(meshType.getName());
            if (data == null) {
                throw new IllegalStateException("MeshTypeData does not exist even though MeshType do");
            }

            String scriptId = "base/" + data.generatorConfig().baseScriptName();

            LoggerProvider.get().info("Building with custom builder id {}", scriptId);
            var script = ScriptRegistry.getCachedScript(scriptId);

            if (script != null) {
                return MeshBuilderExecutor.submitTask(
                    script, 
                    mesh, 
                    layerConfiguration, 
                    state.texture(), 
                    cx, cz, 
                    MESH_BUILD_TIMEOUT_MS, true);
            }
            else {
                LoggerProvider.get().error("Exception getting custom mesh builder {}", scriptId);
            }
        }
        return new AssetHandle<>(null);
    }

    public static AssetHandle<AbstractStaticMesh.Builder<?, ?>> buildOutlineMesh(
        AbstractStaticMesh.Builder<?, ?> mesh, 
        MeshType meshType, 
        int cx, int cz,  int currentLayer,
        CustomCloudRenderer.LayerState state,
        int colorModifier
    ) {
        CloudsConfiguration.LayerConfiguration layerConfiguration = 
                CloudsConfiguration.getInstance().getLayer(currentLayer);

        if (meshType.usesNativeBuilder()) {
            try {
                MeshTypeBuilder builder = MeshBuilderRegistry.getInstance().getObject(layerConfiguration.MODE);

                return MeshBuilderExecutor.wrapTask(
                    mesh, (bb) -> {
                        builder.buildOutline(
                            mesh, 
                            state, 
                            cx, cz,
                            currentLayer, 
                            colorModifier);
                    }, MESH_BUILD_TIMEOUT_MS, true);
            }
            catch (Exception exception) {
                LoggerProvider.get().error("Exception getting native mesh builder: {}", exception);
            }            
        }
        else {
            MeshTypeData data = MeshTypeDataCache.get(meshType.getName());
            if (data == null) {
                throw new IllegalStateException("MeshTypeData does not exist even though MeshType do");
            }

            String scriptId = "outline/" + data.generatorConfig().outlineScriptName();

            LoggerProvider.get().info("Building with custom builder id {}", scriptId);
            var script = ScriptRegistry.getCachedScript(scriptId);

            if (script != null) {
                return MeshBuilderExecutor.submitTask(
                    script, 
                    mesh, 
                    layerConfiguration, 
                    state.texture(), 
                    cx, cz, 
                    MESH_BUILD_TIMEOUT_MS, true);
            }
            else {
                LoggerProvider.get().error("Exception getting custom mesh builder {}", scriptId);
            }
        }
        return new AssetHandle<>(null);
    }
}
