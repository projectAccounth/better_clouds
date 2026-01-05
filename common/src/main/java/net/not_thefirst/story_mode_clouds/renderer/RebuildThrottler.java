package net.not_thefirst.story_mode_clouds.renderer;

public class RebuildThrottler {
    private long lastRebuildMs;
    private final long throttleMs;
    
    public RebuildThrottler(long throttleMs) {
        this.throttleMs = throttleMs;
        this.lastRebuildMs = Long.MIN_VALUE;
    }

    public boolean shouldRebuild(long currentTimeMs) {
        if (currentTimeMs - lastRebuildMs >= throttleMs) {
            lastRebuildMs = currentTimeMs;
            return true;
        }
        return false;
    }
    
    public void reset() {
        lastRebuildMs = Long.MIN_VALUE;
    }
    
    public long getTimeUntilRebuild(long currentTimeMs) {
        long elapsed = currentTimeMs - lastRebuildMs;
        return Math.max(0, throttleMs - elapsed);
    }
}
