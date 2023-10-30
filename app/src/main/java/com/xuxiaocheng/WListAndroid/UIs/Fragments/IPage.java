package com.xuxiaocheng.WListAndroid.UIs.Fragments;

import android.view.LayoutInflater;
import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import org.jetbrains.annotations.NotNull;

public abstract class IPage<V extends ViewBinding, P extends ViewBinding, F extends IFragment<P, F>> extends IFragmentPart<P, F> {
    protected final @NotNull HInitializer<V> pageCache = new HInitializer<>("PageCache");
    public @NotNull V page() {
        return this.pageCache.getInstance();
    }

    protected IPage(final @NotNull F fragment) {
        super(fragment);
    }

    @UiThread
    protected abstract @NotNull V inflate(final @NotNull LayoutInflater inflater);

    @UiThread
    protected void onBuildPage(final @NotNull V page) {
    }

    @Override
    protected void onActivityCreateHook(final @NotNull ActivityMain activity) {
        super.onActivityCreateHook(activity);
        final V page = this.inflate(activity.getLayoutInflater());
        this.pageCache.reinitialize(page);
        this.onBuildPage(page);
    }

    @UiThread
    protected boolean onBackPressed(final @NotNull ActivityMain activity) {
        return false;
    }

    protected void start(final @NotNull ActivityMain activity) {
        activity.transferPage(this.page().getRoot(), unused -> this.onBackPressed(activity));
    }
}
