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

        return UnionPair.ok(new CopySuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull CopyTask task) {
        return new File(CopyTasksManager.CopyRecordsSaveDirectory, task + ".bin.gz");
    }

    public static class CopyTask extends AbstractTasksManager.AbstractTask {
        protected CopyTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time) {
            super(address, username, time);
        }
    }

    @Override
    protected @NotNull CopyTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        return new CopyTask(address, username, time);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull CopyTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
    }

    public static class CopyWorking {
    }

    @Override
    protected @NotNull CopyWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new CopyWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull CopyWorking extra) {
    }

    public static class CopySuccess {
    }

    @Override
    protected @NotNull CopySuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new CopySuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull CopySuccess extra) {
    }

    public static class CopyFailure {
    }

    @Override
    protected @NotNull CopyFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new CopyFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull CopyFailure extra) {
    }

}
