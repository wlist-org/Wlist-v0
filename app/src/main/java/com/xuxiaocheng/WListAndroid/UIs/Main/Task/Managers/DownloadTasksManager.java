package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.Callbacks.HCallbacks;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTasksManager extends AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking, DownloadTasksManager.DownloadSuccess, DownloadTasksManager.DownloadFailure> {
    protected DownloadTasksManager() {
        super(PageTaskAdapter.Types.Download);
    }

    protected static final @NotNull File DownloadRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Downloads");

    @WorkerThread
    public static @NotNull DownloadTasksManager getInstance() {
        return (DownloadTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Download);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Download, () -> {
            final DownloadTasksManager manager = new DownloadTasksManager();
            manager.initialize(activity, DownloadTasksManager.DownloadRecordsSaveDirectory);
            return manager;
        }, null);
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
    protected @NotNull DownloadTasksManager.DownloadWorking prepareTask(final @NotNull Activity activity, final @NotNull DownloadTask task) throws IOException, InterruptedException {
        final File file = DownloadTasksManager.getSaveFile(task);
        PermissionUtil.writePermission(activity);
        HFileHelper.ensureFileAccessible(file, true);
        return new DownloadWorking();
    }

    @Override
    protected @NotNull UnionPair<DownloadSuccess, DownloadFailure> runTask(final @NotNull Activity activity, final @NotNull DownloadTask task, final @NotNull DownloadWorking progress) {
        final File file = DownloadTasksManager.getSaveFile(task);
        try {
            progress.started = true;
            final VisibleFailureReason reason = FilesAssistant.download(task.address, task.username, task.location, file, PredicateE.truePredicate(), (s, p) -> {
                progress.state = s;
                progress.progress = p;
                progress.updateCallbacks.callback(RunnableE::run);
            });
            return reason == null ? UnionPair.ok(new DownloadSuccess()) : UnionPair.fail(new DownloadFailure(reason));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            final VisibleFailureReason reason = new VisibleFailureReason(FailureKind.Others, task.location, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
            return UnionPair.fail(new DownloadFailure(reason));
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

    @Override
    protected @NotNull DownloadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
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

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull DownloadTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
        outputStream.writeUTF(FileLocationGetter.storage(task.location));
        outputStream.writeLong(FileLocationGetter.id(task.location));
        outputStream.writeUTF(task.filename);
        outputStream.writeInt(PageTaskAdapter.Types.toPosition(task.source));
    }

    public static class DownloadWorking {
        protected boolean started = false;
        protected InstantaneousProgressState state = null;
        protected List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress = null;
        protected final @NotNull HCallbacks<RunnableE> updateCallbacks = new HCallbacks<>();

        public boolean isStarted() {
            return this.started;
        }

        public @Nullable InstantaneousProgressState getState() {
            return this.state;
        }

        public @Nullable List<Pair.ImmutablePair<AtomicLong, Long>> getProgress() {
            return this.progress;
        }

        public @NotNull HCallbacks<RunnableE> getUpdateCallbacks() {
            return this.updateCallbacks;
        }

        @Override
        public @NotNull String toString() {
            return "DownloadWorking{" +
                    "started=" + this.started +
                    ", state=" + this.state +
                    ", progress=" + this.progress +
                    '}';
        }
    }

    @Override
    protected @NotNull DownloadWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new DownloadWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull DownloadWorking extra) {
    }

    public static class DownloadSuccess {
    }

    @Override
    protected @NotNull DownloadSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new DownloadSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull DownloadSuccess extra) {
    }

    public static class DownloadFailure {
        protected final @NotNull VisibleFailureReason reason;

        protected DownloadFailure(final @NotNull VisibleFailureReason reason) {
            super();
            this.reason = reason;
        }

        public @NotNull VisibleFailureReason getReason() {
            return this.reason;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final DownloadFailure that)) return false;
            return Objects.equals(this.reason, that.reason);
        }

        @Override
        public int hashCode() {
            return FailureReasonGetter.hashCode(this.reason);
        }

        @Override
        public @NotNull String toString() {
            return "DownloadFailure{" +
                    "reason=" + this.reason +
                    '}';
        }
    }

    @Override
    protected @NotNull DownloadFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new DownloadFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull DownloadFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.reason);
    }

    @Override
    public @NotNull String toString() {
        return "DownloadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
