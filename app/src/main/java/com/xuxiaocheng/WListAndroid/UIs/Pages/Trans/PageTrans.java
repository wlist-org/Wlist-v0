package com.xuxiaocheng.WListAndroid.UIs.Pages.Trans;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageTransBinding;
import org.jetbrains.annotations.NotNull;

public class PageTrans extends IFragment<PageTransBinding> {
    @Override
    protected @NotNull PageTransBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageTransBinding.inflate(inflater);
    }

    @Override
    protected void onBuild(final @NotNull ActivityMain activity, final @NotNull PageTransBinding page) {

    }
}
