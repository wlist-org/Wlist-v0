package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DownloadTasksManager extends AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadProgress> {
    protected DownloadTasksManager() {
        super(PageTaskAdapter.Types.Download);
    }

    @WorkerThread
    public static @NotNull DownloadTasksManager getInstance() {
        return (DownloadTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Download);
    }

    @WorkerThread
    public static void initializeIfNotInitializing(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.initializeIfNotInitializing(PageTaskAdapter.Types.Download, () -> {
            final DownloadTasksManager manager = new DownloadTasksManager();
            manager.initialize(activity);
            manager.tryStartTask(activity);
            return manager;
        }, null);
    }

    private static final @NotNull File DownloadRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Downloads");

    @Override
    protected void initialize(final @NotNull CActivity activity) throws IOException, InterruptedException {
        PermissionUtil.readPermission(activity);
        HFileHelper.ensureDirectoryExist(DownloadTasksManager.DownloadRecordsSaveDirectory.toPath());
        final File[] files = DownloadTasksManager.DownloadRecordsSaveDirectory.listFiles(f -> f.isFile() && f.canRead());
        if (files != null)
            for (final File file: files)
                try {
                    try (final DataInputStream inputStream = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
                        final DownloadTask task = DownloadTasksManager.parseTask(inputStream);
                        final boolean warn = !this.getRecordingFile(task).getCanonicalFile().equals(file.getCanonicalFile());
                        if (warn) {
                            HLogManager.getInstance("TasksManager").log(HLogLevel.WARN, "Incorrect task recording path.", ParametersMap.create()
                                    .add("file", file).add("task", task));
                            Files.deleteIfExists(file.toPath());
                        }
                        if (inputStream.readBoolean())
                            this.addPendingTask(activity, task, warn);
                        else if (inputStream.readBoolean())
                            this.addSuccessfulTask(activity, task, warn);
                        else
                            this.addFailedTask(activity, task, AbstractTasksManager.parseReason(inputStream), warn);
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException exception) {
                        HLogManager.getInstance("TasksManager").log(HLogLevel.WARN, "Failed to parse task.", ParametersMap.create()
                                .add("file", file).add("exception", exception.getLocalizedMessage()));
                        Files.deleteIfExists(file.toPath());
                    }
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                }
        this.updatedTasks.clear();
    }

    @Override
    protected void serializeTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final @NotNull TaskState state, final @Nullable VisibleFailureReason failureReason) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
            try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                DownloadTasksManager.dumpTask(outputStream, task);
                // No Working.
                switch (state) {
                    case Pending -> outputStream.writeBoolean(true);
                    case Successful, Failed -> outputStream.writeBoolean(false);
                }
                switch (state) {
                    case Successful -> outputStream.writeBoolean(true);
                    case Failed -> outputStream.writeBoolean(false);
                }
                if (state == TaskState.Failed) {
                    assert failureReason != null;
                    AbstractTasksManager.dumpReason(outputStream, failureReason);
                }
            }
        });
    }

    @Override
    protected void removeNotWorkingTask(final @NotNull Activity activity, final @NotNull DownloadTask task) throws IOException, InterruptedException {
        PermissionUtil.writePermission(activity);
        Files.deleteIfExists(this.getRecordingFile(task).toPath());
    }

    @WorkerThread
    protected static @NotNull File getSaveFile(final @NotNull DownloadTask task) {
        final File storage = new File(AbstractTasksManager.BaseSaveDirectory, FileLocationGetter.storage(task.location));
        // TODO safe filename.
        return new File(storage, FileLocationGetter.id(task.location) + " - " + task.filename);
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull DownloadTask task) {
        return new File(DownloadTasksManager.DownloadRecordsSaveDirectory, FileLocationGetter.storage(task.location) + " - " + FileLocationGetter.id(task.location) + ".bin.gz");
    }

    @Override
    protected @NotNull DownloadProgress prepareTask(final @NotNull Activity activity, final @NotNull DownloadTask task) throws IOException, InterruptedException {
        final File file = DownloadTasksManager.getSaveFile(task);
        PermissionUtil.writePermission(activity);
        HFileHelper.ensureFileAccessible(file, true);
        return new DownloadProgress();
    }

    @Override
    protected @Nullable VisibleFailureReason runTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final @NotNull DownloadProgress progress) {
        final File file = DownloadTasksManager.getSaveFile(task);
        try {
            return FilesAssistant.download(task.address, task.username, task.location, file, PredicateE.truePredicate(), (s, p) -> {
                progress.state = s;
                progress.progress = p;
                this.updatedTasks.add(task);
            });
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            return new VisibleFailureReason(FailureKind.Others, task.location, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
        }
    }

    public static class DownloadTask extends AbstractTask {
        protected final @NotNull FileLocation location;
        protected final @NotNull String filename;
        protected final PageTaskAdapter.@NotNull Types source;
        protected final @NotNull File savePath;

        @WorkerThread
        public DownloadTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time, final @NotNull FileLocation location, final @NotNull String filename, final PageTaskAdapter.@NotNull Types source) {
            super(address, username, time);
            this.location = location;
            this.filename = filename;
            this.source = source;
            this.savePath = DownloadTasksManager.getSaveFile(this);
        }

        public @NotNull FileLocation getLocation() {
            return this.location;
        }

        public @NotNull String getFilename() {
            return this.filename;
        }

        public PageTaskAdapter.@NotNull Types getSource() {
            return this.source;
        }

        public @NotNull File getSavePath() {
            return this.savePath;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final DownloadTask that)) return false;
            if (!super.equals(o)) return false;
            return FileLocationGetter.equals(this.location, that.location) && this.filename.equals(that.filename) && this.source == that.source;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.filename, this.source);
        }

        @Override
        public @NotNull String toString() {
            return "DownloadTask{" +
                    "location=" + this.location +
                    ", filename=" + this.filename +
                    ", source=" + this.source +
                    '}';
        }
    }

    public static class DownloadProgress {
        protected InstantaneousProgressState state = null;
        protected List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress = null;

        public @Nullable InstantaneousProgressState getState() {
            return this.state;
        }

        public @Nullable List<Pair.ImmutablePair<AtomicLong, Long>> getProgress() {
            return this.progress;
        }

        @Override
        public @NotNull String toString() {
            return "DownloadProgress{" +
                    "state=" + this.state +
                    ", progress=" + this.progress +
                    '}';
        }
    }

    @WorkerThread
    protected static @NotNull DownloadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final FileLocation location = new FileLocation(storage, id);
        final String filename = inputStream.readUTF();
        final PageTaskAdapter.Types source = PageTaskAdapter.Types.fromPosition(inputStream.readInt());
        return new DownloadTask(address, username, time, location, filename, source);
    }

    @WorkerThread
    protected static void dumpTask(final @NotNull DataOutput outputStream, final @NotNull DownloadTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
        outputStream.writeUTF(FileLocationGetter.storage(task.location));
        outputStream.writeLong(FileLocationGetter.id(task.location));
        outputStream.writeUTF(task.filename);
        outputStream.writeInt(PageTaskAdapter.Types.toPosition(task.source));
    }

    @Override
    public @NotNull String toString() {
        return "DownloadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
