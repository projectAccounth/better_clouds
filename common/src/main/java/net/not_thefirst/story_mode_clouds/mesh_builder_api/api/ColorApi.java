package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import net.not_thefirst.lib.utils.math.ARGB;

public class ColorApi extends TwoArgFunction {

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();

        // what the fuck am i doing

        library.set("WHITE", LuaValue.valueOf(ARGB.WHITE));
        library.set("BLACK", LuaValue.valueOf(ARGB.BLACK));

        library.set("alpha", new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.alpha(c.checkint())); } });
        library.set("red",   new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.red(c.checkint())); } });
        library.set("green", new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.green(c.checkint())); } });
        library.set("blue",  new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.blue(c.checkint())); } });

        library.set("alphaFloat", new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.alphaFloat(c.checkint())); } });
        library.set("redFloat",   new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.redFloat(c.checkint())); } });
        library.set("greenFloat", new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.greenFloat(c.checkint())); } });
        library.set("blueFloat",  new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.blueFloat(c.checkint())); } });

        library.set("color",          new ColorFunc());
        library.set("colorFromFloat", new ColorFromFloatFunc());
        library.set("toRGBA",         new OneArgFunction() { @Override public LuaValue call(LuaValue c) { return LuaValue.valueOf(ARGB.toRGBA(c.checkint())); } });
        library.set("fromRGBA",       new FromRGBAFunc());
        library.set("fromRGB",        new FromRGBFunc());
        library.set("withAlpha",      new WithAlphaFunc());
        
        library.set("lerp", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(ARGB.lerp(args.arg(1).checkint(), args.arg(2).checkint(), (float) args.arg(3).checkdouble()));
            }
        });

        library.set("multiply", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (args.arg(2).isint()) {
                    return LuaValue.valueOf(ARGB.multiply(args.arg(1).checkint(), args.arg(2).checkint()));
                }
                return LuaValue.valueOf(ARGB.multiply(args.arg(1).checkint(), (float) args.arg(2).checkdouble()));
            }
        });

        env.set("ARGB", library);
        return library;
    }

    // overload dispatchers

    private static class ColorFunc extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            return LuaValue.valueOf(ARGB.color(args.arg(1).checkint(), args.arg(2).checkint(), args.arg(3).checkint(), args.arg(4).checkint()));
        }
    }

    private static class ColorFromFloatFunc extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            return LuaValue.valueOf(ARGB.colorFromFloat((float) args.arg(1).checkdouble(), (float) args.arg(2).checkdouble(), (float) args.arg(3).checkdouble(), (float) args.arg(4).checkdouble()));
        }
    }

    private static class FromRGBAFunc extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            int count = args.narg();
            if (count == 1) {
                return LuaValue.valueOf(ARGB.fromRGBA(args.arg1().checkint()));
            }
            if (count == 2) {
                return LuaValue.valueOf(ARGB.fromRGBA(args.arg(1).checkint(), args.arg(2).checkint()));
            }
            // 4 arguments layout
            if (args.arg1().isint()) {
                return LuaValue.valueOf(ARGB.fromRGBA(args.arg(1).checkint(), args.arg(2).checkint(), args.arg(3).checkint(), args.arg(4).checkint()));
            }
            return LuaValue.valueOf(ARGB.fromRGBAFloat((float) args.arg(1).checkdouble(), (float) args.arg(2).checkdouble(), (float) args.arg(3).checkdouble(), (float) args.arg(4).checkdouble()));
        }
    }

    private static class FromRGBFunc extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            if (args.arg1().isint()) {
                return LuaValue.valueOf(ARGB.fromRGB(args.arg(1).checkint(), args.arg(2).checkint(), args.arg(3).checkint()));
            }
            return LuaValue.valueOf(ARGB.fromRGBFloat((float) args.arg(1).checkdouble(), (float) args.arg(2).checkdouble(), (float) args.arg(3).checkdouble()));
        }
    }

    private static class WithAlphaFunc extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            if (args.arg(2).isint()) {
                return LuaValue.valueOf(ARGB.withAlpha(args.arg(1).checkint(), args.arg(2).checkint()));
            }
            return LuaValue.valueOf(ARGB.withAlphaFloat(args.arg(1).checkint(), (float) args.arg(2).checkdouble()));
        }
    }
}
