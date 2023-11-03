package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class IFragmentPart<F extends IFragment<?>> {
    protected final @NotNull F fragment;

    protected IFragmentPart(final @NotNull F fragment) {
        super();
        this.fragment = fragment;
    }

    protected @NotNull F fragment() {
        return this.fragment;
    }
    protected @NotNull ViewBinding fragmentContent() {
        return this.fragment.content();
    }
    protected @NotNull IPage<?> page() {
        return this.fragment.page();
    }
    protected @NotNull ViewBinding pageContent() {
        return this.fragment.page().content();
    }
    protected @NotNull IActivity<?> activity() {
        return this.fragment.activity();
    }
    protected @NotNull ViewBinding activityContent() {
        return this.fragment.activity().content();
    }

    @UiThread
    public void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
    }

    @UiThread
    public void iOnSaveInstanceState(final @NotNull Bundle outState) {
    }

    @UiThread
    protected void iOnBuildPage() {
    }

    @UiThread
    public void onAttach() {
    }

    @UiThread
    public void onCreate() {
    }

    @UiThread
    public void onStart() {
    }

    @UiThread
    public void onResume() {
    }

    @UiThread
    public void onPause() {
    }

    @UiThread
    public void onStop() {
    }

    @UiThread
    public void onDestroy() {
    }

    @UiThread
    public void onDetach() {
    }

    @Override
    public @NotNull String toString() {
        return "IFragmentPart{" +
                '}';
    }
}
