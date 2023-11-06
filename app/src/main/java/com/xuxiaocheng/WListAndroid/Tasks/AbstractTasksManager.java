package com.xuxiaocheng.WListAndroid.Tasks;

import android.app.Activity;
import android.os.Environment;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class AbstractTasksManager<T, P> {
    public static final @NotNull EventExecutorGroup TaskExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("TaskExecutors"));

    protected final @NotNull File BaseSaveDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist");
    protected final @NotNull File BaseRecordsSaveDirectory = new File(this.BaseSaveDirectory, ".records");

    protected AbstractTasksManager(final @NotNull Comparator<T> comparator) {
        super();
        this.workingTasks = new ConcurrentSkipListMap<>(comparator);
        this.successfulTasks = new ConcurrentSkipListSet<>(comparator);
        this.failedTasks = new ConcurrentSkipListMap<>(comparator);
        this.unmodifiableWorkingTasks = Collections.unmodifiableNavigableMap(this.workingTasks);
    }

    protected final @NotNull NavigableMap<@NotNull T, @NotNull P> workingTasks;
    protected final @NotNull NavigableSet<@NotNull T> successfulTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull VisibleFailureReason> failedTasks;

    protected final @NotNull Set<@NotNull T> updatedTasks = ConcurrentHashMap.newKeySet();
    public @NotNull Set<@NotNull T> getUpdatedTasks() {
        return this.updatedTasks;
    }

    private final @NotNull @UnmodifiableView NavigableMap<@NotNull T, @NotNull P> unmodifiableWorkingTasks;
    public @NotNull @UnmodifiableView NavigableMap<@NotNull T, @NotNull P> getUnmodifiableWorkingTasks() {
        return this.unmodifiableWorkingTasks;
    }
    public @NotNull NavigableSet<@NotNull T> getSuccessfulTasks() {
        return this.successfulTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull VisibleFailureReason> getFailedTasks() {
        return this.failedTasks;
    }

    @WorkerThread
    public abstract void addWorkingTask(final @NotNull Activity activity, final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull T task, final boolean serializing) throws IOException, InterruptedException;

    @WorkerThread
    public abstract void addSuccessfulTask(final @NotNull Activity activity, final @NotNull T task, final boolean serializing) throws IOException, InterruptedException;

    @WorkerThread
    public abstract void addFailedTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull VisibleFailureReason reason, final boolean serializing) throws IOException, InterruptedException;

}
