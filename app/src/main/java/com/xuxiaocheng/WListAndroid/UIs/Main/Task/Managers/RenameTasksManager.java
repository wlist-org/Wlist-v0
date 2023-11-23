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

        return UnionPair.ok(new RenameSuccess());
    }

    @Override
    public @NotNull File getRecordingFile(final @NotNull RenameTask task) {
        return new File(RenameTasksManager.RenameRecordsSaveDirectory, task + ".bin.gz");
    }

    public static class RenameTask extends AbstractTask {
        protected RenameTask(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull ZonedDateTime time) {
            super(address, username, time);
        }
    }

    @Override
    protected @NotNull RenameTask parseTask(final @NotNull DataInput inputStream) throws IOException {
        final InetSocketAddress address = AbstractTasksManager.parseAddress(inputStream);
        final String username = inputStream.readUTF();
        final ZonedDateTime time = AbstractTasksManager.parseTime(inputStream);
        return new RenameTask(address, username, time);
    }

    @Override
    protected void dumpTask(final @NotNull DataOutput outputStream, final @NotNull RenameTask task) throws IOException {
        AbstractTasksManager.dumpAddress(outputStream, task.address);
        outputStream.writeUTF(task.username);
        AbstractTasksManager.dumpTime(outputStream, task.time);
    }

    public static class RenameWorking {
    }

    @Override
    protected @NotNull RenameWorking parseExtraWorking(final @NotNull DataInput inputStream) {
        return new RenameWorking();
    }

    @Override
    protected void dumpExtraWorking(final @NotNull DataOutput outputStream, final @NotNull RenameWorking extra) {
    }

    public static class RenameSuccess {
    }

    @Override
    protected @NotNull RenameSuccess parseExtraSuccess(final @NotNull DataInput inputStream) {
        return new RenameSuccess();
    }

    @Override
    protected void dumpExtraSuccess(final @NotNull DataOutput outputStream, final @NotNull RenameSuccess extra) {
    }

    public static class RenameFailure {
    }

    @Override
    protected @NotNull RenameFailure parseExtraFailure(final @NotNull DataInput inputStream) {
        return new RenameFailure();
    }

    @Override
    protected void dumpExtraFailure(final @NotNull DataOutput outputStream, final @NotNull RenameFailure extra) {
    }

}
