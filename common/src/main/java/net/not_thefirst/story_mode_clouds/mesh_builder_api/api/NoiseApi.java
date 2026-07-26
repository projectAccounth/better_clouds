package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import net.not_thefirst.story_mode_clouds.utils.math.PerlinNoise;

public class NoiseApi extends TwoArgFunction {

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();

        library.set("create", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue seedArg) {
                long seed = seedArg.checklong();
                PerlinNoise nativeNoise = new PerlinNoise(seed);
                return new LuaNoiseInstance(nativeNoise);
            }
        });

        env.set("Noise", library);
        return library;
    }

    /**
     * A lightweight wrapper around the instantiated PerlinNoise object.
     * Overrides the get() method to handle object instance method lookups.
     */
    private static class LuaNoiseInstance extends LuaUserdata {
        private final PerlinNoise noiseEngine;

        public LuaNoiseInstance(PerlinNoise noiseEngine) {
            super(noiseEngine);
            this.noiseEngine = noiseEngine;
        }

        @Override
        public LuaValue get(LuaValue key) {
            String methodName = key.checkjstring();
            
            if ("sample".equals(methodName)) {
                return new SampleFunc(noiseEngine);
            }
            
            return super.get(key);
        }
    }

    /**
     * noiseInstance:sample(x, y) OR noiseInstance:sample(x, y, z)
     */
    private static class SampleFunc extends VarArgFunction {
        private final PerlinNoise noiseEngine;

        public SampleFunc(PerlinNoise noiseEngine) {
            this.noiseEngine = noiseEngine;
        }

        @Override
        public Varargs invoke(Varargs args) {
            // args.arg(1) is the self object (the LuaNoiseInstance).
            int argCount = args.narg();

            if (argCount == 3) {
                // noiseInstance:sample(x, y) -> self, x, y
                double x = args.arg(2).checkdouble();
                double y = args.arg(3).checkdouble();
                return LuaValue.valueOf(noiseEngine.noise(x, y));
            } else if (argCount == 4) {
                // noiseInstance:sample(x, y, z) -> self, x, y, z
                double x = args.arg(2).checkdouble();
                double y = args.arg(3).checkdouble();
                double z = args.arg(4).checkdouble();
                return LuaValue.valueOf(noiseEngine.noise(x, y, z));
            }
            
            throw new LuaError("Invalid number of arguments for noise sampling. Expected 2 or 3.");
        }
    }
}
