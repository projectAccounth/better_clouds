package net.not_thefirst.story_mode_clouds.mesh_builder_api.api;

import org.luaj.vm2.LuaValue;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.OneArgFunction;

import net.not_thefirst.story_mode_clouds.utils.math.WrappedCoordinates;

public class GridCoordsApi extends TwoArgFunction {

    private final WrappedCoordinates underlying;

    public GridCoordsApi(WrappedCoordinates underlying) {
        this.underlying = underlying;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable library = new LuaTable();
        
        library.set("getCellIndex", new GetCellIndex());
        library.set("getWrappedX", new GetCellX());
        library.set("getWrappedZ", new GetCellZ());
        library.set("getCloudGridHalfSize", new Range());
        
        env.set("Coords", library);
        return library;
    }
    
    private class GetCellIndex extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue z) {
            return LuaValue.valueOf(underlying.getCellIndex(
                x.checkint(),
                z.checkint()
            ));
        }
    }

    private class GetCellX extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue x) {
            return LuaValue.valueOf(underlying.getWrappedX(
                x.checkint()
            ));
        }
    }

    private class GetCellZ extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue x) {
            return LuaValue.valueOf(underlying.getWrappedZ(
                x.checkint()
            ));
        }
    }

    private class Range extends ZeroArgFunction {
        @Override
        public LuaValue call() {
            return LuaValue.valueOf(underlying.getRange());
        }
    }
}
