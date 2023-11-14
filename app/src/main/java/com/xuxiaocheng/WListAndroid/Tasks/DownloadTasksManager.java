package com.xuxiaocheng.WListAndroid.Tasks;

import android.app.Activity;
import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
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
import java.time.format.DateTimeFormatter;
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

    private static final @NotNull HInitializer<DownloadTasksManager> Instance = new HInitializer<>("DownloadTasksManager");

    @AnyThread
    public static @NotNull DownloadTasksManager getInstance() {
        return DownloadTasksManager.Instance.getInstance();
    }

    @WorkerThread
    public static void initialize(final @NotNull CActivity activity) throws InterruptedException, IOException {
        final DownloadTasksManager manager = new DownloadTasksManager();
        DownloadTasksManager.Instance.initialize(manager);
        PermissionUtil.readPermission(activity);
        HFileHelper.ensureDirectoryExist(manager.DownloadRecordsSaveDirectory.toPath());
        final File[] files = manager.DownloadRecordsSaveDirectory.listFiles();
        if (files == null) return;
        for (final File file: files)
            try (final DataInputStream inputStream = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
                final DownloadTask task = DownloadTasksManager.parseTask(inputStream);
                if (inputStream.readBoolean())
                    manager.addWorkingTask(activity, activity.address(), activity.username(), task, false);
                else if (inputStream.readBoolean())
                    manager.addSuccessfulTask(activity, task, false);
                else
                    manager.addFailedTask(activity, task, DownloadTasksManager.parseReason(inputStream), false);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
    }

    private final @NotNull File DownloadRecordsSaveDirectory = new File(this.BaseRecordsSaveDirectory, "Downloads");

    @WorkerThread
    private @NotNull File getSaveFile(final @NotNull DownloadTask task) {
        // TODO safe filename.
        return new File(this.BaseSaveDirectory, task.storage + File.separator + FileInformationGetter.id(task.information) + " - " + FileInformationGetter.name(task.information));
    }

    @WorkerThread
    private @NotNull File getRecordingFile(final @NotNull DownloadTask task) {
        return new File(this.DownloadRecordsSaveDirectory, task.storage + " - " + FileInformationGetter.id(task.information) + ".bin");
    }

    @Override
    public void addWorkingTask(final @NotNull Activity activity, final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull DownloadTask task, final boolean serializing) throws IOException, InterruptedException {
        final File file = this.getSaveFile(task);
        PermissionUtil.writePermission(activity);
        HFileHelper.ensureFileAccessible(file, true);
        final FileLocation location = new FileLocation(task.storage, FileInformationGetter.id(task.information));
        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
                ParametersMap.create().add("address", address).add("location", location).add("file", file));
        final DownloadProgress progress = new DownloadProgress();
        this.workingTasks.put(task, progress);
        if (serializing)
            HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
                try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                    DownloadTasksManager.dumpTask(outputStream, task);
                    outputStream.writeBoolean(true);
                }
            });
        this.updatedTasks.add(task);
        AbstractTasksManager.TaskExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
            VisibleFailureReason reason;
            try {
                reason = FilesAssistant.download(address, username, location, file, PredicateE.truePredicate(), (s, p) -> {
                    progress.state = s;
                    progress.progress = p;
                    this.updatedTasks.add(task);
                });
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
                reason = new VisibleFailureReason(FailureKind.Others, location, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
            }
            this.workingTasks.remove(task);
            if (reason != null)
                this.addFailedTask(activity, task, reason, true);
            else
                this.addSuccessfulTask(activity, task, true);
        })).addListener(Main.exceptionListenerWithToast(activity));
    }

    @Override
    public void addSuccessfulTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final boolean serializing) throws IOException, InterruptedException {
        this.successfulTasks.add(task);
        if (serializing) {
            PermissionUtil.writePermission(activity);
            HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
                try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                    DownloadTasksManager.dumpTask(outputStream, task);
                    outputStream.writeBoolean(false);
                    outputStream.writeBoolean(true);
                }
            });
        }
        this.updatedTasks.add(task);
    }

    @Override
    public void addFailedTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final @NotNull VisibleFailureReason reason, final boolean serializing) throws IOException, InterruptedException {
        this.failedTasks.put(task, reason);
        if (serializing) {
            PermissionUtil.writePermission(activity);
            HFileHelper.writeFileAtomically(this.getRecordingFile(task), s -> {
                try (final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(s))) {
                    DownloadTasksManager.dumpTask(outputStream, task);
                    outputStream.writeBoolean(false);
                    outputStream.writeBoolean(false);
                    DownloadTasksManager.dumpReason(outputStream, reason);
                }
            });
        }
        this.updatedTasks.add(task);
    }

    public static class DownloadTask {
        protected final @NotNull String storage;
        protected final @NotNull VisibleFileInformation information;
        protected final @NotNull ZonedDateTime time;

        public DownloadTask(final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull ZonedDateTime time) {
            super();
            this.storage = storage;
            this.information = information;
            this.time = time;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final DownloadTask that)) return false;
            return this.storage.equals(that.storage) && FileInformationGetter.id(this.information) == FileInformationGetter.id(that.information);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.storage, this.information);
        }

        @Override
        public @NotNull String toString() {
            return "DownloadTask{" +
                    "storage='" + this.storage + '\'' +
                    ", information=" + this.information +
                    ", time=" + this.time +
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
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final long parentId = inputStream.readLong();
        final String name = inputStream.readUTF();
        final boolean isDirectory = inputStream.readBoolean();
        final long size = inputStream.readLong();
        final ZonedDateTime createTime = inputStream.readBoolean() ? null : ZonedDateTime.parse(inputStream.readUTF(), DateTimeFormatter.ISO_DATE_TIME);
        final ZonedDateTime updateTime = inputStream.readBoolean() ? null : ZonedDateTime.parse(inputStream.readUTF(), DateTimeFormatter.ISO_DATE_TIME);
        final VisibleFileInformation information = new VisibleFileInformation(id, parentId, name, isDirectory, size, createTime, updateTime);
        final ZonedDateTime time = ZonedDateTime.parse(inputStream.readUTF(), DateTimeFormatter.ISO_DATE_TIME);
        return new DownloadTask(storage, information, time);
    }

    protected static void dumpTask(final @NotNull DataOutput outputStream, final @NotNull DownloadTask task) throws IOException {
        outputStream.writeUTF(task.storage);
        outputStream.writeLong(FileInformationGetter.id(task.information));
        outputStream.writeLong(FileInformationGetter.parentId(task.information));
        outputStream.writeUTF(FileInformationGetter.name(task.information));
        outputStream.writeBoolean(FileInformationGetter.isDirectory(task.information));
        outputStream.writeLong(FileInformationGetter.size(task.information));
        final ZonedDateTime createTime = FileInformationGetter.createTime(task.information);
        outputStream.writeBoolean(createTime == null);
        if (createTime != null)
            outputStream.writeUTF(createTime.format(DateTimeFormatter.ISO_DATE_TIME));
        final ZonedDateTime updateTime = FileInformationGetter.updateTime(task.information);
        outputStream.writeBoolean(updateTime == null);
        if (updateTime != null)
            outputStream.writeUTF(updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        outputStream.writeUTF(task.time.format(DateTimeFormatter.ISO_DATE_TIME));
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
        return "DownloadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
