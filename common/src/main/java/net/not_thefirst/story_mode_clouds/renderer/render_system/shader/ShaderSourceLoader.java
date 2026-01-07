package net.not_thefirst.story_mode_clouds.renderer.render_system.shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ShaderSourceLoader {

    private ShaderSourceLoader() {}

    public static String load(ResourceManager rm, ResourceLocation rl) {
        try (Resource res = rm.getResource(rl);
             InputStream in = res.getInputStream()) {

            return IOUtils.toString(in, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + rl, e);
        }
    }
}
