package net.not_thefirst.story_mode_clouds.renderer.shader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ProgramManager implements AutoCloseable {

    public static final class ProgramDefinition {
        private final ResourceLocation vertex;
        private final ResourceLocation fragment;

        public ProgramDefinition(ResourceLocation vert, ResourceLocation frag) {
            this.vertex = vert;
            this.fragment = frag;
        }

        public ResourceLocation vertex() { return this.vertex; }
        public ResourceLocation fragment() { return this.fragment; }
    }

    private ResourceManager resourceManager;

    private final Map<ResourceLocation, ProgramDefinition> definitions = new HashMap<>();
    private final Map<ResourceLocation, GLProgram> programs = new HashMap<>();

    public ProgramManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setResourceManager(ResourceManager manager) {
        this.resourceManager = Objects.requireNonNull(
            manager,
            "Attempted to set a null ResourceManager on ProgramManager"
        );
    }

    /**
     * Registers a new shader program.
     */
    public void register(
        ResourceLocation id,
        ResourceLocation vertex,
        ResourceLocation fragment
    ) {
        Objects.requireNonNull(id, "Program id must not be null");
        Objects.requireNonNull(vertex, "Vertex shader path must not be null");
        Objects.requireNonNull(fragment, "Fragment shader path must not be null");

        definitions.put(id, new ProgramDefinition(vertex, fragment));
    }

    /**
     * Gets (and lazily creates) a program.
     */
    public GLProgram get(ResourceLocation id) {
        Objects.requireNonNull(id, "Program id must not be null");

        GLProgram existing = programs.get(id);
        if (existing != null) {
            return existing;
        }

        ProgramDefinition def = definitions.get(id);
        if (def == null) {
            throw new IllegalStateException(
                "No shader program registered with id: " + id
            );
        }

        if (resourceManager == null) {
            throw new IllegalStateException(
                "Cannot create shader program '" + id + "': ResourceManager is not available yet"
            );
        }

        try {
            GLProgram program = ShaderUtils.create(
                resourceManager,
                def.vertex(),
                def.fragment()
            );

            programs.put(id, program);
            return program;

        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to create shader program '" + id + "'\n" +
                "  Vertex:   " + def.vertex() + "\n" +
                "  Fragment: " + def.fragment(),
                e
            );
        }
    }


    /**
     * Destroys and recompiles a single program.
     */
    public GLProgram reload(ResourceLocation id) {
        Objects.requireNonNull(id, "Program id must not be null");

        GLProgram old = programs.remove(id);
        if (old != null) {
            old.close();
        }

        return get(id);
    }

    /**
     * Reloads all registered programs.
     */
    public void reloadAll() {
        programs.values().forEach(GLProgram::close);
        programs.clear();
    }

    @Override
    public void close() {
        reloadAll();
        definitions.clear();
    }
}
