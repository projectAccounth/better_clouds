package net.not_thefirst.story_mode_clouds.mesh_builder_api.runner;

import org.luaj.vm2.Prototype;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.math.Texture.TextureData;
import net.not_thefirst.story_mode_clouds.utils.memory.AssetHandle;
import net.not_thefirst.story_mode_clouds.utils.memory.SharedHandle;
import net.not_thefirst.story_mode_clouds.utils.memory.SharedHandle.ResourceAction;

import java.util.concurrent.*;
import java.util.function.Consumer;

public final class MeshBuilderExecutor {

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

    private MeshBuilderExecutor() {}

    public static AssetHandle<AbstractStaticMesh.Builder<?, ?>> submitTask(
        SharedHandle<Prototype> blueprint, 
        AbstractStaticMesh.Builder<?, ?> builder, 
        CloudsConfiguration.LayerConfiguration config,
        TextureData gridData,
        int cx, int cz,
        long timeoutMs, boolean async) {

        Executor executor = async ? POOL_EXECUTOR : Runnable::run;

        final ResourceAction<Prototype, AbstractStaticMesh.Builder<?, ?>> action = (res) -> {
            ThreadLocalMeshRunner.executeScript(res, builder, config, gridData, cx, cz);
            return builder;
        };
        
        CompletableFuture<AbstractStaticMesh.Builder<?, ?>> future = CompletableFuture.supplyAsync(() -> {
            return blueprint.use(action);
        }, executor);

        scheduleWatchdogTimeout(future, timeoutMs);

        return new AssetHandle<>(future);
    }

    public static AssetHandle<AbstractStaticMesh.Builder<?, ?>> wrapTask(
                                AbstractStaticMesh.Builder<?, ?> builder,
                                Consumer<AbstractStaticMesh.Builder<?, ?>> func,
                                long timeoutMs, boolean async) {

        Executor executor = async ? POOL_EXECUTOR : Runnable::run;

        CompletableFuture<AbstractStaticMesh.Builder<?, ?>> future = CompletableFuture.supplyAsync(() -> {
            func.accept(builder);
            return builder;
        }, executor);

        scheduleWatchdogTimeout(future, timeoutMs);

        return new AssetHandle<>(future);
    }

    /**
     * Independent tracking thread tasked with terminating runaways after a set limit.
     */
    private static void scheduleWatchdogTimeout(Future<?> future, long timeoutMs) {
        CompletableFuture.runAsync(() -> {
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                LoggerProvider.get().error("Mesh building execution timeout. Aborting thread task allocation loop.");
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

