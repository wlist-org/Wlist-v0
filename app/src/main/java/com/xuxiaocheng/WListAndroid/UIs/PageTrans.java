package com.xuxiaocheng.WListAndroid.UIs;

import android.view.View;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListAndroid.databinding.PageTransBinding;
import org.jetbrains.annotations.NotNull;

public class PageTrans implements ActivityMainChooser.MainPage {
    protected final @NotNull ActivityMain activity;

    public PageTrans(final @NotNull ActivityMain activity) {
        super();
        this.activity = activity;
    }

    protected final @NotNull HInitializer<PageTransBinding> pageCache = new HInitializer<>("PageTrans");
    @Override
    public @NotNull View onShow() {
        final PageTransBinding cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache.getRoot();
        final PageTransBinding page = PageTransBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.initialize(page);

        return page.getRoot();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
