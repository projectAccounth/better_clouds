package net.not_thefirst.story_mode_clouds.utils;

public class Texture {
    public static int getColor(long cell) { return (int)(cell >> 4 & 0xFFFFFFFFL); }
    public static boolean isNorthEmpty(long c) { return (c >> 3 & 1L) != 0L; }
    public static boolean isEastEmpty(long c)  { return (c >> 2 & 1L) != 0L; }
    public static boolean isSouthEmpty(long c) { return (c >> 1 & 1L) != 0L; }
    public static boolean isWestEmpty(long c)  { return (c & 1L) != 0L; }
}
