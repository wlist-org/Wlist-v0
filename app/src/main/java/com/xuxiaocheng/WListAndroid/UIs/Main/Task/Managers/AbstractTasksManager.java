package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import android.os.Environment;
import androidx.annotation.WorkerThread;
import com.qw.soul.permission.PermissionTools;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HProcessingInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class AbstractTasksManager<T extends AbstractTasksManager.AbstractTask, P> {
    protected final @NotNull EventExecutorGroup TaskExecutors =
            new DefaultEventExecutorGroup(3, new DefaultThreadFactory(this.getClass().getSimpleName() + "Executors"));

    public static final @NotNull HProcessingInitializer<PageTaskAdapter.@NotNull Types, @NotNull AbstractTasksManager<?, ?>> managers = new HProcessingInitializer<>("TaskManagers");

    protected final @NotNull File BaseSaveDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist");
    protected final @NotNull File BaseRecordsSaveDirectory = new File(this.BaseSaveDirectory, ".records");

    public enum TaskState {
        Working,
        Pending,
        Successful,
        Failed,
    }

    protected final PageTaskAdapter.@NotNull Types type;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull P> workingTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull P> pendingTasks;
    protected final @NotNull NavigableSet<@NotNull T> successfulTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull VisibleFailureReason> failedTasks;

    protected AbstractTasksManager(final PageTaskAdapter.@NotNull Types type) {
        super();
        this.type = type;
        this.workingTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
        this.pendingTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
        this.successfulTasks = new ConcurrentSkipListSet<>(Comparator.comparing(t -> t.time));
        this.failedTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
    }

    protected final @NotNull Set<@NotNull T> updatedTasks = ConcurrentHashMap.newKeySet();
    public @NotNull Set<@NotNull T> getUpdatedTasks() {
        return this.updatedTasks;
    }

    public @NotNull @UnmodifiableView NavigableMap<@NotNull T, @NotNull P> getWorkingTasks() {
        return this.workingTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull P> getPendingTasks() {
        return this.pendingTasks;
    }
    public @NotNull NavigableSet<@NotNull T> getSuccessfulTasks() {
        return this.successfulTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull VisibleFailureReason> getFailedTasks() {
        return this.failedTasks;
    }

    public synchronized void tryStartTask(final @NotNull Activity activity) {
        while (this.workingTasks.size() < 3) {
            final Map.Entry<T, P> entry = this.pendingTasks.pollFirstEntry();
            if (entry == null) break;
            final T task = entry.getKey();
            final P progress = entry.getValue();
            this.workingTasks.put(task, progress);
            this.updatedTasks.add(task);
            this.TaskExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                final VisibleFailureReason reason = this.runTask(activity, task, progress);
                this.workingTasks.remove(task, progress);
                if (reason != null)
                    this.addFailedTask(activity, task, reason, true);
                else
                    this.addSuccessfulTask(activity, task, true);
                if (PermissionTools.isActivityAvailable(activity))
                    this.tryStartTask(activity);
            })).addListener(Main.exceptionListenerWithToast(activity));
        }
    }

    @WorkerThread
    protected abstract @NotNull P prepareTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException;

    @WorkerThread
    protected abstract void serializeTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull TaskState state, final @Nullable VisibleFailureReason failureReason) throws IOException, InterruptedException;

    @WorkerThread
    protected abstract @Nullable VisibleFailureReason runTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull P progress) throws Exception;

    @WorkerThread
    protected void addPendingTask(final @NotNull Activity activity, final @NotNull T task, final boolean serializing) throws IOException, InterruptedException {
        final P progress = this.prepareTask(activity, task);
        this.pendingTasks.put(task, progress);
        if (serializing)
            this.serializeTask(activity, task, TaskState.Pending, null);
        this.updatedTasks.add(task);
    }

    @WorkerThread
    protected void addSuccessfulTask(final @NotNull Activity activity, final @NotNull T task, final boolean serializing) throws IOException, InterruptedException {
        this.successfulTasks.add(task);
        if (serializing)
            this.serializeTask(activity, task, TaskState.Successful, null);
        this.updatedTasks.add(task);
    }

    @WorkerThread
    protected void addFailedTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull VisibleFailureReason reason, final boolean serializing) throws IOException, InterruptedException {
        this.failedTasks.put(task, reason);
        if (serializing)
            this.serializeTask(activity, task, TaskState.Failed, reason);
        this.updatedTasks.add(task);
    }

    @WorkerThread
    public void addTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        this.addPendingTask(activity, task, true);
        this.tryStartTask(activity);
        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Adding task.",
                ParametersMap.create().add("type", this.type).add("task", task));
    }

    protected abstract static class AbstractTask {
        protected final @NotNull InetSocketAddress address;
        protected final @NotNull String username;
        protected final @NotNull ZonedDateTime time;

        protected AbstractTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time) {
            super();
            this.address = address;
            this.username = username;
            this.time = time;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final AbstractTasksManager.AbstractTask that)) return false;
            return this.address.equals(that.address) && this.username.equals(that.username) && this.time.equals(that.time);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.address, this.username, this.time);
        }

        @Override
        public @NotNull String toString() {
            return "AbstractTask{" +
                    "address=" + this.address +
                    ", username='" + this.username + '\'' +
                    ", time=" + this.time +
                    '}';
        }
    }

    protected static @NotNull InetSocketAddress parseAddress(final @NotNull DataInput inputStream) throws IOException {
        final String host = inputStream.readUTF();
        final int port = inputStream.readInt();
        return new InetSocketAddress(host, port);
    }

    protected static void dumpAddress(final @NotNull DataOutput outputStream, final @NotNull InetSocketAddress address) throws IOException {
        outputStream.writeUTF(address.getHostName());
        outputStream.writeInt(address.getPort());
    }

    protected static @NotNull ZonedDateTime parseTime(final @NotNull DataInput inputStream) throws IOException {
        return ZonedDateTime.parse(inputStream.readUTF(), DateTimeFormatter.ISO_DATE_TIME);
    }

    protected static void dumpTime(final @NotNull DataOutput outputStream, final @NotNull ZonedDateTime time) throws IOException {
        outputStream.writeUTF(time.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    protected static @NotNull VisibleFailureReason parseReason(final @NotNull DataInput inputStream) throws IOException {
        final FailureKind kind = FailureKind.of(inputStream.readUTF());
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final FileLocation location = new FileLocation(storage, id);
        final String message = inputStream.readUTF();
        return new VisibleFailureReason(kind, location, message);
    }

    protected static void dumpReason(final @NotNull DataOutput outputStream, final @NotNull VisibleFailureReason reason) throws IOException {
        outputStream.writeUTF(FailureReasonGetter.kind(reason).name());
        outputStream.writeUTF(FileLocationGetter.storage(FailureReasonGetter.location(reason)));
        outputStream.writeLong(FileLocationGetter.id(FailureReasonGetter.location(reason)));
        outputStream.writeUTF(FailureReasonGetter.message(reason));
    }

    @Override
    public @NotNull String toString() {
        return "AbstractTasksManager{" +
                "workingTasks=" + this.workingTasks +
                ", pendingTasks=" + this.pendingTasks +
                ", successfulTasks=" + this.successfulTasks +
                ", failedTasks=" + this.failedTasks +
                ", updatedTasks=" + this.updatedTasks +
                '}';
    }
}
