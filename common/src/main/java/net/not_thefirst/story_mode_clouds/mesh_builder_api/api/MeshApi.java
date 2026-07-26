package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.vertex.GLVertexBuilder;

public class MeshApi extends TwoArgFunction {

    private final AbstractStaticMesh.Builder<?, ?> underlyingBuilder;

    public MeshApi(AbstractStaticMesh.Builder<?, ?> underlyingBuilder) {
        this.underlyingBuilder = underlyingBuilder;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();
        
        library.set("addVertex", new AddVertexFunc());
        library.set("setColor", new SetColorFunc());
        library.set("setUv", new SetUvFunc());
        library.set("setNormal", new SetNormalFunc());
        library.set("setLineWidth", new SetLineWidthFunc());
        library.set("quad", new QuadFunc());
        library.set("triangle", new TriangleFunc());
        
        env.set("Mesh", library);
        return library;
    }
    
    /** 
     * Mesh.addVertex(x, y, z) 
     */
    private class AddVertexFunc extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            underlyingBuilder.addVertex(
                (float) x.checkdouble(),
                (float) y.checkdouble(),
                (float) z.checkdouble()
            );
            return NONE;
        }
    }

    /** 
     * Mesh.setColor(color) 
     * OR 
     * Mesh.setColor(r, g, b, a) 
     */
    private class SetColorFunc extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            int argCount = args.narg();
            
            if (argCount == 1) {
                // etColor(int color)
                int hexColor = args.arg1().checkint();
                underlyingBuilder.setColor(hexColor);
            } else if (argCount == 4) {
                // setColor(int r, int g, int b, int a)
                int r = args.arg(1).checkint();
                int g = args.arg(2).checkint();
                int b = args.arg(3).checkint();
                int a = args.arg(4).checkint();
                underlyingBuilder.setColor(r, g, b, a);
            }
            return NONE;
        }
    }

    /** Mesh.setUv(u, v) */
    private class SetUvFunc extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            // Flexible checking for either float or int signatures
            if (args.arg1().isint()) {
                underlyingBuilder.setUv1(args.arg(1).checkint(), args.arg(2).checkint());
            } else {
                underlyingBuilder.setUv((float) args.arg(1).checkdouble(), (float) args.arg(2).checkdouble());
            }
            return NONE;
        }
    }

    /** Mesh.setNormal(x, y, z) */
    private class SetNormalFunc extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            underlyingBuilder.setNormal(
                (float) x.checkdouble(),
                (float) y.checkdouble(),
                (float) z.checkdouble()
            );
            return NONE;
        }
    }

    /** Mesh.setLineWidth(width) */
    private class SetLineWidthFunc extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            underlyingBuilder.setLineWidth((float) args.arg1().checkdouble());
            return NONE;
        }
    }

    /** 
     * Mesh.quad(x1, y1, z1, ..., x4, y4, z4, hexColor)
     */
    private class QuadFunc extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            float[] positions = new float[12];
            for (int i = 0; i < 12; i++) {
                // Lua arguments are 1-indexed
                positions[i] = (float) args.arg(i + 1).checkdouble(); 
            }
            int hexColor = args.arg(13).checkint();
            
            GLVertexBuilder.quad(underlyingBuilder,
                positions[0], positions[1], positions[2],
                positions[3], positions[4], positions[5],
                positions[6], positions[7], positions[8],
                positions[9], positions[10], positions[11],
                hexColor);
            return NONE;
        }
    }

    /** 
     * Mesh.triangle(x1, y1, z1, ..., x3, y3, z3, hexColor)
     */
    private class TriangleFunc extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            float[] positions = new float[9];
            for (int i = 0; i < 9; i++) {
                positions[i] = (float) args.arg(i + 1).checkdouble();
            }
            int hexColor = args.arg(10).checkint();
            
            GLVertexBuilder.triangle(underlyingBuilder,
                positions[0], positions[1], positions[2],
                positions[3], positions[4], positions[5],
                positions[6], positions[7], positions[8], 
                hexColor);
            return NONE;
        }
    }
}
