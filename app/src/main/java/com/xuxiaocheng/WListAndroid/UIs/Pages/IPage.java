package com.xuxiaocheng.WListAndroid.UIs.Pages;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListAndroid.UIs.IActivity;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class IPage<P extends ViewBinding> extends Fragment {
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentCache");
    public @NotNull P page() {
        return this.pageCache.getInstance();
    }
    @SuppressWarnings("unchecked")
    protected <A extends IActivity<?>> @NotNull A activity() {
        return (A) this.requireActivity();
    }

    @UiThread
    protected abstract @NotNull P iOnInflater();
    @UiThread
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
    }
    @UiThread
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
    }
    @UiThread
    protected void iOnBuildPage(final @NotNull P page, final boolean isFirstTime) {
    }
    @UiThread
    protected boolean iOnBackPressed() {
        return false;
    }


    @UiThread
    @Override
    public void onAttach(final @NotNull Context context) {
        super.onAttach(context);
    }

    @UiThread
    @Override
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Creating page.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        final P page = this.iOnInflater();
        this.pageCache.reinitialize(page);
        this.iOnRestoreInstanceState(this.getArguments(), savedInstanceState == null ? null : savedInstanceState.getBundle("i:page"));
        this.iOnBuildPage(page, savedInstanceState == null);
        return page.getRoot();
    }

    @UiThread
    @Override
    public void onStart() {
        super.onStart();
    }

    @UiThread
    @Override
    public void onResume() {
        super.onResume();
    }

    @UiThread
    @Override
    public void onPause() {
        super.onPause();
    }

    @UiThread
    @Override
    public void onStop() {
        super.onStop();
    }

    @UiThread
    @Override
    public void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final Bundle bundle = new Bundle();
        this.iOnSaveInstanceState(bundle);
        outState.putBundle("i:page", bundle);
    }

    @UiThread
    @Override
    public void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Destroyed page.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
    }

    @UiThread
    @Override
    public void onDetach() {
        super.onDetach();
    }

    @UiThread
    public boolean onBackPressed() {
        return this.iOnBackPressed();
    }

    @Override
    public @NotNull String toString() {
        return "IPage{" +
                "pageCache=" + this.pageCache.isInitialized() +
                '}';
    }
}
