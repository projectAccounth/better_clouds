package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
 
/**
 * Boxed integer wider than 32 bits (up to 48 bits), backed by a
 * long. Values are always masked to 48 bits and therefore always
 * fit in a non-negative long, so there is no sign-extension to
 * worry about when shifting.
 *
 * <p>
 * Instances are immutable. Use {@link #valueOf(long)} to create one and
 * {@link #checklong(LuaValue)} to pull a raw long back out of a LuaValue
 * that might be a LuaLong, a plain Lua number, or a numeric string.
 * </p>
 */
public final class LuaLong extends LuaUserdata {
 
    // 48-bit mask
    public static final long MASK = 0x0000FFFFFFFFFFFFL;
 
    private static final LuaTable table = new LuaTable();
 
    static {
        table.set("__tostring", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue ignored) {
                return LuaValue.valueOf(self.tojstring());
            }
        });
        table.set("__eq", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue a, LuaValue b) {
                return LuaValue.valueOf(a.checklong() == b.checklong());
            }
        });
        table.set("__lt", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue a, LuaValue b) {
                return LuaValue.valueOf(Long.compareUnsigned(a.checklong(), b.checklong()) < 0);
            }
        });
        table.set("__le", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue a, LuaValue b) {
                return LuaValue.valueOf(Long.compareUnsigned(a.checklong(), b.checklong()) <= 0);
            }
        });
    }
 
    private final long value;
 
    private LuaLong(long value) {
        super(Long.valueOf(value & MASK));
        this.value = value & MASK;
        this.setmetatable(table);
    }
 
    /** Wraps a long, masking it down to 48 bits */
    public static LuaLong valueOf(long v) {
        return new LuaLong(v);
    }
 
    /** The raw (already-masked) 48-bit value. */
    public long longValue() {
        return value;
    }
 
    @Override
    public String typename() {
        return "long";
    }
 
    @Override
    public String tojstring() {
        return Long.toString(value);
    }
 
    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }
 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LuaLong)) return false;
        return value == ((LuaLong) o).value;
    }
 
    /**
     * Extracts a raw 48-bit-masked long from a LuaValue. Accepts:
     * <li>a {@link LuaLong}</li>
     * <li>a plain Lua number (int or double)</li>
     * <li>a numeric string</li>
     * Throws a {@link LuaError} for anything else.
     */
    public static long checklong(LuaValue v) {
        if (v instanceof LuaLong) {
            return ((LuaLong) v).value;
        }
        if (v.isnumber()) {
            return ((long) v.checkdouble()) & MASK;
        }
        if (v.isstring()) {
            try {
                return Long.parseLong(v.checkjstring().trim()) & MASK;
            } catch (NumberFormatException e) {
                throw new LuaError("cannot convert string to long: " + v.tojstring());
            }
        }
        throw new LuaError("expected long/number, got " + v.typename());
    }
}