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
        return new File(MoveTasksManager.MoveRecordsSaveDirectory, task + ".bin.gz");
    }

    public static class MoveTask extends AbstractTasksManager.AbstractTask {
        protected MoveTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time) {
            super(address, username, time);
        }
    }

    @Override
    protected @NotNull MoveTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        return new MoveTask(address, username, time);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull MoveTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
    }

    public static class MoveWorking {
    }

    @Override
    protected @NotNull MoveWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new MoveWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull MoveWorking extra) {
    }

    public static class MoveSuccess {
    }

    @Override
    protected @NotNull MoveSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new MoveSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull MoveSuccess extra) {
    }

    public static class MoveFailure {
    }

    @Override
    protected @NotNull MoveFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new MoveFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull MoveFailure extra) {
    }

}
