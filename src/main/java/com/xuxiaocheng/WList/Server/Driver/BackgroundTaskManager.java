package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;

// TODO link to user interface.
public final class BackgroundTaskManager {
    private BackgroundTaskManager() {
        super();
    }

    public static final @NotNull EventExecutorGroup BackgroundExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("BackgroundExecutors"));

    private static final @NotNull Map<@NotNull String, @NotNull Object> LockMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T getLock(final @NotNull String type, final @NotNull String name, final @NotNull Supplier<? extends @NotNull T> supplier, final @NotNull Class<T> tClass) {
        final Object raw = BackgroundTaskManager.LockMap.computeIfAbsent(type + ": " + name, k -> supplier.get());
        if (!tClass.isAssignableFrom(raw.getClass()))
            throw new IllegalStateException("Conflict background task: Unexpected lock class! type: " + type + ", name: '" + name + '\'');
        return (T) raw;
    }

    public static void removeLock(final @NotNull String type, final @NotNull String name) {
        BackgroundTaskManager.LockMap.remove(type + ": " + name);
    }

    private static final @NotNull Map<@NotNull String, @NotNull Future<?>> TaskMap = new ConcurrentHashMap<>();

    public static void background(final @NotNull String type, final @NotNull String name, final @NotNull RunnableE runnable, final boolean removeLock, final @NotNull RunnableE finisher) {
        final boolean[] flag = {true};
        BackgroundTaskManager.TaskMap.computeIfAbsent(type + ": " + name, k -> {
            flag[0] = false;
            return BackgroundTaskManager.BackgroundExecutors.submit(() -> {
                try {
                    try {
                        HExceptionWrapper.wrapRunnable(runnable).run();
                    } finally {
                        HExceptionWrapper.wrapRunnable(finisher).run();
                    }
                } finally {
                    if (removeLock)
                        BackgroundTaskManager.removeLock(type, name);
                    BackgroundTaskManager.TaskMap.remove(type + ": " + name);
                }
            });
        });
        if (flag[0]) {
            HExceptionWrapper.wrapRunnable(finisher).run();
            if (removeLock)
                BackgroundTaskManager.removeLock(type, name);
            throw new IllegalStateException("Conflict background task: Task already exists! type: " + type + ", name: '" + name + '\'');
        }
    }

    public static void cancel(final @NotNull String type, final @NotNull String name) {
        final Future<?> future = BackgroundTaskManager.TaskMap.remove(type + ": " + name);
        future.cancel(true);
    }
}
