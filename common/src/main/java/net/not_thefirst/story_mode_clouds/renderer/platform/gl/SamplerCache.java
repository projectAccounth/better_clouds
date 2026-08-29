package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import java.util.HashMap;
import java.util.Map;

import net.not_thefirst.lib.gl_render_system.alt.SamplerDefinition;
import net.not_thefirst.lib.gl_render_system.shader.GLSampler;

class SamplerCache {
    private static final SamplerCache INSTANCE = new SamplerCache();
    private final Map<SamplerDefinition, GLSampler> CACHE = new HashMap<>();

    private SamplerCache() {}

    public static SamplerCache getInstance() {
        return INSTANCE;
    }

    public GLSampler getSampler(SamplerDefinition definition) {
        GLSampler sampler = CACHE.get(definition);
        if (sampler == null) {
            sampler = new GLSampler();
            sampler.setDefinition(definition);
            CACHE.put(definition, sampler);
        }
        return sampler;
    }

   public GLSampler getSampler(SamplerDefinition.FilterDefinition filter, SamplerDefinition.WrapDefinition wrap) {
        SamplerDefinition definition = new SamplerDefinition(filter, wrap);
        return getSampler(definition);
    }

    public void clear() {
        CACHE.values().forEach(GLSampler::close);
        CACHE.clear();
    }
}

