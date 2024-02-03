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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class RenameTasksManager extends AbstractTasksManager<RenameTasksManager.RenameTask, RenameTasksManager.RenameWorking, RenameTasksManager.RenameSuccess, RenameTasksManager.RenameFailure> {
    protected RenameTasksManager() {
        super(PageTaskAdapter.Types.Rename);
    }

    protected static final @NotNull File RenameRecordsSaveDirectory = new File(AbstractTasksManager.BaseRecordsSaveDirectory, "Renames");

    @WorkerThread
    public static @NotNull RenameTasksManager getInstance() {
        return (RenameTasksManager) AbstractTasksManager.managers.getInstance(PageTaskAdapter.Types.Rename);
    }

    @WorkerThread
    public static void initializeIfNotSuccess(final @NotNull CActivity activity) {
        AbstractTasksManager.managers.reinitializeIfNotSuccess(PageTaskAdapter.Types.Rename, () -> {
            final RenameTasksManager manager = new RenameTasksManager();
            manager.initialize(activity, RenameTasksManager.RenameRecordsSaveDirectory);
            return manager;
        }, null);
    }
    
    @Override
    protected @NotNull RenameWorking prepareTask(final @NotNull Activity activity, final @NotNull RenameTask task) {
        return new RenameWorking();
    }

    @Override
    protected @NotNull UnionPair<RenameSuccess, RenameFailure> runTask(final @NotNull Activity activity, final @NotNull RenameTask task, final @NotNull RenameWorking progress) {
        try {
            progress.started = true;
            progress.updateCallbacks.callback(RunnableE::run);
            final UnionPair<VisibleFileInformation, VisibleFailureReason> reason = FilesAssistant.rename(task.address, task.username,
                    task.location, task.isDirectory, task.name, Main.ClientExecutors, p -> true);
            assert reason != null;
            return reason.isSuccess() ? UnionPair.ok(new RenameSuccess()) : UnionPair.fail(new RenameFailure(reason.getE()));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            final VisibleFailureReason reason = new VisibleFailureReason(FailureKind.Others, task.location, Objects.requireNonNullElse(throwable.getLocalizedMessage(), ""));
            return UnionPair.fail(new RenameFailure(reason));
        }
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull RenameTask task) {
        return new File(RenameTasksManager.RenameRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class RenameTask extends AbstractTask {
        protected final @NotNull FileLocation location;
        protected final boolean isDirectory;
        protected final @NotNull String name;

        @WorkerThread
        public RenameTask(final @NotNull AbstractTask task, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull String name) {
            super(task);
            this.location = location;
            this.isDirectory = isDirectory;
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof final RenameTask that)) return false;
            if (!super.equals(o)) return false;
            return this.isDirectory == that.isDirectory && Objects.equals(this.location, that.location) && Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.location, this.isDirectory, this.name);
        }

        @Override
        public @NotNull String toString() {
            return "RenameTask{" +
                    "location=" + this.location +
                    ", isDirectory=" + this.isDirectory +
                    ", name='" + this.name + '\'' +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    protected @NotNull RenameTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        final FileLocation location = AbstractTasksManager.parseLocation(inputStream);
        final boolean isDirectory = inputStream.readBoolean();
        final String name = inputStream.readUTF();
        return new RenameTask(abstractTask, location, isDirectory, name);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull RenameTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
        AbstractTasksManager.dumpLocation(outputStream, task.location);
        outputStream.writeBoolean(task.isDirectory);
        outputStream.writeUTF(task.name);
    }

    public static class RenameWorking extends AbstractSimpleExtraWorking {
    }

    @Override
    protected @NotNull RenameWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new RenameWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull RenameWorking extra) {
    }

    public static class RenameSuccess extends AbstractSimpleExtraSuccess {
    }

    @Override
    protected @NotNull RenameSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new RenameSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull RenameSuccess extra) {
    }

    public static class RenameFailure extends AbstractSimpleExtraFailure {
        protected RenameFailure(final @NotNull VisibleFailureReason reason) {
            super(reason);
        }
    }

    @Override
    protected @NotNull RenameFailure parseExtraFailure(final @NotNull DataInput inputStream) throws IOException {
        final VisibleFailureReason reason = AbstractTasksManager.parseReason(inputStream);
        return new RenameFailure(reason);
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull RenameFailure extra) throws IOException {
        AbstractTasksManager.dumpReason(outputStream, extra.reason);
    }
}
