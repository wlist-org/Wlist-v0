package com.xuxiaocheng.WListAndroid.Tasks;

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
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task.PageTaskAdapter;
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
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DownloadTasksManager extends AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadProgress> {
    protected DownloadTasksManager() {
        super(Comparator.comparing(t -> t.time));
    }

    @WorkerThread
    public static @NotNull DownloadTasksManager getInstance() {
        return (DownloadTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Download);
    }

    @WorkerThread
    public static void initializeIfNotInitializing(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.initializeIfNotInitializing(PageTaskAdapter.Types.Download, () -> {
            final DownloadTasksManager manager = new DownloadTasksManager();
            PermissionUtil.readPermission(activity);
            HFileHelper.ensureDirectoryExist(manager.DownloadRecordsSaveDirectory.toPath());
            final File[] files = manager.DownloadRecordsSaveDirectory.listFiles();
            if (files != null)
                for (final File file: files)
                    try (final DataInputStream inputStream = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
                        final DownloadTask task = DownloadTasksManager.parseTask(inputStream);
                        if (inputStream.readBoolean())
                            manager.addPendingTask(activity, task, false);
                        else if (inputStream.readBoolean())
                            manager.addSuccessfulTask(activity, task, false);
                        else
                            manager.addFailedTask(activity, task, AbstractTasksManager.parseReason(inputStream), false);
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                    }
            manager.tryStartTask(activity);
            return manager;
        }, null);
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

    private final @NotNull File DownloadRecordsSaveDirectory = new File(this.BaseRecordsSaveDirectory, "Downloads");

    @WorkerThread
    private @NotNull File getSaveFile(final @NotNull DownloadTask task) {
        // TODO safe filename.
        return new File(this.BaseSaveDirectory, FileLocationGetter.storage(task.location) + File.separator + FileLocationGetter.id(task.location) + " - " + task.filename);
    }

    @WorkerThread
    private @NotNull File getRecordingFile(final @NotNull DownloadTask task) {
        return new File(this.DownloadRecordsSaveDirectory, FileLocationGetter.storage(task.location) + " - " + FileLocationGetter.id(task.location) + ".bin");
    }

    @Override
    protected @NotNull DownloadProgress prepareTask(final @NotNull Activity activity, final @NotNull DownloadTask task) throws IOException, InterruptedException {
        final File file = this.getSaveFile(task);
        PermissionUtil.writePermission(activity);
        HFileHelper.ensureFileAccessible(file, true);
        HLogManager.getInstance("ClientLogger").log(HLogLevel.LESS, "Downloading.",
                ParametersMap.create().add("address", task.address).add("username", task.username).add("location", task.location).add("file", file));
        return new DownloadProgress();
    }

    @Override
    protected @Nullable VisibleFailureReason runTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final @NotNull DownloadProgress progress) {
        final File file = this.getSaveFile(task);
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

        public DownloadTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time, final @NotNull FileLocation location, final @NotNull String filename) {
            super(address, username, time);
            this.location = location;
            this.filename = filename;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final DownloadTask that)) return false;
            if (!super.equals(o)) return false;
            return FileLocationGetter.equals(this.location, that.location) && this.filename.equals(that.filename);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.filename);
        }

        @Override
        public @NotNull String toString() {
            return "DownloadTask{" +
                    "location=" + this.location +
                    ", filename=" + this.filename +
                    '}';
        }
    }

    public static class DownloadProgress {
        protected InstantaneousProgressState state;
        protected List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress;

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

    protected static @NotNull DownloadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final FileLocation location = new FileLocation(storage, id);
        final String filename = inputStream.readUTF();
        return new DownloadTask(address, username, time, location, filename);
    }

    protected static void dumpTask(final @NotNull DataOutput outputStream, final @NotNull DownloadTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
        outputStream.writeUTF(FileLocationGetter.storage(task.location));
        outputStream.writeLong(FileLocationGetter.id(task.location));
        outputStream.writeUTF(task.filename);
    }

    @Override
    public @NotNull String toString() {
        return "DownloadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
