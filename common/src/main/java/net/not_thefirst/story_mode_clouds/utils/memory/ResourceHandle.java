package net.not_thefirst.story_mode_clouds.utils.memory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceHandle<T> implements AutoCloseable {
    private final T resource;
    private final AtomicBoolean isUsed;

    public ResourceHandle(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        this.resource = resource;
        this.isUsed = new AtomicBoolean(false);
    }

    /**
     * Attempts to acquire the resource. 
     * @return an Optional containing the resource if successful, or empty if already in use.
     */
    public Optional<T> tryAcquire() {
        if (isUsed.compareAndSet(false, true)) {
            return Optional.of(resource);
        }
        return Optional.empty();
    }

    /**
     * Releases the resource handle, making it available for reuse.
     */
    public void release() {
        isUsed.set(false);
    }

    /**
     * Checks the current usage status.
     * @return Whether the resource is being used.
     */
    public boolean isInUse() {
        return isUsed.get();
    }

    @Override
    public void close() {
        release();
    }
}
