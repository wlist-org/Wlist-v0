package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.view.LayoutInflater;
import android.view.View;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.IPage;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskBinding;
import org.jetbrains.annotations.NotNull;

class PageTask extends IPage<PageTaskBinding, PageFileBinding, FragmentFile> {
    protected PageTask(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void onBuild(final @NotNull PageFileBinding fragment) {
        super.onBuild(fragment);
        this.activity().getContent().activityMainTasks.setOnClickListener(v -> this.start(this.activity()));
    }

    @Override
    protected @NotNull PageTaskBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageTaskBinding.inflate(inflater);
    }

    @Override
    protected void onBuildPage(final @NotNull PageTaskBinding page) {
        super.onBuildPage(page);

    }

    @Override
    protected void onConnected(final @NotNull ActivityMain activity) {
        super.onConnected(activity);
        Main.runOnUiThread(activity, () -> activity.getContent().activityMainTasks.setVisibility(View.VISIBLE));
    }

    @Override
    protected void onDisconnected(final @NotNull ActivityMain activity) {
        super.onDisconnected(activity);
        Main.runOnUiThread(activity, () -> activity.getContent().activityMainTasks.setVisibility(View.GONE));
    }
}
