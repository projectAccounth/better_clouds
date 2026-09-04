package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import net.not_thefirst.story_mode_clouds.utils.math.Texture;
import net.not_thefirst.story_mode_clouds.utils.math.Texture.TextureData;
import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

/** The convenient, local-offset view of the texture used by a mesh generator. */
public final class CloudGridApi extends TwoArgFunction {
    private final TextureData data;
    private final WrappedCoordinates coordinates;

    public CloudGridApi(TextureData data, WrappedCoordinates coordinates) {
        this.data = data;
        this.coordinates = coordinates;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();
        library.set("range", LuaValue.valueOf(coordinates.getRange()));
        library.set("width", LuaValue.valueOf(data.width));
        library.set("height", LuaValue.valueOf(data.height));
        library.set("getRange", new GetRange());
        library.set("getSize", new GetSize());
        library.set("getCell", new GetCell());
        library.set("isEmpty", new IsEmpty());
        library.set("isOccupied", new IsOccupied());
        library.set("getColor", new GetColor());
        library.set("getWrappedX", new GetWrappedX());
        library.set("getWrappedZ", new GetWrappedZ());
        library.set("getIndex", new GetIndex());
        library.set("forEach", new ForEach());
        library.set("forEachOccupied", new ForEachOccupied());

        env.set("CloudGrid", library);
        return library;
    }

    private long cell(int x, int z) {
        return data.cells[coordinates.getCellIndex(x, z)];
    }

    private LuaTable cellTable(int x, int z) {
        long cell = cell(x, z);
        LuaTable result = new LuaTable();
        result.set("x", LuaValue.valueOf(x));
        result.set("z", LuaValue.valueOf(z));
        result.set("localX", LuaValue.valueOf(x));
        result.set("localZ", LuaValue.valueOf(z));
        result.set("wrappedX", LuaValue.valueOf(coordinates.getWrappedX(x)));
        result.set("wrappedZ", LuaValue.valueOf(coordinates.getWrappedZ(z)));
        result.set("textureX", LuaValue.valueOf(coordinates.getWrappedX(x)));
        result.set("textureZ", LuaValue.valueOf(coordinates.getWrappedZ(z)));
        result.set("index", LuaValue.valueOf(coordinates.getCellIndex(x, z)));
        result.set("data", LuaLong.valueOf(cell));
        result.set("color", LuaValue.valueOf(Texture.getColor(cell)));
        result.set("empty", LuaValue.valueOf(cell == 0L));
        result.set("occupied", LuaValue.valueOf(cell != 0L));
        result.set("northEmpty", LuaValue.valueOf(Texture.isNorthEmpty(cell)));
        result.set("eastEmpty", LuaValue.valueOf(Texture.isEastEmpty(cell)));
        result.set("southEmpty", LuaValue.valueOf(Texture.isSouthEmpty(cell)));
        result.set("westEmpty", LuaValue.valueOf(Texture.isWestEmpty(cell)));
        result.set("neighborCount", LuaValue.valueOf(data.neighbors[coordinates.getCellIndex(x, z)] & 0xFF));
        return result;
    }

    private final class GetRange extends ZeroArgFunction {
        @Override
        public LuaValue call() {
            return LuaValue.valueOf(coordinates.getRange());
        }
    }

    private final class GetSize extends ZeroArgFunction {
        @Override
        public LuaValue call() {
            return LuaValue.valueOf(coordinates.getRange() * 2 + 1);
        }
    }

    private final class GetCell extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return cellTable(x.checkint(), z.checkint());
        }
    }

    private final class IsEmpty extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return LuaValue.valueOf(cell(x.checkint(), z.checkint()) == 0L);
        }
    }

    private final class IsOccupied extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return LuaValue.valueOf(cell(x.checkint(), z.checkint()) != 0L);
        }
    }

    private final class GetColor extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return LuaValue.valueOf(Texture.getColor(cell(x.checkint(), z.checkint())));
        }
    }

    private final class GetWrappedX extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue x) {
            return LuaValue.valueOf(coordinates.getWrappedX(x.checkint()));
        }
    }

    private final class GetWrappedZ extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue z) {
            return LuaValue.valueOf(coordinates.getWrappedZ(z.checkint()));
        }
    }

    private final class GetIndex extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return LuaValue.valueOf(coordinates.getCellIndex(x.checkint(), z.checkint()));
        }
    }

    private final class ForEach extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue callback) {
            callback.checkfunction();
            int range = coordinates.getRange();
            for (int z = -range; z <= range; z++) {
                for (int x = -range; x <= range; x++) {
                    callback.call(cellTable(x, z));
                }
            }
            return NONE;
        }
    }

    private final class ForEachOccupied extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue callback) {
            callback.checkfunction();
            int range = coordinates.getRange();
            for (int z = -range; z <= range; z++) {
                for (int x = -range; x <= range; x++) {
                    if (cell(x, z) != 0L) {
                        callback.call(cellTable(x, z));
                    }
                }
            }
            return NONE;
        }
    }
}