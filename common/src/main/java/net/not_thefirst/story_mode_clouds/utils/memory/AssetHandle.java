package net.not_thefirst.story_mode_clouds.utils.memory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

public class AssetHandle<T> {
    private final CompletableFuture<T> future;
    private T asset = null;
    private boolean failed = false;
    private Throwable exception = null;

    public AssetHandle(CompletableFuture<T> future) {
        this.future = future;

        if (future == null) {
            failed = true;
            return;
        }
        
        this.future.thenAccept(result -> this.asset = result)
                   .exceptionally(ex -> {
                       this.failed = true;
                       this.exception = ex;
                       return null;
                   });
    }

    public boolean isReady() {
        return asset != null;
    }

    public boolean isPending() {
        return !isReady() && !hasFailed();
    }

    public boolean hasFailed() {
        return failed;
    }

    public T get() {
        return asset;
    }

    public Throwable getException() {
        return exception;
    }

    public void onLoaded(Consumer<T> callback) {
        if (failed)
            LoggerProvider.get().error("Trying to hook into a failed handle");
        future.thenAccept(callback);
    }
}
