package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class IFragment<F extends ViewBinding> extends IFragmentBase<F> {
    public @NotNull IPage<?> page() {
        return (IPage<?>) this.requireParentFragment();
    }

    private final @NotNull List<IFragmentPart<IFragment<F>>> parts = new ArrayList<>();
    protected @NotNull List<? extends IFragmentPart<? extends IFragment<F>>> parts() {
        return this.parts;
    }

    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        this.parts().forEach(f -> f.iOnRestoreInstanceState(arguments, savedInstanceState));
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        this.parts().forEach(f -> f.iOnSaveInstanceState(outState));
    }

    @Override
    protected void iOnBuildPage(@NotNull final F page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        this.parts().forEach(IFragmentPart::iOnBuildPage);
    }

    @Override
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Creating fragment.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        this.parts().forEach(IFragmentPart::onCreate);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.parts().forEach(IFragmentPart::onStart);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.parts().forEach(IFragmentPart::onResume);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.parts().forEach(IFragmentPart::onPause);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.parts().forEach(IFragmentPart::onStop);
    }

    @Override
    public void onDestroy() {
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Destroying fragment.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        this.parts().forEach(IFragmentPart::onDestroy);
        super.onDestroy();
    }

    @Override
    public @NotNull String toString() {
        return "IFragment{" +
                "contentCache=" + this.contentCache.isInitialized() +
                '}';
    }
}
