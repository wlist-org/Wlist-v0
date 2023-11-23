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
    protected @NotNull UnionPair<TrashSuccess, TrashFailure> runTask(final @NotNull Activity activity, final @NotNull TrashTask task, final @NotNull TrashWorking progress) {

        return UnionPair.ok(new TrashSuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull TrashTask task) {
        return new File(TrashTasksManager.TrashRecordsSaveDirectory, task + ".bin.gz");
    }

    public static class TrashTask extends AbstractTasksManager.AbstractTask {
        protected TrashTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time) {
            super(address, username, time);
        }
    }

    @Override
    protected @NotNull TrashTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        return new TrashTask(address, username, time);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull TrashTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
    }

    public static class TrashWorking {
    }

    @Override
    protected @NotNull TrashWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new TrashWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull TrashWorking extra) {
    }

    public static class TrashSuccess {
    }

    @Override
    protected @NotNull TrashSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new TrashSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull TrashSuccess extra) {
    }

    public static class TrashFailure {
    }

    @Override
    protected @NotNull TrashFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new TrashFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull TrashFailure extra) {
    }

}
