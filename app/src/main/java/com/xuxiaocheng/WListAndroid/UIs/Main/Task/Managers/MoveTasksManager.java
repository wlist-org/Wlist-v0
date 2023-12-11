package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;

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

        return UnionPair.ok(new MoveSuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull MoveTask task) {
        return new File(MoveTasksManager.MoveRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class MoveTask extends AbstractTasksManager.AbstractTask {
        protected MoveTask(final @NotNull AbstractTask task) {
            super(task);
        }
    }

    @Override
    protected @NotNull MoveTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        return new MoveTask(abstractTask);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull MoveTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
    }

    public static class MoveWorking extends AbstractExtraWorking {
    }

    @Override
    protected @NotNull MoveWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new MoveWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull MoveWorking extra) {
    }

    public static class MoveSuccess extends AbstractExtraSuccess {
    }

    @Override
    protected @NotNull MoveSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new MoveSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull MoveSuccess extra) {
    }

    public static class MoveFailure extends AbstractExtraFailure {
    }

    @Override
    protected @NotNull MoveFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new MoveFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull MoveFailure extra) {
    }

}
