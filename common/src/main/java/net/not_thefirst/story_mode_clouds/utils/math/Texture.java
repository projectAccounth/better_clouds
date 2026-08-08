package net.not_thefirst.story_mode_clouds.utils.math;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import net.not_thefirst.lib.utils.math.ARGB;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

public class Texture {

    private Texture() {}

    public static int getColor(long cell) {
        return (int)((cell >> 4) & 0xFFFFFFFFL);
    }

    public static boolean isNorthEmpty(long c) {
        return ((c >> 3) & 1L) != 0L;
    }

    public static boolean isEastEmpty(long c) {
        return ((c >> 2) & 1L) != 0L;
    }

    public static boolean isSouthEmpty(long c) {
        return ((c >> 1) & 1L) != 0L;
    }

    public static boolean isWestEmpty(long c) {
        return (c & 1L) != 0L;
    }

    public static boolean hasAllSidesOccupied(long c) {
        return !isNorthEmpty(c)
            && !isEastEmpty(c)
            && !isSouthEmpty(c)
            && !isWestEmpty(c);
    }

    public static int idx(int x, int y, int w, int h) {
        x = Math.floorMod(x, w);
        y = Math.floorMod(y, h);
        return x + y * w;
    }

    private static boolean isEmpty(long cell) {
        return cell == 0L;
    }

    private static boolean isSolid(long cell) {
        return cell != 0L;
    }

    public static boolean isNorthSideClear(
            long[] cells, int x, int y, int w, int h
    ) {
        int k = idx(x, y - 1, w, h);
        if (!isEmpty(cells[k])) {
            return false;
        }

        return isEmpty(cells[idx(x - 1, y - 1, w, h)])
            && isEmpty(cells[idx(x + 1, y - 1, w, h)]);
    }

    public static boolean isSouthSideClear(
            long[] cells, int x, int y, int w, int h
    ) {
        int k = idx(x, y + 1, w, h);
        if (!isEmpty(cells[k])) {
            return false;
        }

        return isEmpty(cells[idx(x - 1, y + 1, w, h)])
            && isEmpty(cells[idx(x + 1, y + 1, w, h)]);
    }

    public static boolean isEastSideClear(
            long[] cells, int x, int y, int w, int h
    ) {
        int k = idx(x + 1, y, w, h);
        if (!isEmpty(cells[k])) {
            return false;
        }

        return isEmpty(cells[idx(x + 1, y - 1, w, h)])
            && isEmpty(cells[idx(x + 1, y + 1, w, h)]);
    }

    public static boolean isWestSideClear(
            long[] cells, int x, int y, int w, int h
    ) {
        int k = idx(x - 1, y, w, h);
        if (!isEmpty(cells[k])) {
            return false;
        }

        return isEmpty(cells[idx(x - 1, y - 1, w, h)])
            && isEmpty(cells[idx(x - 1, y + 1, w, h)]);
    }

    public static int occupiedCardinalCount(
            long[] cells, int x, int y, int w, int h
    ) {
        int count = 0;

        if (isSolid(cells[idx(x,     y - 1, w, h)])) count++; // N
        if (isSolid(cells[idx(x + 1, y,     w, h)])) count++; // E
        if (isSolid(cells[idx(x,     y + 1, w, h)])) count++; // S
        if (isSolid(cells[idx(x - 1, y,     w, h)])) count++; // W

        return count;
    }

    public static class TextureData {
        public final long[] cells;
        public final byte[] neighbors;
        public final int width;
        public final int height;

        public TextureData(long[] cells, byte[] neighbors, int width, int height) {
            this.cells = cells;
            this.neighbors = neighbors;
            this.width = width;
            this.height = height;
        }
    }

    public static Optional<TextureData> buildTexture(InputStream imgStream, boolean flipX, boolean flipY) {
        try (NativeImage nativeImage = NativeImage.read(imgStream)) {
            
            int w = nativeImage.getWidth();
            int h = nativeImage.getHeight();

            long[] cells = new long[w * h];
            byte[] neighbors = new byte[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int gridCoordX = flipX ? (w - 1 - x) : x;
                    int gridCoordY = flipY ? (h - 1 - y) : y;
                    int idx = idx(gridCoordX, gridCoordY, w, h);
                    int pixelRGBA = nativeImage.getPixel(x, y);

                    int a = (pixelRGBA >> 24) & 0xFF;
                    int r = (pixelRGBA >> 16) & 0xFF;
                    int g = (pixelRGBA >> 8) & 0xFF;
                    int b = (pixelRGBA) & 0xFF;

                    int pixel = ARGB.color(a, r, g, b);

                    if (!isPixelSolid(a)) {
                        cells[idx] = 0L;
                        neighbors[idx] = 0;
                        continue;
                    }

                    int count = 0;

                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dz == 0) {
                                continue;
                            }

                            if (isSolid(nativeImage, x + dx, y + dz, w, h)) {
                                count++;
                            }
                        }
                    }

                    boolean n  = !isSolid(nativeImage, x, flipY ? y + 1 : y - 1, w, h);
                    boolean e  = !isSolid(nativeImage, flipX ? x - 1 : x + 1, y, w, h);
                    boolean s  = !isSolid(nativeImage, x, flipY ? y - 1 : y + 1, w, h);
                    boolean w0 = !isSolid(nativeImage, flipX ? x + 1 : x - 1, y, w, h);

                    cells[idx] = packCellData(pixel, n, e, s, w0);
                    neighbors[idx] = (byte)count;
                }
            }

            return Optional.of(
                new Texture.TextureData(cells, neighbors, w, h)
            );

        } catch (IOException e) {
            LoggerProvider.get().error("Failed to load texture: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Texture.TextureData> buildTexture(InputStream imgStream) {
        return buildTexture(imgStream, false, false);
    }

    private static boolean isPixelSolid(int alpha) {
        return alpha >= 5;
    }


    private static boolean isSolid(NativeImage img, int x, int y, int w, int h) {
        int pixelRGBA = img.getPixel(
            Math.floorMod(x, w),
            Math.floorMod(y, h));

        int a = (pixelRGBA >> 24) & 0xFF;
        return isPixelSolid(a);
    }

    private static long packCellData(int color, boolean north, boolean east, boolean south, boolean west) {
        return (long) color << 4 |
               (north ? 1 : 0) << 3 |
               (east ? 1 : 0) << 2 |
               (south ? 1 : 0) << 1 |
               (west ? 1 : 0);
    }
}
