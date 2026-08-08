package net.not_thefirst.story_mode_clouds.utils.memory;

import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

public class SharedHandle<T> {
    private final T resource;
    private final StampedLock lock = new StampedLock();
    private boolean isValid = true;

    public SharedHandle(T resource) {
        this.resource = Objects.requireNonNull(resource, "Resource cannot be null");
    }

    /**
     * Reads or executes against the resource.
     * 
     * @param action The operation to perform on the resource.
     * @return The result, or null if the resource is invalidated or the result of the action is null.
     */
    public <R> R use(ResourceAction<T, R> action) {
        long stamp = lock.readLock();
        try {
            if (!isValid) {
                return null;
            }
            return action.apply(resource);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Invalidates the handle and executes a cleanup routine on the resource.
     * Blocks new readers and waits for active readers to finish before running.
     * 
     * @param disposer Callback to release or destroy the raw resource.
     */
    public void invalidate(Consumer<T> disposer) {
        long stamp = lock.writeLock();
        try {
            if (isValid) {
                isValid = false;
                disposer.accept(resource);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Checks if the handle is still valid.
     */
    public boolean isValid() {
        long stamp = lock.tryOptimisticRead();
        boolean valid = isValid;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                valid = isValid;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return valid;
    }

    @FunctionalInterface
    public interface ResourceAction<T, R> {
        R apply(T resource);
    }
}
