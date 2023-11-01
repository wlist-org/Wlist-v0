package com.xuxiaocheng.WListAndroid.UIs.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

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
    protected void onAttach() {
        super.onAttach();
        final V page = this.inflate(this.activity().getLayoutInflater());
        this.pageCache.reinitialize(page);
        this.onBuildPage(page);
    }

    @Override
    protected void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("wlist:i_page:showed", this.showed.get());
    }

    @Override
    protected void onRestoreInstanceState(final @Nullable Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null)
            this.showed.set(savedInstanceState.getBoolean("wlist:i_page:showed", false));
    }

    @Override
    protected void onConnected(final @NotNull ActivityMain activity) {
        super.onConnected(activity);
        if (this.showed.get() != this.realShowed.get() && this.showed.get())
            Main.runOnUiThread(this.activity(), () -> this.start(this.activity()));
    }

    @Override
    protected void onDisconnected(final @NotNull ActivityMain activity) {
        super.onDisconnected(activity);
        if (this.realShowed.get())
            Main.runOnUiThread(this.activity(), () -> this.activity().resetPage());
        else if (this.showed.get())
            Main.runOnUiThread(this.activity(), () -> this.start(this.activity()));
    }

    protected final @NotNull AtomicBoolean showed = new AtomicBoolean(false);
    protected final @NotNull AtomicBoolean realShowed = new AtomicBoolean(false);
    public boolean isShowed() {
        return this.showed.get();
    }

    protected void onShow() {
        this.showed.set(true);
        this.realShowed.set(true);
    }

    protected void onHide() {
        this.showed.set(false);
        this.realShowed.set(false);
    }

    @UiThread
    protected boolean onBackPressed() {
        return false;
    }

    @UiThread
    protected void start(final @NotNull ActivityMain activity) {
        activity.transferPage(this.page().getRoot(), unused -> this.onBackPressed(), this::onHide);
        this.onShow();
    }

    @Override
    public @NotNull String toString() {
        return "IPage{" +
                "pageCache=" + this.pageCache.isInitialized() +
                ", showed=" + this.showed +
                ", super=" + super.toString() +
                '}';
    }
}
