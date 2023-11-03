package com.xuxiaocheng.WListAndroid.UIs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FragmentBase<V extends ViewBinding> extends Fragment {
    @SuppressWarnings("unchecked")
    protected <A extends IActivity<?>> @NotNull A activity() {
        return (A) this.requireActivity();
    }


    protected final @NotNull HInitializer<V> contentCache = new HInitializer<>("FragmentBaseContentCache");
    public @NotNull V content() {
        return this.contentCache.getInstance();
    }
    @UiThread
    protected abstract @NotNull V iOnInflater();
    @UiThread
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
    }
    @UiThread
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
    }
    @UiThread
    protected void iOnBuildPage(final @NotNull V page, final boolean isFirstTime) {
    }
    @UiThread
    protected boolean iOnBackPressed() {
        return false;
    }


    @UiThread
    @Override
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        final V fragment = this.iOnInflater();
        this.contentCache.reinitialize(fragment);
        this.iOnRestoreInstanceState(this.getArguments(), savedInstanceState == null ? null : savedInstanceState.getBundle("i:fragment"));
        this.iOnBuildPage(fragment, savedInstanceState == null);
        return fragment.getRoot();
    }

    @UiThread
    @Override
    public void onAttach(final @NotNull Context context) {
        super.onAttach(context);
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
        outState.putBundle("i:fragment", bundle);
    }

    @UiThread
    @Override
    public void onDestroy() {
        super.onDestroy();
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
        return "FragmentBase{" +
                "contentCached=" + this.contentCache.isInitialized() +
                '}';
    }
}
