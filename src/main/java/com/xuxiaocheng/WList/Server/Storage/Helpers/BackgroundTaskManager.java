package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class BackgroundTaskManager {
    private BackgroundTaskManager() {
        super();
    }

    public static final @NotNull EventExecutorGroup BackgroundExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("BackgroundExecutors"));

    public static final @NotNull String SyncDirectory = "Sync directory.";
    public static final @NotNull String SyncInfo = "Sync info.";

    public record BackgroundTaskIdentifier(@NotNull String provider, @NotNull String task, @NotNull String identifier) {
    }

    private static final @NotNull Map<@NotNull BackgroundTaskIdentifier, @NotNull CompletableFuture<?>> tasks = new ConcurrentHashMap<>();

    public static @Nullable CompletableFuture<?> background(final @NotNull BackgroundTaskIdentifier identify, final @NotNull Runnable runnable) {
        final boolean[] flag = {true};
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.computeIfAbsent(identify, k -> {
            flag[0] = false;
            return CompletableFuture.runAsync(runnable, BackgroundTaskManager.BackgroundExecutors).whenComplete((v, e) -> {
                BackgroundTaskManager.tasks.remove(identify);
                if (e != null)
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
            });
        });
        return flag[0] ? null : future;
    }

    @Deprecated
    public static @Nullable CompletableFuture<?> get(final @NotNull BackgroundTaskIdentifier identify) {
        return BackgroundTaskManager.tasks.get(identify);
    }

    public static void cancel(final @NotNull BackgroundTaskIdentifier identify) {
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.remove(identify);
        if (future != null)
            future.cancel(true);
    }

    @Deprecated
    public static void join(final @NotNull BackgroundTaskIdentifier identify) {
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.get(identify);
        if (future != null)
            future.join();
    }

    public static void onFinally(final @NotNull BackgroundTaskIdentifier identify, final Runnable runnable) {
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.get(identify);
        if (future == null)
            runnable.run();
        else
            future.whenComplete((v, e) -> runnable.run());
    }
}
