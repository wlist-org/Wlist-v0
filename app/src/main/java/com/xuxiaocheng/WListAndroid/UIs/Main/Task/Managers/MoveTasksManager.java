package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MoveTasksManager extends AbstractTasksManager<MoveTasksManager.MoveTask, MoveTasksManager.MoveWorking, MoveTasksManager.MoveSuccess, MoveTasksManager.MoveFailure> {
    protected MoveTasksManager() {
        super(PageTaskAdapter.Types.Move);
    }

    protected static final @NotNull File MoveRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Moves");

    @WorkerThread
    public static @NotNull MoveTasksManager getInstance() {
        return (MoveTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Move);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Move, () -> {
            final MoveTasksManager manager = new MoveTasksManager();
            manager.initialize(activity, MoveTasksManager.MoveRecordsSaveDirectory);
            return manager;
        }, null);
    }
    
    @Override
    protected @NotNull MoveWorking prepareTask(final @NotNull Activity activity, final @NotNull MoveTask task) {
        return new MoveWorking();
    }

    @Override
    protected @NotNull UnionPair<MoveSuccess, MoveFailure> runTask(final @NotNull Activity activity, final @NotNull MoveTask task, final @NotNull MoveWorking progress) {
        // TODO
        return UnionPair.ok(new MoveSuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull MoveTask task) {
        return new File(MoveTasksManager.MoveRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class MoveTask extends AbstractTask {
        protected final @NotNull FileLocation location;
        protected final boolean isDirectory;
        protected final @NotNull FileLocation target;

        public MoveTask(final @NotNull AbstractTask task, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation target) {
            super(task);
            this.location = location;
            this.isDirectory = isDirectory;
            this.target = target;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final MoveTask moveTask)) return false;
            if (!super.equals(o)) return false;
            return this.isDirectory == moveTask.isDirectory && Objects.equals(this.location, moveTask.location) && Objects.equals(this.target, moveTask.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.isDirectory, this.target);
        }

        @Override
        public @NotNull String toString() {
            return "MoveTask{" +
                    "location=" + this.location +
                    ", isDirectory=" + this.isDirectory +
                    ", target=" + this.target +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull MoveTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        final FileLocation location = AbstractTasksManager.parseLocation(inputStream);
        final boolean isDirectory = inputStream.readBoolean();
        final FileLocation target = AbstractTasksManager.parseLocation(inputStream);
        return new MoveTask(abstractTask, location, isDirectory, target);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull MoveTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
        AbstractTasksManager.dumpLocation(outputStream, task.location);
        outputStream.writeBoolean(task.isDirectory);
        AbstractTasksManager.dumpLocation(outputStream, task.target);
    }

    public static class MoveWorking extends AbstractSimpleExtraWorking {
    }

    @Override
    protected @NotNull MoveWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new MoveWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull MoveWorking extra) {
    }

    public static class MoveSuccess extends AbstractSimpleExtraSuccess {
    }

    @Override
    protected @NotNull MoveSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new MoveSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull MoveSuccess extra) {
    }

    public static class MoveFailure extends AbstractSimpleExtraFailure {
        protected MoveFailure(final @NotNull VisibleFailureReason reason) {
            super(reason);
        }
    }

    @Override
    protected @NotNull MoveFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new MoveFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull MoveFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.getReason());
    }
}
