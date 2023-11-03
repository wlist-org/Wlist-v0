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

public abstract class IPage<P extends ViewBinding> extends FragmentBase<P> {
    @Override
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Creating page.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Destroying page.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        super.onDestroy();
    }

    @Override
    public @NotNull String toString() {
        return "IPage{" +
                "contentCache=" + this.contentCache.isInitialized() +
                '}';
    }
}
