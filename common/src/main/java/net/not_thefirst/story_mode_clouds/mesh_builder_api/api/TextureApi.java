package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import net.not_thefirst.story_mode_clouds.utils.math.Texture.TextureData;

import static net.not_thefirst.story_mode_clouds.utils.math.Texture.*;

public class TextureApi extends TwoArgFunction {

    private final TextureData data;

    public TextureApi(TextureData data) {
        this.data = data;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable lib = new LuaTable();

        lib.set("width", LuaValue.valueOf(data.width));
        lib.set("height", LuaValue.valueOf(data.height));

        lib.set("idx", new Fn(Fn.IDX, data));
        lib.set("getCell", new Fn(Fn.GET_CELL, data));
        lib.set("getNeighbor", new Fn(Fn.GET_NEIGHBOR, data));
        lib.set("setNeighbor", new Fn(Fn.SET_NEIGHBOR, data));

        lib.set("getColor", new Fn(Fn.GET_COLOR, data));
        lib.set("isNorthEmpty", new Fn(Fn.IS_NORTH_EMPTY, data));
        lib.set("isEastEmpty", new Fn(Fn.IS_EAST_EMPTY, data));
        lib.set("isSouthEmpty", new Fn(Fn.IS_SOUTH_EMPTY, data));
        lib.set("isWestEmpty", new Fn(Fn.IS_WEST_EMPTY, data));
        lib.set("hasAllSidesOccupied", new Fn(Fn.HAS_ALL_SIDES_OCCUPIED, data));
        lib.set("isCellEmpty", new Fn(Fn.IS_CELL_EMPTY, data));

        lib.set("isNorthSideClear", new Fn(Fn.IS_NORTH_SIDE_CLEAR, data));
        lib.set("isSouthSideClear", new Fn(Fn.IS_SOUTH_SIDE_CLEAR, data));
        lib.set("isEastSideClear", new Fn(Fn.IS_EAST_SIDE_CLEAR, data));
        lib.set("isWestSideClear", new Fn(Fn.IS_WEST_SIDE_CLEAR, data));
        lib.set("occupiedCardinalCount", new Fn(Fn.OCCUPIED_CARDINAL_COUNT, data));

        env.set("Texture", lib);
        LuaValue pkg = env.get("package");
        if (!pkg.isnil()) {
            pkg.get("loaded").set("Texture", lib);
        }
        return lib;
    }

    private static final class Fn extends VarArgFunction {
        static final int IDX = 0, GET_CELL = 1, GET_NEIGHBOR = 3, SET_NEIGHBOR = 4,
                GET_COLOR = 5, IS_NORTH_EMPTY = 6, IS_EAST_EMPTY = 7, IS_SOUTH_EMPTY = 8,
                IS_WEST_EMPTY = 9, HAS_ALL_SIDES_OCCUPIED = 10,
                IS_NORTH_SIDE_CLEAR = 11, IS_SOUTH_SIDE_CLEAR = 12,
                IS_EAST_SIDE_CLEAR = 13, IS_WEST_SIDE_CLEAR = 14, OCCUPIED_CARDINAL_COUNT = 15,
                IS_CELL_EMPTY = 16;

        private final TextureData data;

        Fn(int opcode, TextureData data) {
            this.opcode = opcode;
            this.data = data;
        }

        private long resolveCell(Varargs args, int w, int h) {
            if (args.narg() >= 2) {
                int k = idx(args.checkint(1), args.checkint(2), w, h);
                return data.cells[k];
            }
            return LuaLong.checklong(args.arg1());
        }

        @Override
        public Varargs invoke(Varargs args) {
            int w = data.width, h = data.height;

            switch (opcode) {
                case IDX:
                    return LuaValue.valueOf(idx(args.checkint(1), args.checkint(2), w, h));

                case GET_CELL: {
                    int k = idx(args.checkint(1), args.checkint(2), w, h);
                    return LuaLong.valueOf(data.cells[k]);
                }

                case GET_NEIGHBOR: {
                    int k = idx(args.checkint(1), args.checkint(2), w, h);
                    return LuaValue.valueOf(data.neighbors[k] & 0xFF);
                }

                case SET_NEIGHBOR: {
                    int k = idx(args.checkint(1), args.checkint(2), w, h);
                    data.neighbors[k] = (byte) args.checkint(3);
                    return LuaValue.NONE;
                }

                case GET_COLOR:
                    return LuaValue.valueOf(getColor(resolveCell(args, w, h)));

                case IS_NORTH_EMPTY:
                    return LuaValue.valueOf(isNorthEmpty(resolveCell(args, w, h)));

                case IS_EAST_EMPTY:
                    return LuaValue.valueOf(isEastEmpty(resolveCell(args, w, h)));

                case IS_SOUTH_EMPTY:
                    return LuaValue.valueOf(isSouthEmpty(resolveCell(args, w, h)));

                case IS_WEST_EMPTY:
                    return LuaValue.valueOf(isWestEmpty(resolveCell(args, w, h)));

                case HAS_ALL_SIDES_OCCUPIED:
                    return LuaValue.valueOf(hasAllSidesOccupied(resolveCell(args, w, h)));

                case IS_NORTH_SIDE_CLEAR:
                    return LuaValue.valueOf(
                        isNorthSideClear(data.cells, args.checkint(1), args.checkint(2), w, h));

                case IS_SOUTH_SIDE_CLEAR:
                    return LuaValue.valueOf(
                        isSouthSideClear(data.cells, args.checkint(1), args.checkint(2), w, h));

                case IS_EAST_SIDE_CLEAR:
                    return LuaValue.valueOf(
                        isEastSideClear(data.cells, args.checkint(1), args.checkint(2), w, h));

                case IS_WEST_SIDE_CLEAR:
                    return LuaValue.valueOf(
                        isWestSideClear(data.cells, args.checkint(1), args.checkint(2), w, h));

                case OCCUPIED_CARDINAL_COUNT:
                    return LuaValue.valueOf(
                        occupiedCardinalCount(data.cells, args.checkint(1), args.checkint(2), w, h));

                case IS_CELL_EMPTY: {
                    return LuaValue.valueOf(resolveCell(args, w, h) == 0L);
                }

                default:
                    return LuaValue.NONE;
            }
        }
    }
}