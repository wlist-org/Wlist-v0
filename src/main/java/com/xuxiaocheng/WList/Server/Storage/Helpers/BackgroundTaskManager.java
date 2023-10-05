package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

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

    public static void background(final @NotNull BackgroundTaskIdentifier identify, final @NotNull Runnable runnable, final boolean removeLock,
                                  final @Nullable Runnable onConflictWhenCompletedIgnoreOthersParams) {
        final CountDownLatch latch = new CountDownLatch(1);
        BackgroundTaskManager.tasks.compute(identify, (k, o) -> {
            if (o != null) {
                if (onConflictWhenCompletedIgnoreOthersParams == null)
                    return o;
                return o.whenComplete((v, e) -> onConflictWhenCompletedIgnoreOthersParams.run())
                        .exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            if (!removeLock)
                BackgroundTaskManager.removable.add(identify);
            return CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                latch.await(); // wait to prevent 'java.lang.IllegalStateException: Recursive update'.
                runnable.run();
            }), BackgroundTaskManager.BackgroundExecutors).whenComplete((v, e) -> {
                if (removeLock)
                    BackgroundTaskManager.tasks.remove(identify);
                if (e != null)
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
            });
        });
        latch.countDown();
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
