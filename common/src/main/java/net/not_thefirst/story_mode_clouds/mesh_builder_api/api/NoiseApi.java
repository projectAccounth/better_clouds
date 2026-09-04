package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import net.not_thefirst.story_mode_clouds.utils.math.PerlinNoise;

public class NoiseApi extends TwoArgFunction {

    // seed -> PerlinNoise instance
    private static final Map<Long, PerlinNoise> noiseInstances = new HashMap<>();

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();

        library.set("create", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue seedArg) {
                long seed = seedArg.checklong();
                if (noiseInstances.containsKey(seed)) {
                    return new LuaNoiseInstance(noiseInstances.get(seed));
                }

                PerlinNoise nativeNoise = new PerlinNoise(seed);
                noiseInstances.put(seed, nativeNoise);
                return new LuaNoiseInstance(nativeNoise);
            }
        });
        library.set("new", library.get("create"));

        env.set("Noise", library);
        return library;
    }

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
            if ("sample2D".equals(methodName)) {
                return new Sample2DFunc(noiseEngine);
            }
            if ("sample3D".equals(methodName)) {
                return new Sample3DFunc(noiseEngine);
            }
            
            return super.get(key);
        }
    }

    /**
     * noiseInstance:sample(x, y) and noiseInstance:sample(x, y, z)
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
                // noiseInstance:sample(x, y)
                double x = args.arg(2).checkdouble();
                double y = args.arg(3).checkdouble();
                return LuaValue.valueOf(noiseEngine.noise(x, y));
            } else if (argCount == 4) {
                // noiseInstance:sample(x, y, z)
                double x = args.arg(2).checkdouble();
                double y = args.arg(3).checkdouble();
                double z = args.arg(4).checkdouble();
                return LuaValue.valueOf(noiseEngine.noise(x, y, z));
            }
            
            throw new LuaError("Invalid number of arguments for noise sampling. Expected 2 or 3.");
        }
    }

    private static class Sample2DFunc extends TwoArgFunction {
        private final PerlinNoise noiseEngine;

        public Sample2DFunc(PerlinNoise noiseEngine) {
            this.noiseEngine = noiseEngine;
        }

        @Override
        public LuaValue call(LuaValue x, LuaValue y) {
            double xVal = x.checkdouble();
            double yVal = y.checkdouble();
            return LuaValue.valueOf(noiseEngine.noise(xVal, yVal));
        }
    }

    private static class Sample3DFunc extends ThreeArgFunction {
        private final PerlinNoise noiseEngine;

        public Sample3DFunc(PerlinNoise noiseEngine) {
            this.noiseEngine = noiseEngine;
        }

        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            double xVal = x.checkdouble();
            double yVal = y.checkdouble();
            double zVal = z.checkdouble();
            return LuaValue.valueOf(noiseEngine.noise(xVal, yVal, zVal));
        }
    }
}
