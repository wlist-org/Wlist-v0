package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class CopyTasksManager extends AbstractTasksManager<CopyTasksManager.CopyTask, CopyTasksManager.CopyWorking, CopyTasksManager.CopySuccess, CopyTasksManager.CopyFailure> {
    protected CopyTasksManager() {
        super(PageTaskAdapter.Types.Copy);
    }

    protected static final @NotNull File CopyRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Copies");

    @WorkerThread
    public static @NotNull CopyTasksManager getInstance() {
        return (CopyTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Copy);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Copy, () -> {
            final CopyTasksManager manager = new CopyTasksManager();
            manager.initialize(activity, CopyTasksManager.CopyRecordsSaveDirectory);
            return manager;
        }, null);
    }
    
    @Override
    protected @NotNull CopyWorking prepareTask(final @NotNull Activity activity, final @NotNull CopyTask task) {
        return new CopyWorking();
    }

    @Override
    protected @NotNull UnionPair<CopySuccess, CopyFailure> runTask(final @NotNull Activity activity, final @NotNull CopyTask task, final @NotNull CopyWorking progress) {
        try {
            progress.started = true;
            progress.updateCallbacks.callback(RunnableE::run);
            final UnionPair<VisibleFileInformation, VisibleFailureReason> reason = FilesAssistant.copy(task.address, task.username,
                    task.location, task.isDirectory, task.target, task.targetName, Main.ClientExecutors, p -> true);
            assert reason != null;
            return reason.isSuccess() ? UnionPair.ok(new CopySuccess()) : UnionPair.fail(new CopyFailure(reason.getE()));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            final VisibleFailureReason reason = new VisibleFailureReason(FailureKind.Others, task.location, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
            return UnionPair.fail(new CopyFailure(reason));
        }
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull CopyTask task) {
        return new File(CopyTasksManager.CopyRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class CopyTask extends AbstractTasksManager.AbstractTask {
        protected final @NotNull FileLocation location;
        protected final boolean isDirectory;
        protected final @NotNull FileLocation target;
        protected final @NotNull String targetName;

        public CopyTask(final @NotNull AbstractTask task, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation target, final @NotNull String targetName) {
            super(task);
            this.location = location;
            this.isDirectory = isDirectory;
            this.target = target;
            this.targetName = targetName;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof final CopyTask copyTask)) return false;
            if (!super.equals(o)) return false;
            return this.isDirectory == copyTask.isDirectory && Objects.equals(this.location, copyTask.location) && Objects.equals(this.target, copyTask.target) && Objects.equals(this.targetName, copyTask.targetName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.isDirectory, this.target, this.targetName);
        }

        @Override
        public @NotNull String toString() {
            return "CopyTask{" +
                    "location=" + this.location +
                    ", isDirectory=" + this.isDirectory +
                    ", target=" + this.target +
                    ", targetName='" + this.targetName + '\'' +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull CopyTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        final FileLocation location = AbstractTasksManager.parseLocation(inputStream);
        final boolean isDirectory = inputStream.readBoolean();
        final FileLocation target = AbstractTasksManager.parseLocation(inputStream);
        final String targetName = inputStream.readUTF();
        return new CopyTask(abstractTask, location, isDirectory, target, targetName);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull CopyTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
        AbstractTasksManager.dumpLocation(outputStream, task.location);
        outputStream.writeBoolean(task.isDirectory);
        AbstractTasksManager.dumpLocation(outputStream, task.target);
        outputStream.writeUTF(task.targetName);
    }

    public static class CopyWorking extends AbstractSimpleExtraWorking {
    }

    @Override
    protected @NotNull CopyWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new CopyWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull CopyWorking extra) {
    }

    public static class CopySuccess extends AbstractSimpleExtraSuccess {
    }

    @Override
    protected @NotNull CopySuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new CopySuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull CopySuccess extra) {
    }

    public static class CopyFailure extends AbstractSimpleExtraFailure {
        protected CopyFailure(final @NotNull VisibleFailureReason reason) {
            super(reason);
        }
    }

    @Override
    protected @NotNull CopyFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new CopyFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull CopyFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.getReason());
    }
}
