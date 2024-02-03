package com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers;

import android.app.Activity;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

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
        // TODO
        return UnionPair.ok(new CopySuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull CopyTask task) {
        return new File(CopyTasksManager.CopyRecordsSaveDirectory, this.getRecordingFileIdentifier(task));
    }

    public static class CopyTask extends AbstractTasksManager.AbstractTask {
        // TODO

        protected CopyTask(final @NotNull AbstractTask task) {
            super(task);
        }
    }

    @Override
    protected @NotNull CopyTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final AbstractTask abstractTask = AbstractTasksManager.parseAbstractTask(inputStream);
        return new CopyTask(abstractTask);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull CopyTask task) throws IOException {
        AbstractTasksManager.dumpAbstractTask(outputStream, task);
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
