package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.Callbacks.HCallbacks;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
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

public class UploadTasksManager extends AbstractTasksManager<UploadTasksManager.UploadTask, UploadTasksManager.UploadWorking, UploadTasksManager.UploadSuccess, UploadTasksManager.UploadFailure> {
    protected UploadTasksManager() {
        super(PageTaskAdapter.Types.Upload);
    }

    protected static final @NotNull File UploadRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Uploads");

    @WorkerThread
    public static @NotNull UploadTasksManager getInstance() {
        return (UploadTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Upload);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Upload, () -> {
            final UploadTasksManager manager = new UploadTasksManager();
            manager.initialize(activity, UploadTasksManager.UploadRecordsSaveDirectory);
            return manager;
        }, null);
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull UploadTask task) {
        return new File(UploadTasksManager.UploadRecordsSaveDirectory, FileLocationGetter.storage(task.location) + " - " + FileLocationGetter.id(task.location) + ".bin.gz");
    }

    @Override
    protected @NotNull UploadWorking prepareTask(final @NotNull Activity activity, final @NotNull UploadTask task) throws IOException, InterruptedException {
        PermissionUtil.readPermission(activity);
        return new UploadWorking();
    }

    @Override
    protected @NotNull UnionPair<UploadSuccess, UploadFailure> runTask(final @NotNull Activity activity, final @NotNull UploadTask task, final @NotNull UploadTasksManager.UploadWorking progress) {
        return UnionPair.ok(new UploadSuccess());
    }


    public static class UploadTask extends AbstractTask {
        protected final @NotNull FileLocation location;
        protected final @NotNull String filename;
        protected final PageTaskAdapter.@NotNull Types source;
//        protected final @NotNull File sourcePath;

        @WorkerThread
        public UploadTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time, final @NotNull FileLocation location, final @NotNull String filename, final PageTaskAdapter.@NotNull Types source) {
            super(address, username, time);
            this.location = location;
            this.filename = filename;
            this.source = source;
//            this.sourcePath = UploadTasksManager.getSaveFile(this);
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

//        public @NotNull File getSourcePath() {
//            return this.sourcePath;
//        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final UploadTask that)) return false;
            if (!super.equals(o)) return false;
            return FileLocationGetter.equals(this.location, that.location) && this.filename.equals(that.filename) && this.source == that.source;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.filename, this.source);
        }

        @Override
        public @NotNull String toString() {
            return "UploadTask{" +
                    "location=" + this.location +
                    ", filename=" + this.filename +
                    ", source=" + this.source +
                    '}';
        }
    }

    @Override
    protected @NotNull UploadTasksManager.UploadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final FileLocation location = new FileLocation(storage, id);
        final String filename = inputStream.readUTF();
        final PageTaskAdapter.Types source = PageTaskAdapter.Types.fromPosition(inputStream.readInt());
        return new UploadTask(address, username, time, location, filename, source);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull UploadTasksManager.UploadTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
        outputStream.writeUTF(FileLocationGetter.storage(task.location));
        outputStream.writeLong(FileLocationGetter.id(task.location));
        outputStream.writeUTF(task.filename);
        outputStream.writeInt(PageTaskAdapter.Types.toPosition(task.source));
    }

    public static class UploadWorking {
        protected InstantaneousProgressState state = null;
        protected List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress = null;
        protected final @NotNull HCallbacks<RunnableE> updateCallbacks = new HCallbacks<>();

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
            return "UploadWorking{" +
                    "state=" + this.state +
                    ", progress=" + this.progress +
                    '}';
        }
    }

    @Override
    protected @NotNull UploadTasksManager.UploadWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new UploadWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull UploadTasksManager.UploadWorking extra) {
    }

    public static class UploadSuccess {
    }

    @Override
    protected @NotNull UploadTasksManager.UploadSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new UploadSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull UploadTasksManager.UploadSuccess extra) {
    }

    public static class UploadFailure {
        protected final @NotNull VisibleFailureReason reason;

        protected UploadFailure(final @NotNull VisibleFailureReason reason) {
            super();
            this.reason = reason;
        }

        public @NotNull VisibleFailureReason getReason() {
            return this.reason;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final UploadFailure that)) return false;
            return Objects.equals(this.reason, that.reason);
        }

        @Override
        public int hashCode() {
            return FailureReasonGetter.hashCode(this.reason);
        }

        @Override
        public @NotNull String toString() {
            return "UploadFailure{" +
                    "reason=" + this.reason +
                    '}';
        }
    }

    @Override
    protected @NotNull UploadTasksManager.UploadFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new UploadFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull UploadTasksManager.UploadFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.reason);
    }

    @Override
    public @NotNull String toString() {
        return "UploadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
