package net.not_thefirst.story_mode_clouds.mesh_builder_api.compiler;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class LuaJitCompiler {

    private static final Globals COMPILER_CONTEXT;

    static {
        COMPILER_CONTEXT = JsePlatform.standardGlobals();
        LuaJC.install(COMPILER_CONTEXT);
    }

    private LuaJitCompiler() {
        // static init
    }

    /**
     * Compiles an InputStream directly to real JVM bytecode in memory.
     * 
     * @param stream     The resource stream from your mod's file API.
     * @param chunkName  A debugging name given to the script (e.g., "wave_gen.lua").
     * @return A thread-safe, cached Proto blueprint containing the compiled bytecode loops.
     * @throws IOException If the stream fails to read or compile.
     */
    public static Prototype compile(InputStream stream, String chunkName) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("Cannot compile a null InputStream resource.");
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Prototype prototype = COMPILER_CONTEXT.compilePrototype(reader, chunkName);
            
            if (prototype == null) {
                throw new IOException("Compiler failed to generate bytecode for chunk: " + chunkName);
            }
            
            return prototype;
        } catch (Exception e) {
            throw new IOException("In-memory bytecode JIT compilation failure for " + chunkName, e);
        }
    }
}
