package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.Callbacks.HCallbacks;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.time.ZonedDateTime;
import java.util.Objects;

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
        return new File(UploadTasksManager.UploadRecordsSaveDirectory, FileLocationGetter.storage(task.parent) + " - " + FileLocationGetter.id(task.parent) + ".bin.gz");
    }

    @Override
    protected @NotNull UploadWorking prepareTask(final @NotNull Activity activity, final @NotNull UploadTask task) throws IOException, InterruptedException {
        PermissionUtil.readPermission(activity);
        return new UploadWorking();
    }

    @Override
    protected @NotNull UnionPair<UploadSuccess, UploadFailure> runTask(final @NotNull Activity activity, final @NotNull UploadTask task, final @NotNull UploadWorking progress) {
        try {
            final UnionPair<VisibleFileInformation, VisibleFailureReason> reason = FilesAssistant.upload(task.address, task.username, HExceptionWrapper.wrapConsumer(access -> {
                try (final ParcelFileDescriptor parcelFileDescriptor = activity.getContentResolver().openFileDescriptor(task.uri, "r");
                     final FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                     final FileChannel channel = fileInputStream.getChannel()) {
                    access.accept(channel);
                }
            }), task.parent, task.filename, task.filesize, PredicateE.truePredicate(), s -> {
                progress.state = s;
                progress.updateCallbacks.callback(RunnableE::run);
            });
            assert reason != null;
            return reason.isSuccess() ? UnionPair.ok(new UploadSuccess()) : UnionPair.fail(new UploadFailure(reason.getE()));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            final VisibleFailureReason reason = new VisibleFailureReason(FailureKind.Others, task.parent, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
            return UnionPair.fail(new UploadFailure(reason));
        }
    }


    public static class UploadTask extends AbstractTask {
        protected final @NotNull FileLocation parent;
        protected final @NotNull String filename;
        protected final long filesize;
        protected final PageTaskAdapter.@NotNull Types source;
        protected final @NotNull Uri uri;

        @WorkerThread
        public UploadTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time, final @NotNull FileLocation parent, final @NotNull String filename, final long filesize, final PageTaskAdapter.@NotNull Types source, final @NotNull Uri uri) {
            super(address, username, time);
            this.parent = parent;
            this.filename = filename;
            this.filesize = filesize;
            this.source = source;
            this.uri = uri;
        }

        public @NotNull FileLocation getParent() {
            return this.parent;
        }

        public @NotNull String getFilename() {
            return this.filename;
        }

        public long getFilesize() {
            return this.filesize;
        }

        public PageTaskAdapter.@NotNull Types getSource() {
            return this.source;
        }

        public @NotNull Uri getUri() {
            return this.uri;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final UploadTask that)) return false;
            if (!super.equals(o)) return false;
            return this.filesize == that.filesize && FileLocationGetter.equals(this.parent, that.parent) && Objects.equals(this.filename, that.filename) && this.source == that.source && Objects.equals(this.uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.parent, this.filename, this.filesize, this.source, this.uri);
        }

        @Override
        public @NotNull String toString() {
            return "UploadTask{" +
                    "location=" + this.parent +
                    ", filename='" + this.filename + '\'' +
                    ", filesize=" + this.filesize +
                    ", source=" + this.source +
                    ", uri=" + this.uri +
                    '}';
        }
    }

    @Override
    protected @NotNull UploadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        final String storage = inputStream.readUTF();
        final long id = inputStream.readLong();
        final FileLocation parent = new FileLocation(storage, id);
        final String filename = inputStream.readUTF();
        final long filesize = inputStream.readLong();
        final PageTaskAdapter.Types source = PageTaskAdapter.Types.fromPosition(inputStream.readInt());
        final Uri uri = Uri.parse(inputStream.readUTF());
        return new UploadTask(address, username, time, parent, filename, filesize, source, uri);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull UploadTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
        outputStream.writeUTF(FileLocationGetter.storage(task.parent));
        outputStream.writeLong(FileLocationGetter.id(task.parent));
        outputStream.writeUTF(task.filename);
        outputStream.writeLong(task.filesize);
        outputStream.writeInt(PageTaskAdapter.Types.toPosition(task.source));
        outputStream.writeUTF(task.uri.toString());
    }

    public static class UploadWorking {
        protected InstantaneousProgressState state = null;
        protected final @NotNull HCallbacks<RunnableE> updateCallbacks = new HCallbacks<>();

        public @Nullable InstantaneousProgressState getState() {
            return this.state;
        }

        public @NotNull HCallbacks<RunnableE> getUpdateCallbacks() {
            return this.updateCallbacks;
        }

        @Override
        public @NotNull String toString() {
            return "UploadWorking{" +
                    "state=" + this.state +
                    '}';
        }
    }

    @Override
    protected @NotNull UploadWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new UploadWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull UploadWorking extra) {
    }

    public static class UploadSuccess {
    }

    @Override
    protected @NotNull UploadSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new UploadSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull UploadSuccess extra) {
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
    protected @NotNull UploadFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new UploadFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull UploadFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.reason);
    }

    @Override
    public @NotNull String toString() {
        return "UploadTasksManager{" +
                "super=" + super.toString() +
                '}';
    }
}
