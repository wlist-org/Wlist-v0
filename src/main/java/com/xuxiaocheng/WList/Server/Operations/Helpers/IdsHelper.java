package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IdsHelper {
    private IdsHelper() {
        super();
    }

    public static final @NotNull EventExecutorGroup CleanerExecutors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("CleanerExecutors"));

    public static @NotNull String randomTimerId() {
        return Long.toString(System.currentTimeMillis(), 36) + HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.AnyWords);
    }


    private static final @NotNull Map<@NotNull String, @NotNull ProgressBar> progresses = new ConcurrentHashMap<>();

    public static @NotNull ProgressBar setProgressBar(final @NotNull String id) {
        final ProgressBar progress = new ProgressBar();
        if (IdsHelper.progresses.putIfAbsent(id, progress) != null)
            throw new IllegalStateException("Conflict progress id." + ParametersMap.create().add("id", id));
        return progress;
    }

    public static @Nullable ProgressBar getProgressBar(final @NotNull String id) {
        return IdsHelper.progresses.get(id);
    }

    public static void removeProgressBar(final @NotNull String id) {
        IdsHelper.progresses.remove(id);
    }
}
