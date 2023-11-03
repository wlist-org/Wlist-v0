package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.view.View;
import androidx.annotation.AnyThread;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task.PageTask;
import org.jetbrains.annotations.NotNull;

class PartTask extends SFragmentFilePart{
    protected PartTask(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        this.pageContent().activityMainTasks.setOnClickListener(v -> this.start());
    }

    @Override
    public void cOnConnect() {
        super.cOnConnect();
        Main.runOnUiThread(this.activity(), () -> this.pageContent().activityMainTasks.setVisibility(View.VISIBLE));
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
