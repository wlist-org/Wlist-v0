package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public class PageFile extends IFragment<PageFileBinding, PageFile> {
    protected final @NotNull PageFilePartUpload partUpload = new PageFilePartUpload(this);

    @Override
    protected @NotNull PageFileBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageFileBinding.inflate(inflater);
    }
    @Override
    protected void onBuild(final @NotNull PageFileBinding page) {
        this.partUpload.onBind(page);
    }

    @Override
    public void onActivityCreateHook(final @NotNull ActivityMain activity) {
        this.partUpload.onActivityCreateHook(activity);
    }

    @Override
    public @NotNull String toString() {
        return "PageFile{" +
                "super=" + super.toString() +
                '}';
    }
}
