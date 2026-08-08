package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
 
/**
 * Library for bitwise operators for integers wider than 32 bits backed {@code long} via {@link LuaLong}.
 *
 * <pre>{@code
 *   local a = bit64.tolong(0xFFFFFFFF)
 *   local b = bit64.lshift(a, 4)
 *   print(bit64.tohex(b))         -- FFFFFFFF0
 *   print(bit64.tonumber(b))      -- 68719476720
 * }</pre>
 */
public class Bit64Lib extends TwoArgFunction {
 
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable bit64 = new LuaTable();
 
        bit64.set("tolong",   new Fn(Fn.TOLONG));
        bit64.set("tonumber", new Fn(Fn.TONUMBER));
        bit64.set("tohex",    new Fn(Fn.TOHEX));
        bit64.set("band",     new Fn(Fn.BAND));
        bit64.set("bor",      new Fn(Fn.BOR));
        bit64.set("bxor",     new Fn(Fn.BXOR));
        bit64.set("bnot",     new Fn(Fn.BNOT));
        bit64.set("lshift",   new Fn(Fn.LSHIFT));
        bit64.set("rshift",   new Fn(Fn.RSHIFT));
        bit64.set("arshift",  new Fn(Fn.ARSHIFT));
        bit64.set("equals",   new Fn(Fn.EQUALS));
 
        env.set("bit64", bit64);
        LuaValue pkg = env.get("package");
        if (!pkg.isnil()) {
            pkg.get("loaded").set("bit64", bit64);
        }
        return bit64;
    }

    // engineering masterclass
    private static final class Fn extends VarArgFunction {
        static final int TOLONG = 0, TONUMBER = 1, TOHEX = 2, BAND = 3, BOR = 4,
                BXOR = 5, BNOT = 6, LSHIFT = 7, RSHIFT = 8, ARSHIFT = 9, EQUALS = 10;
 
        Fn(int opcode) {
            this.opcode = opcode;
        }
 
        @Override
        public Varargs invoke(Varargs args) {
            switch (opcode) {
 
                case TOLONG:
                    return LuaLong.valueOf(LuaLong.checklong(args.arg1()));
 
                case TONUMBER: {
                    long v = LuaLong.checklong(args.arg1());
                    // Safe: 48-bit values are always exactly representable as a double.
                    return LuaValue.valueOf((double) v);
                }
 
                case TOHEX: {
                    long v = LuaLong.checklong(args.arg1());
                    return LuaValue.valueOf(String.format("%012X", v));
                }
 
                case BAND: {
                    long r = LuaLong.MASK;
                    for (int i = 1; i <= args.narg(); i++) {
                        r &= LuaLong.checklong(args.arg(i));
                    }
                    return LuaLong.valueOf(r);
                }
 
                case BOR: {
                    long r = 0L;
                    for (int i = 1; i <= args.narg(); i++) {
                        r |= LuaLong.checklong(args.arg(i));
                    }
                    return LuaLong.valueOf(r);
                }
 
                case BXOR: {
                    long r = 0L;
                    for (int i = 1; i <= args.narg(); i++) {
                        r ^= LuaLong.checklong(args.arg(i));
                    }
                    return LuaLong.valueOf(r);
                }
 
                case BNOT: {
                    long v = LuaLong.checklong(args.arg1());
                    return LuaLong.valueOf(~v);
                }
 
                case LSHIFT: {
                    long v = LuaLong.checklong(args.arg1());
                    int n = args.checkint(2);
                    return LuaLong.valueOf(n >= 0 ? (v << n) : (v >>> -n));
                }
 
                case RSHIFT: {
                    long v = LuaLong.checklong(args.arg1());
                    int n = args.checkint(2);
                    // unambiguous since values never use bit 63/sign.
                    return LuaLong.valueOf(n >= 0 ? (v >>> n) : (v << -n));
                }
 
                case ARSHIFT: {
                    // No sign bit exists within the 48-bit unsigned range in use here,
                    // so arithmetic and logical right-shift coincide.
                    long v = LuaLong.checklong(args.arg1());
                    int n = args.checkint(2);
                    return LuaLong.valueOf(n >= 0 ? (v >>> n) : (v << -n));
                }
 
                case EQUALS: {
                    long a = LuaLong.checklong(args.arg1());
                    long b = LuaLong.checklong(args.arg(2));
                    return LuaValue.valueOf(a == b);
                }
 
                default:
                    return LuaValue.NONE;
            }
        }
    }
}