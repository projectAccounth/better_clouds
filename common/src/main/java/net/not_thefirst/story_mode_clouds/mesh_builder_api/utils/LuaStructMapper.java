package net.not_thefirst.story_mode_clouds.mesh_builder_api.utils;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import java.lang.reflect.Field;

public class LuaStructMapper {

    /**
     * Converts all top-level fields of an object with supported type to a LuaJ table.
     * @param obj The object.
     * @return The Lua table.
     */
    public static LuaValue toLua(Object obj) {
        if (obj == null) return LuaValue.NIL;
        
        Class<?> clazz = obj.getClass();
        
        if (clazz == String.class) return LuaValue.valueOf((String) obj);
        if (clazz == Integer.class || clazz == int.class) return LuaValue.valueOf((Integer) obj);
        if (clazz == Double.class || clazz == double.class) return LuaValue.valueOf((Double) obj);
        if (clazz == Boolean.class || clazz == boolean.class) return LuaValue.valueOf((Boolean) obj);
        if (clazz == Float.class || clazz == float.class) return LuaValue.valueOf((Float) obj);
        if (clazz == Long.class || clazz == long.class) return LuaValue.valueOf((Long) obj);
        
        if (clazz.isEnum()) {
            return LuaValue.valueOf(((Enum<?>) obj).name());
        }

        LuaTable table = LuaValue.tableOf();
        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                
                String name = field.getName();
                Object value = field.get(obj);
                
                table.set(name, toLua(value));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to map struct field to LuaJ table", e);
        }
        
        return table;
    }
}
