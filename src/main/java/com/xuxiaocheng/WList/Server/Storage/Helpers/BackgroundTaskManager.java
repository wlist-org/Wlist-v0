package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
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
    public static final @NotNull String Uploading = "Create or upload.";

    public record BackgroundTaskIdentifier(@NotNull String provider, @NotNull String task, @NotNull String identifier) {
    }

    private static final @NotNull Map<@NotNull BackgroundTaskIdentifier, @NotNull CompletableFuture<?>> tasks = new ConcurrentHashMap<>();
    private static final @NotNull Set<@NotNull BackgroundTaskIdentifier> removable = ConcurrentHashMap.newKeySet();

    public static @Nullable CompletableFuture<?> background(final @NotNull BackgroundTaskIdentifier identify, final @NotNull Runnable runnable, final boolean removeLock) {
        final boolean[] flag = {true};
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.computeIfAbsent(identify, k -> {
            flag[0] = false;
            if (!removeLock)
                BackgroundTaskManager.removable.add(identify);
            return CompletableFuture.runAsync(runnable, BackgroundTaskManager.BackgroundExecutors).whenCompleteAsync((v, e) -> {
                if (removeLock)
                    BackgroundTaskManager.tasks.remove(identify);
                if (e != null)
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
            }, BackgroundTaskManager.BackgroundExecutors); // Async to prevent 'java.lang.IllegalStateException: Recursive update'.
        });
        return flag[0] ? null : future;
    }

    public static boolean isExist(final @NotNull BackgroundTaskIdentifier identify) {
        return BackgroundTaskManager.tasks.containsKey(identify);
    }

    public static boolean createIfNot(final @NotNull BackgroundTaskIdentifier identify) {
        if (BackgroundTaskManager.tasks.putIfAbsent(identify, CompletableFuture.completedFuture(null)) == null) {
            BackgroundTaskManager.removable.add(identify);
            return false; // Success.
        }
        return true;
    }

    public static void remove(final @NotNull BackgroundTaskIdentifier identify) {
        if (BackgroundTaskManager.removable.remove(identify))
            BackgroundTaskManager.tasks.remove(identify);
    }

    @Deprecated
    public static @Nullable CompletableFuture<?> get(final @NotNull BackgroundTaskIdentifier identify) {
        return BackgroundTaskManager.tasks.get(identify);
    }

    public static boolean cancel(final @NotNull BackgroundTaskIdentifier identify) {
        final CompletableFuture<?> future = BackgroundTaskManager.tasks.remove(identify);
        if (future != null) {
            if (future.isDone())
                return false;
            future.cancel(true);
            return true;
        }
        return false;
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
