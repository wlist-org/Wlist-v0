package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class TrashTasksManager extends AbstractTasksManager<TrashTasksManager.TrashTask, TrashTasksManager.TrashWorking, TrashTasksManager.TrashSuccess, TrashTasksManager.TrashFailure> {
    protected TrashTasksManager() {
        super(PageTaskAdapter.Types.Trash);
    }

    protected static final @NotNull File TrashRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Trashes");

    @WorkerThread
    public static @NotNull TrashTasksManager getInstance() {
        return (TrashTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Trash);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Trash, () -> {
            final TrashTasksManager manager = new TrashTasksManager();
            manager.initialize(activity, TrashTasksManager.TrashRecordsSaveDirectory);
            return manager;
        }, null);
    }
    
    @Override
    protected @NotNull TrashWorking prepareTask(final @NotNull Activity activity, final @NotNull TrashTask task) {
        return new TrashWorking();
    }

    @Override
    protected @NotNull UnionPair<TrashSuccess, TrashFailure> runTask(final @NotNull Activity activity, final @NotNull TrashTask task, final @NotNull TrashWorking progress) throws WrongStateException, IOException, InterruptedException {
        final boolean success = FilesAssistant.trash(task.address, task.username, task.directory, true, PredicateE.truePredicate(), (done, total) -> {
            progress.done = done;
            progress.total = total;
            progress.updateCallbacks.callback(RunnableE::run);
        });
        return success ? UnionPair.ok(new TrashSuccess()) : UnionPair.fail(new TrashFailure());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull TrashTask task) {
        return new File(TrashTasksManager.TrashRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class TrashTask extends AbstractTask {
        protected final @NotNull FileLocation directory;

        @WorkerThread
        public TrashTask(final @NotNull AbstractTask task, final @NotNull FileLocation directory) {
            super(task);
            this.directory = directory;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final TrashTask trashTask)) return false;
            if (!super.equals(o)) return false;
            return Objects.equals(this.directory, trashTask.directory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.directory);
        }

        @Override
        public @NotNull String toString() {
            return "TrashTask{" +
                    "directory=" + this.directory +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull TrashTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        final FileLocation directory = AbstractTasksManager.parseLocation(inputStream);
        return new TrashTask(abstractTask, directory);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull TrashTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
        AbstractTasksManager.dumpLocation(outputStream, task.directory);
    }

    public static class TrashWorking extends AbstractExtraWorking {
        protected @Nullable AtomicLong done = null;
        protected @Nullable AtomicLong total = null;

        public @Nullable AtomicLong getDone() {
            return this.done;
        }

        public @Nullable AtomicLong getTotal() {
            return this.total;
        }

        @Override
        public @NotNull String toString() {
            return "TrashWorking{" +
                    "done=" + this.done +
                    ", total=" + this.total +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull TrashWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new TrashWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull TrashWorking extra) {
    }

    public static class TrashSuccess extends AbstractExtraSuccess {
    }

    @Override
    protected @NotNull TrashSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new TrashSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull TrashSuccess extra) {
    }

    public static class TrashFailure extends AbstractExtraFailure {
    }

    @Override
    protected @NotNull TrashFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new TrashFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull TrashFailure extra) {
    }

}
