package net.not_thefirst.story_mode_clouds.mesh_builder_api;

import net.minecraft.server.packs.resources.ResourceManager;
import net.not_thefirst.story_mode_clouds.Initializer;
import net.not_thefirst.story_mode_clouds.config.resources.ResourceHandler;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.compiler.ScriptRegistry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshType;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeDataCache;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeRegistry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.GeneratorType;
import net.not_thefirst.story_mode_clouds.utils.json.JsonSchemaService;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.minecraft.IdentifierWrapper;

public final class Starter {
    private static final IdentifierWrapper SCHEMA = IdentifierWrapper.of(Initializer.MOD_ID, "api/type_schema.json");

    private static final IdentifierWrapper TYPES = IdentifierWrapper.of(Initializer.MOD_ID, "api/types");
    private static final IdentifierWrapper GENERATORS_BASE = IdentifierWrapper.of(Initializer.MOD_ID, "api/generators/base");
    private static final IdentifierWrapper GENERATORS_OUTLINE = IdentifierWrapper.of(Initializer.MOD_ID, "api/generators/outline");

    private static JsonSchemaService service = null;

    private Starter() {}

    private static void loadTypes() {
        ResourceManager manager = ClientHelper.getClient().getResourceManager();
        if (service == null) {
            try {
                String content = ResourceHandler.readResourceAsString(ResourceHandler.getResource(manager, SCHEMA));
                service = new JsonSchemaService(content);
            }
            catch (Exception e) {
                LoggerProvider.get().error("Error loading JSON schema supplier: {}", e);
                throw new IllegalStateException(e);
            }
        } 

        var definitions = ResourceHandler.getResourcesInDirectoryAndNamespace(manager, TYPES.getNamespace(), TYPES.getPath(), ".json");

        for (var entry : definitions.entrySet()) {
            try {
                MeshTypeData data = service.validateAndProcess(ResourceHandler.readResourceAsString(entry.getValue()), MeshTypeData.class);
                MeshTypeDataCache.addData(data);
                MeshTypeRegistry.getInstance().register(data.name(), () -> new MeshType(data.name(), data.renderOptions().depth(), data.generatorConfig().type() == GeneratorType.NATIVE));
                LoggerProvider.get().info("Successfully initialized and registered type {}", data.name());
            }
            catch (Exception e) {
                LoggerProvider.get().warn("Found incompatible type {}, skipping, reason: {}", entry.getKey(), e);
                continue;
            }
        }
    }

    private static void loadScripts() {
        ResourceManager manager = ClientHelper.getClient().getResourceManager();

        var baseGeneratorScripts = ResourceHandler.getResourcesInDirectoryAndNamespace(manager, GENERATORS_BASE.getNamespace(), GENERATORS_BASE.getPath(), ".lua");
        var outlineGeneratorScripts = ResourceHandler.getResourcesInDirectoryAndNamespace(manager, GENERATORS_BASE.getNamespace(), GENERATORS_OUTLINE.getPath(), ".lua");

        for (var entry : baseGeneratorScripts.entrySet()) {
            try {
                ScriptRegistry.loadScript("base/" + entry.getKey().getFilename(), entry.getValue().open());
                LoggerProvider.get().info("Loaded base script {}", entry.getKey());
            }
            catch (Exception e) {
                LoggerProvider.get().error("Error loading base generator script {}: {}", entry.getKey(), e);
                throw new IllegalStateException(e);
            }
        }

        for (var entry : outlineGeneratorScripts.entrySet()) {
            try {
                ScriptRegistry.loadScript("outline/" + entry.getKey().getFilename(), entry.getValue().open());
                LoggerProvider.get().info("Loaded outline script {}", entry.getKey());
            }
            catch (Exception e) {
                LoggerProvider.get().error("Error loading outline generator script {}: {}", entry.getKey(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    public static void reloadScript() {
        ScriptRegistry.clear();
        loadScripts();
    }

    public static void initialize() {
        loadTypes();
        loadScripts();
    }
}
