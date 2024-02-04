package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.view.View;
import androidx.annotation.AnyThread;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.CActivity;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.CopyTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.MoveTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.RenameTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.TrashTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.UploadTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTask;
import org.jetbrains.annotations.NotNull;

class PartTask extends SFragmentFilePart {
    protected PartTask(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @AnyThread
    public void initializeManagers() {
        final CActivity activity = this.activity();
        Main.runOnBackgroundThread(activity, () -> DownloadTasksManager.initializeIfNotSuccess(activity));
        Main.runOnBackgroundThread(activity, () -> UploadTasksManager.initializeIfNotSuccess(activity));
        Main.runOnBackgroundThread(activity, () -> TrashTasksManager.initializeIfNotSuccess(activity));
        Main.runOnBackgroundThread(activity, () -> CopyTasksManager.initializeIfNotSuccess(activity));
        Main.runOnBackgroundThread(activity, () -> MoveTasksManager.initializeIfNotSuccess(activity));
        Main.runOnBackgroundThread(activity, () -> RenameTasksManager.initializeIfNotSuccess(activity));
    }

    @Override
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        if (this.fragment.isSelectingMode())
            return;
        this.pageContent().activityMainTasks.setOnClickListener(v -> this.start());
    }

    @Override
    public void cOnConnect() {
        super.cOnConnect();
        if (this.fragment.isSelectingMode())
            return;
        Main.runOnUiThread(this.activity(), () -> this.pageContent().activityMainTasks.setVisibility(View.VISIBLE));
        this.initializeManagers();
    }

    @Override
    public void cOnDisconnect() {
        super.cOnDisconnect();
        Main.runOnUiThread(this.activity(), () -> this.pageContent().activityMainTasks.setVisibility(View.GONE));
    }

    @AnyThread
    private void start() {
        this.activity().push(new PageTask(), "PageTasks");
    }
}
