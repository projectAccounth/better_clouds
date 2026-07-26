package net.not_thefirst.story_mode_clouds.mesh_builder_api.runner;

import org.luaj.vm2.Prototype;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

import java.util.concurrent.*;

public final class MeshScriptExecutor {

    private static final int WORKER_THREADS = 1;
    private static final int QUEUE_CAPACITY = 8;

    private static final ThreadPoolExecutor POOL_EXECUTOR;

    static {
        POOL_EXECUTOR = new ThreadPoolExecutor(
                WORKER_THREADS,
                WORKER_THREADS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "MeshBuilder");
                    thread.setDaemon(true); // ensure background threads don't hang the JVM closing cycle
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy() 
        );
    }

    private MeshScriptExecutor() {}

    /**
     * Submits an isolated algorithm task to the thread pool.
     */
    public static void submitTask(Prototype blueprint, 
                                  AbstractStaticMesh.Builder<?, ?> builder, 
                                  CloudsConfiguration.LayerConfiguration config, 
                                  long timeoutMs) {
        
        Future<?> future = POOL_EXECUTOR.submit(() -> {
            ThreadLocalMeshRunner.executeIsolatedScript(blueprint, builder, config);
        });

        scheduleWatchdogTimeout(future, timeoutMs);
    }

    /**
     * Independent tracking thread tasked with terminating runaways after a set limit.
     */
    private static void scheduleWatchdogTimeout(Future<?> future, long timeoutMs) {
        CompletableFuture.runAsync(() -> {
            try {
                // Wait for the task to complete, or throw a timeout exception if it expires
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.err.println("[Watchdog] Mesh script execution timeout. Aborting thread task allocation loop.");
                future.cancel(true); // Force an interruption signal directly to the running thread
            } catch (InterruptedException | ExecutionException e) {
                // TODO: handle shenanigans
            }
        });
    }

    public static void shutdown() {
        POOL_EXECUTOR.shutdownNow();
    }
}

