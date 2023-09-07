package com.xuxiaocheng.WList.Server.Handlers.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BackgroundTaskManager {
    private BackgroundTaskManager() {
        super();
    }

    public static final @NotNull EventExecutorGroup BackgroundExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("BackgroundExecutors"));

    public enum BackgroundTaskType {
        Driver,
        User,
    }

    public record BackgroundTaskIdentify(@NotNull BackgroundTaskType type, @NotNull String driver, @NotNull String task, @NotNull String identifier) {
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

    public static void background(final @NotNull BackgroundTaskIdentify identify, final @NotNull RunnableE runnable, final @Nullable ConsumerE<? super @Nullable Throwable> finisherAfterRun) {
        final boolean[] flag = {true};
        BackgroundTaskManager.TaskMap.computeIfAbsent(identify, k -> {
            flag[0] = false;
            return CompletableFuture.runAsync(() -> {
                try {
                    HExceptionWrapper.wrapRunnable(runnable, Objects.requireNonNullElse(finisherAfterRun, ConsumerE.emptyConsumerE()), false).run();
                } catch (final Throwable throwable) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
                } finally {
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

    public static void wait(final @NotNull BackgroundTaskIdentify identify) {
        final CompletableFuture<?> future = BackgroundTaskManager.TaskMap.get(identify);
        if (future != null)
            try {
                future.join();
            } catch (final CancellationException | CompletionException ignore) {
            }
    }

    public static <T> void backgroundWithLock(final @NotNull BackgroundTaskIdentify identify, final @NotNull Supplier<? extends @NotNull T> defaultLockSupplier, final @NotNull Class<T> lockClass,
                                              final @NotNull Predicate<? super @NotNull T> runningPredicate, final @NotNull RunnableE runnable, final @Nullable RunnableE finisherAfterRun) {
        final T lock = BackgroundTaskManager.getLock(identify, defaultLockSupplier, lockClass);
        synchronized (lock) {
            if (runningPredicate.test(lock)) {
                BackgroundTaskManager.cancel(identify);
                BackgroundTaskManager.background(identify, runnable, e -> {
                    BackgroundTaskManager.removeLock(identify);
                    if (finisherAfterRun != null)
                        finisherAfterRun.run();
                });
            }
        }
    }
}
