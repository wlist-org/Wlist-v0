package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO link to user interface.
public final class BackgroundTaskManager {
    private BackgroundTaskManager() {
        super();
    }

    public static final @NotNull EventExecutorGroup BackgroundExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("BackgroundExecutors"));

    public record BackgroundTaskIdentify(@NotNull String type, @NotNull String name) {
    }

    private static final @NotNull Map<@NotNull BackgroundTaskIdentify, @NotNull Object> LockMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T getLock(final @NotNull BackgroundTaskIdentify identify, final @NotNull Supplier<? extends @NotNull T> supplier, final @NotNull Class<T> lockClass) {
        final Object raw = BackgroundTaskManager.LockMap.computeIfAbsent(identify, k -> supplier.get());
        if (!lockClass.isAssignableFrom(raw.getClass()))
            throw new IllegalStateException("Conflict background task: Unexpected lock class!" + ParametersMap.create().add("identify", identify).add("expect", lockClass.toString()).add("real", raw.getClass().toString()));
        return (T) raw;
    }

    public static void removeLock(final @NotNull BackgroundTaskIdentify identify) {
        BackgroundTaskManager.LockMap.remove(identify);
    }

    private static final @NotNull Map<@NotNull BackgroundTaskIdentify, @NotNull CompletableFuture<?>> TaskMap = new ConcurrentHashMap<>();

    public static void background(final @NotNull BackgroundTaskIdentify identify, final @NotNull RunnableE runnable, final boolean removeLockAfterRun, final @NotNull ConsumerE<? super @Nullable Exception> finisherAfterRun) {
        final boolean[] flag = {true};
        BackgroundTaskManager.TaskMap.computeIfAbsent(identify, k -> {
            flag[0] = false;
            return CompletableFuture.runAsync(() -> {
                try {
                    HExceptionWrapper.wrapRunnable(runnable, finisherAfterRun, true).run();
                } catch (final Throwable throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
                } finally {
                    if (removeLockAfterRun)
                        BackgroundTaskManager.removeLock(identify);
                    BackgroundTaskManager.TaskMap.remove(identify);
                }
            }, BackgroundTaskManager.BackgroundExecutors);
        });
        if (flag[0])
            throw new IllegalStateException("Conflict background task: Task already exists!" + ParametersMap.create().add("identify", identify));
    }

    public static void cancel(final @NotNull BackgroundTaskIdentify identify) {
        final CompletableFuture<?> future = BackgroundTaskManager.TaskMap.remove(identify);
        if (future != null)
            future.cancel(true);
    }

    public static <T> void backgroundWithLock(final @NotNull BackgroundTaskIdentify identify, final @NotNull Supplier<? extends @NotNull T> defaultLockSupplier, final @NotNull Class<T> lockClass,
                                              final @NotNull Predicate<? super @NotNull T> runningPredicate, final @NotNull RunnableE runnable, final @NotNull RunnableE finisherAfterRun) {
        final T lock = BackgroundTaskManager.getLock(identify, defaultLockSupplier, lockClass);
        synchronized (lock) {
            if (runningPredicate.test(lock)) {
                BackgroundTaskManager.cancel(identify);
                BackgroundTaskManager.background(identify, runnable, true, e -> finisherAfterRun.run());
            }
        }
    }
}
