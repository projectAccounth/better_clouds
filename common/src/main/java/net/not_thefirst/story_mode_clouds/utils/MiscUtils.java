package net.not_thefirst.story_mode_clouds.utils;

public class MiscUtils {
    public static final class CacheKey {
        final int texHash;
        final int layer;

        public CacheKey(int texHash, int layer) {
            this.texHash = texHash;
            this.layer = layer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return texHash == k.texHash && layer == k.layer;
        }

        @Override
        public int hashCode() {
            return 31 * texHash + layer;
        }
    }
}