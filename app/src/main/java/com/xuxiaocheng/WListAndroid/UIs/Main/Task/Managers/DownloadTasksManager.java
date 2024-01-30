package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
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
import java.io.FileNotFoundException;
import java.io.IOException;
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
        return new File(DownloadTasksManager.DownloadRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    @Override
    protected @NotNull DownloadTasksManager.DownloadWorking prepareTask(final @NotNull Activity activity, final @NotNull DownloadTask task) throws IOException, InterruptedException {
        final ContentResolver contentResolver = activity.getContentResolver();
        final Uri file = Uri.fromFile(DownloadTasksManager.getSaveFile(task));
        try {
            contentResolver.openFileDescriptor(file, "w").close();
        } catch (final FileNotFoundException ignore) {
            contentResolver.insert(file, null);
        }
        PermissionUtil.writePermission(activity);
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
        protected final @NotNull File savePath;

        @WorkerThread
        public DownloadTask(final @NotNull AbstractTask task, final @NotNull FileLocation location) {
            super(task);
            this.location = location;
            this.savePath = DownloadTasksManager.getSaveFile(this);
        }

        public @NotNull FileLocation getLocation() {
            return this.location;
        }

        public @NotNull File getSavePath() {
            return this.savePath;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final DownloadTask that)) return false;
            if (!super.equals(o)) return false;
            return FileLocationGetter.equals(this.location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location);
        }

        @Override
        public @NotNull String toString() {
            return "DownloadTask{" +
                    "location=" + this.location +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull DownloadTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        final FileLocation location = AbstractTasksManager.parseLocation(inputStream);
        return new DownloadTask(abstractTask, location);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull DownloadTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
        AbstractTasksManager.dumpLocation(outputStream, task.location);
    }

    public static class DownloadWorking extends AbstractSimpleExtraWorking {
        protected @Nullable List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress = null;

        public @Nullable List<Pair.ImmutablePair<AtomicLong, Long>> getProgress() {
            return this.progress;
        }

        @Override
        public @NotNull String toString() {
            return "DownloadWorking{" +
                    "progress=" + this.progress +
                    ", super=" + super.toString() +
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

    public static class DownloadSuccess extends AbstractSimpleExtraSuccess {
    } // TODO: is deleted?

    @Override
    protected @NotNull DownloadSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new DownloadSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull DownloadSuccess extra) {
    }

    public static class DownloadFailure extends AbstractSimpleExtraFailure {
        protected DownloadFailure(final @NotNull VisibleFailureReason reason) {
            super(reason);
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
