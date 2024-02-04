package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import android.os.Environment;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.Callbacks.HCallbacks;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HProcessingInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractTasksManager<T extends AbstractTasksManager.AbstractTask, EW extends AbstractTasksManager.AbstractExtraWorking, ES extends AbstractTasksManager.AbstractExtraSuccess, EF extends AbstractTasksManager.AbstractExtraFailure> {
    protected final @NotNull EventExecutorGroup TaskExecutors =
            new DefaultEventExecutorGroup(3, new DefaultThreadFactory(this.getClass().getSimpleName() + "Executors"));

    public static final @NotNull HProcessingInitializer<PageTaskAdapter.@NotNull Types, @NotNull AbstractTasksManager<?, ?, ?, ?>> managers = new HProcessingInitializer<>("TaskManagers");

    /** @noinspection NonFinalStaticVariableUsedInClassInitialization*/
    protected static final @NotNull File BaseSaveDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist");
    protected static final @NotNull File BaseRecordsSaveDirectory = new File(AbstractTasksManager.BaseSaveDirectory, ".records");

    public enum TaskState {
        Working,
        Pending,
        Success,
        Failure,
    }

    public interface UpdateCallback<T, E> {
        @WorkerThread
        void onAdded(final @NotNull T task, final @NotNull E extra) throws Exception;
        @WorkerThread
        void onRemoved(final @NotNull T task, final @NotNull E extra) throws Exception;
    }

    protected final PageTaskAdapter.@NotNull Types type;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull EW> workingTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull EW> pendingTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull ES> successTasks;
    protected final @NotNull NavigableMap<@NotNull T, @NotNull EF> failureTasks;
    protected final @NotNull HCallbacks<UpdateCallback<T, EW>> workingTasksCallbacks = new HCallbacks<>();
    protected final @NotNull HCallbacks<UpdateCallback<T, EW>> pendingTasksCallbacks = new HCallbacks<>();
    protected final @NotNull HCallbacks<UpdateCallback<T, ES>> successTasksCallbacks = new HCallbacks<>();
    protected final @NotNull HCallbacks<UpdateCallback<T, EF>> failureTasksCallbacks = new HCallbacks<>();

    protected AbstractTasksManager(final PageTaskAdapter.@NotNull Types type) {
        super();
        this.type = type;
        this.workingTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
        this.pendingTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
        this.successTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
        this.failureTasks = new ConcurrentSkipListMap<>(Comparator.comparing(t -> t.time));
    }

    public PageTaskAdapter.@NotNull Types getType() {
        return this.type;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull EW> getWorkingTasks() {
        return this.workingTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull EW> getPendingTasks() {
        return this.pendingTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull ES> getSuccessTasks() {
        return this.successTasks;
    }
    public @NotNull NavigableMap<@NotNull T, @NotNull EF> getFailureTasks() {
        return this.failureTasks;
    }
    public @NotNull HCallbacks<UpdateCallback<T, EW>> getWorkingTasksCallbacks() {
        return this.workingTasksCallbacks;
    }
    public @NotNull HCallbacks<UpdateCallback<T, EW>> getPendingTasksCallbacks() {
        return this.pendingTasksCallbacks;
    }
    public @NotNull HCallbacks<UpdateCallback<T, ES>> getSuccessTasksCallbacks() {
        return this.successTasksCallbacks;
    }
    public @NotNull HCallbacks<UpdateCallback<T, EF>> getFailureTasksCallbacks() {
        return this.failureTasksCallbacks;
    }

    @WorkerThread
    @Contract(pure = true)
    protected final @NotNull String getRecordingFileIdentifier(final @NotNull T file) {
        return file.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(':', '.') + '-' + file.time.getNano() + ".bin.gz";
    }

    @WorkerThread
    @Contract(pure = true)
    public abstract @NotNull File getRecordingFile(final @NotNull T task);

    @WorkerThread
    protected void initialize(final @NotNull CActivity activity, final @NotNull File baseRecordsSaveDirectory) throws IOException, InterruptedException {
        PermissionUtil.readPermission(activity);
        HFileHelper.ensureDirectoryExist(baseRecordsSaveDirectory.toPath());
        final File[] files = baseRecordsSaveDirectory.listFiles(f -> f.isFile() && f.canRead());
        if (files != null)
            for (final File file: files)
                try {
                    try (final DataInputStream inputStream = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
                        final T task = this.parseTask(inputStream);
//                        final boolean warn = !this.getRecordingFile(task).getCanonicalFile().equals(file.getCanonicalFile());
//                        if (warn) {
//                            HLogManager.getInstance("TasksManager").log(HLogLevel.WARN, "Incorrect task recording path.", ParametersMap.create()
//                                    .add("file", file).add("task", task));
//                            Files.deleteIfExists(file.toPath());
//                        }
                        final TaskState _state = this.parseState(inputStream);
                        final TaskState state = _state == TaskState.Working ? TaskState.Pending : _state;
                        switch (state) {
                            case Pending -> {
                                final EW extra = this.parseExtraWorking(inputStream);
                                this.pendingTasks.put(task, extra);
//                                if (warn)
//                                    this.dumpWorkingTask(activity, task, extra);
                            }
                            case Success -> {
                                final ES extra = this.parseExtraSuccess(inputStream);
                                this.successTasks.put(task, extra);
//                                if (warn)
//                                    this.dumpSuccessTask(activity, task, extra);
                            }
                            case Failure -> {
                                final EF extra = this.parseExtraFailure(inputStream);
                                this.failureTasks.put(task, extra);
//                                if (warn)
//                                    this.dumpFailureTask(activity, task, extra);
                            }
                        }
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException exception) {
                        HLogManager.getInstance("TasksManager").log(HLogLevel.WARN, "Failed to parse task.", ParametersMap.create()
                                .add("type", this.type).add("file", file).add("exception", exception.getLocalizedMessage()));
                        Files.deleteIfExists(file.toPath());
                    }
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                }
        this.tryStartTask(activity);
    }

    @WorkerThread
    protected abstract @NotNull EW prepareTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException;

    @WorkerThread
    protected abstract @NotNull UnionPair<ES, EF> runTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull EW progress) throws Exception;

    @WorkerThread
    public void addTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        final EW pending = this.prepareTask(activity, task);
        this.pendingTasks.put(task, pending);
        this.pendingTasksCallbacks.callback(c -> c.onAdded(task, pending));
        this.dumpWorkingTask(activity, task, pending);
        HLogManager.getInstance("TasksManager").log(HLogLevel.INFO, "Adding task.",
                ParametersMap.create().add("type", this.type).add("task", task));
        this.tryStartTask(activity);
    }

    @WorkerThread
    protected synchronized void tryStartTask(final @NotNull Activity activity) {
        while (this.workingTasks.size() < 3) {
            final Map.Entry<T, EW> entry = this.pendingTasks.pollFirstEntry();
            if (entry == null) break;
            final T task = entry.getKey();
            final EW working = entry.getValue();
            this.pendingTasksCallbacks.callback(c -> c.onRemoved(task, working));
            this.workingTasks.put(task, working);
            this.workingTasksCallbacks.callback(c -> c.onAdded(task, working));
            this.TaskExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                final UnionPair<ES, EF> result = this.runTask(activity, task, working);
                this.workingTasks.remove(task, working);
                this.workingTasksCallbacks.callback(c -> c.onRemoved(task, working));
                if (result.isSuccess()) {
                    final ES extra = result.getT();
                    this.successTasks.put(task, extra);
                    this.successTasksCallbacks.callback(c -> c.onAdded(task, extra));
                    this.dumpSuccessTask(activity, task, extra);
                } else {
                    final EF extra = result.getE();
                    HLogManager.getInstance("TasksManager").log(HLogLevel.WARN, "Task failed.", ParametersMap.create()
                            .add("task", task).add("extra", extra));
                    this.failureTasks.put(task, extra);
                    this.failureTasksCallbacks.callback(c -> c.onAdded(task, extra));
                    this.dumpFailureTask(activity, task, extra);
                }
                if (ViewUtil.isActivityAvailable(activity))
                    this.tryStartTask(activity);
            })).addListener(Main.exceptionListenerWithToast());
        }
    }

    @WorkerThread
    protected void deleteTaskRecord(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        Files.deleteIfExists(this.getRecordingFile(task).toPath());
    }

    @WorkerThread
    public void removePendingTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        final EW pending = this.pendingTasks.remove(task);
        if (pending != null) {
            this.pendingTasksCallbacks.callback(c -> c.onRemoved(task, pending));
            this.deleteTaskRecord(activity, task);
        }
    }

    @WorkerThread
    public void removeSuccessTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        final ES success = this.successTasks.remove(task);
        if (success != null) {
            this.successTasksCallbacks.callback(c -> c.onRemoved(task, success));
            this.deleteTaskRecord(activity, task);
        }
    }

    @WorkerThread
    public void removeAllSuccessTask(final @NotNull Activity activity) throws IOException, InterruptedException {
        try {
            HMultiRunHelper.runConsumers(this.TaskExecutors, this.successTasks.entrySet(), HExceptionWrapper.wrapConsumer(e -> {
                this.successTasksCallbacks.callback(c -> c.onRemoved(e.getKey(), e.getValue()));
                this.deleteTaskRecord(activity, e.getKey());
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class, InterruptedException.class);
        }
        this.successTasks.clear();
    }

    @WorkerThread
    public void removeFailureTask(final @NotNull Activity activity, final @NotNull T task) throws IOException, InterruptedException {
        final EF failure = this.failureTasks.remove(task);
        if (failure != null) {
            this.failureTasksCallbacks.callback(c -> c.onRemoved(task, failure));
            this.deleteTaskRecord(activity, task);
        }
    }

    @WorkerThread
    public void removeAllFailureTask(final @NotNull Activity activity) throws IOException, InterruptedException {
        try {
            HMultiRunHelper.runConsumers(this.TaskExecutors, this.failureTasks.entrySet(), HExceptionWrapper.wrapConsumer(e -> {
                this.failureTasksCallbacks.callback(c -> c.onRemoved(e.getKey(), e.getValue()));
                this.deleteTaskRecord(activity, e.getKey());
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class, InterruptedException.class);
        }
        this.failureTasks.clear();
    }


    @WorkerThread
    protected abstract @NotNull T parseTask(final @NotNull DataInput inputStream) throws IOException;

    @WorkerThread
    protected @NotNull TaskState parseState(final @NotNull DataInput inputStream) throws IOException {
        if (inputStream.readBoolean())
            return TaskState.Pending;
        if (inputStream.readBoolean())
            return TaskState.Success;
        return TaskState.Failure;
    }

    @WorkerThread
    protected abstract @NotNull EW parseExtraWorking(final @NotNull DataInput inputStream) throws IOException;

    @WorkerThread
    protected abstract @NotNull ES parseExtraSuccess(final @NotNull DataInput inputStream) throws IOException;

    @WorkerThread
    protected abstract @NotNull EF parseExtraFailure(final @NotNull DataInput inputStream) throws IOException;

    @WorkerThread
    protected abstract void dumpTask(final @NotNull DataOutput outputStream, final @NotNull T task) throws IOException;

    @WorkerThread
    protected void dumpState(final @NotNull DataOutput outputStream, final @NotNull TaskState state) throws IOException {
        switch (state) {
            case Pending, Working -> outputStream.writeBoolean(true);
            case Success, Failure -> outputStream.writeBoolean(false);
        }
        switch (state) {
            case Success -> outputStream.writeBoolean(true);
            case Failure -> outputStream.writeBoolean(false);
        }
    }

    @WorkerThread
    protected abstract void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull EW extra) throws IOException;

    @WorkerThread
    protected abstract void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull ES extra) throws IOException;

    @WorkerThread
    protected abstract void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull EF extra) throws IOException;

    @WorkerThread
    protected void dumpWorkingTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull EW extra) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
            try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                this.dumpTask(outputStream, task);
                this.dumpState(outputStream, TaskState.Pending);
                this.dumpExtraWorking(outputStream, extra);
            }
        });
    }

    @WorkerThread
    protected void dumpSuccessTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull ES extra) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
            try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                this.dumpTask(outputStream, task);
                this.dumpState(outputStream, TaskState.Success);
                this.dumpExtraSuccess(outputStream, extra);
            }
        });
    }

    @WorkerThread
    protected void dumpFailureTask(final @NotNull Activity activity, final @NotNull T task, final @NotNull EF extra) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
            try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                this.dumpTask(outputStream, task);
                this.dumpState(outputStream, TaskState.Failure);
                this.dumpExtraFailure(outputStream, extra);
            }
        });
    }


    public static class AbstractTask {
        protected final @NotNull InetSocketAddress address;
        protected final @NotNull String username;
        protected final @NotNull ZonedDateTime time;
        protected final @NotNull String filename;
        protected final PageTaskAdapter.@NotNull Types source;

        public AbstractTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time, final @NotNull String filename, final PageTaskAdapter.@NotNull Types source) {
            super();
            this.address = address;
            this.username = username;
            this.time = time;
            this.filename = filename;
            this.source = source;
        }

        @WorkerThread
        protected AbstractTask(final @NotNull AbstractTask task) {
            super();
            this.address = task.address;
            this.username = task.username;
            this.time = task.time;
            this.filename = task.filename;
            this.source = task.source;
        }

        public @NotNull InetSocketAddress getAddress() {
            return this.address;
        }

        public @NotNull String getUsername() {
            return this.username;
        }

        public @NotNull ZonedDateTime getTime() {
            return this.time;
        }

        public @NotNull String getFilename() {
            return this.filename;
        }

        public PageTaskAdapter.@NotNull Types getSource() {
            return this.source;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final AbstractTasksManager.AbstractTask that)) return false;
            return this.address.equals(that.address) && this.username.equals(that.username) && this.time.equals(that.time) && this.filename.equals(that.filename) && this.source == that.source;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.address, this.username, this.time, this.filename, this.source);
        }

        @Override
        public @NotNull String toString() {
            return "AbstractTask{" +
                    "address=" + this.address +
                    ", username='" + this.username + '\'' +
                    ", time=" + this.time +
                    ", filename=" + this.filename +
                    ", source=" + this.source +
                    '}';
        }
    }

    protected static @NotNull AbstractTask parseAbstractTask(final @NotNull DataInput inputStream) throws IOException {
        final String host = inputStream.readUTF();
        final int port = inputStream.readInt();
        final InetSocketAddress address = new InetSocketAddress(host, port);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = ZonedDateTime.parse(inputStream.readUTF(), DateTimeFormatter.ISO_DATE_TIME).plusNanos(inputStream.readInt());
        final String filename = inputStream.readUTF();
        final PageTaskAdapter.Types source = PageTaskAdapter.Types.fromPosition(inputStream.readInt());
        return new AbstractTask(address, username, time, filename, source);
    }

    protected static void dumpAbstractTask(final @NotNull DataOutput outputStream, final @NotNull AbstractTask task) throws IOException {
        outputStream.writeUTF(task.address.getHostName());
        outputStream.writeInt(task.address.getPort());
        outputStream.writeUTF(task.username);
        outputStream.writeUTF(task.time.format(DateTimeFormatter.ISO_DATE_TIME));
        outputStream.writeInt(task.time.getNano());
        outputStream.writeUTF(task.filename);
        outputStream.writeInt(PageTaskAdapter.Types.toPosition(task.source));
    }

    protected static @NotNull FileLocation parseLocation(final @NotNull DataInput inputStream) throws IOException {
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        return new FileLocation(storage, id);
    }

    protected static void dumpLocation(final @NotNull DataOutput outputStream, final @NotNull FileLocation location) throws IOException {
        outputStream.writeUTF(FileLocationGetter.storage(location));
        outputStream.writeLong(FileLocationGetter.id(location));
    }

    public abstract static class AbstractExtraFailure {
    }

    public abstract static class AbstractExtraWorking {
        protected /*transient*/ boolean started = false;
        protected final /*transient*/ @NotNull HCallbacks<RunnableE> updateCallbacks = new HCallbacks<>();

        public void setStarted(final boolean started) {
            this.started = started;
        }

        public boolean isStarted() {
            return this.started;
        }

        public @NotNull HCallbacks<RunnableE> getUpdateCallbacks() {
            return this.updateCallbacks;
        }

        @Override
        public @NotNull String toString() {
            return "AbstractExtraWorking{" +
                    "started=" + this.started +
                    ", updateCallbacks=" + this.updateCallbacks +
                    '}';
        }
    }

    public abstract static class AbstractExtraSuccess {
    }


    public abstract static class AbstractSimpleExtraWorking extends AbstractExtraWorking {
        protected @Nullable InstantaneousProgressState state = null;

        public void setState(final @Nullable InstantaneousProgressState state) {
            this.state = state;
        }

        public @Nullable InstantaneousProgressState getState() {
            return this.state;
        }

        @Override
        public @NotNull String toString() {
            return "AbstractSimpleExtraWorking{" +
                    "state=" + this.state +
                    ", state=" + super.toString() +
                    '}';
        }
    }

    public abstract static class AbstractSimpleExtraSuccess extends AbstractExtraSuccess {

    }

    public abstract static class AbstractSimpleExtraFailure extends AbstractExtraFailure {
        protected final @NotNull VisibleFailureReason reason;

        protected AbstractSimpleExtraFailure(final @NotNull VisibleFailureReason reason) {
            super();
            this.reason = reason;
        }

        public @NotNull VisibleFailureReason getReason() {
            return this.reason;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final AbstractSimpleExtraFailure that)) return false;
            return Objects.equals(this.reason, that.reason);
        }

        @Override
        public int hashCode() {
            return FailureReasonGetter.hashCode(this.reason);
        }

        @Override
        public @NotNull String toString() {
            return "AbstractSimpleExtraFailure{" +
                    "reason=" + this.reason +
                    '}';
        }
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
                ", successTasks=" + this.successTasks +
                ", failureTasks=" + this.failureTasks +
                '}';
    }
}
