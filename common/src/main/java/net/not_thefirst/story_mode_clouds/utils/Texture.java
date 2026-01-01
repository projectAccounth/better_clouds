package net.not_thefirst.story_mode_clouds.utils;

public class Texture {
    public static int getColor(long cell) { return (int)(cell >> 4 & 0xFFFFFFFFL); }
    public static boolean isNorthEmpty(long c) { return (c >> 3 & 1L) != 0L; }
    public static boolean isEastEmpty(long c)  { return (c >> 2 & 1L) != 0L; }
    public static boolean isSouthEmpty(long c) { return (c >> 1 & 1L) != 0L; }
    public static boolean isWestEmpty(long c)  { return (c & 1L) != 0L; }
    public static boolean hasAllSidesOccupied(long c) {
        return ((c >> 3 & 1L) == 0L) &&
               ((c >> 2 & 1L) == 0L) &&
               ((c >> 1 & 1L) == 0L) &&
               ((c & 1L) == 0L);
    }

    /*
        clear if the cell to the north is empty, call that empty cell k
        and k's side cells (west and east) are also empty
    */
    public static boolean clearOnNorth(long c) {
        return isNorthEmpty(c) &&
               isWestEmpty((c >> 8) & 0xFFFFFFFFFFFFFFFFL) &&
               isEastEmpty((c >> 16) & 0xFFFFFFFFFFFFFFFFL);
    }

    public static boolean clearOnEast(long c) {
        return isEastEmpty(c) &&
               isNorthEmpty((c >> 16) & 0xFFFFFFFFFFFFFFFFL) &&
               isSouthEmpty((c >> 256) & 0xFFFFFFFFFFFFFFFFL);
    }

    public static boolean clearOnSouth(long c) {
        return isSouthEmpty(c) &&
               isWestEmpty((c >> 256) & 0xFFFFFFFFFFFFFFFFL) &&
               isEastEmpty((c >> 512) & 0xFFFFFFFFFFFFFFFFL);
    }

    public static boolean clearOnWest(long c) {
        return isWestEmpty(c) &&
               isNorthEmpty((c >> 512) & 0xFFFFFFFFFFFFFFFFL) &&
               isSouthEmpty((c >> 4096) & 0xFFFFFFFFFFFFFFFFL);
    }

    public static class TextureData { 
        public long[] cells; 
        public final byte[] neighbors;
        public int width, height;

        public TextureData(long[] cells, byte[] neighbors, int width, int height) {
            this.cells = cells;
            this.width = width;
            this.height = height;
            this.neighbors = neighbors;
        }
    }

    
}
