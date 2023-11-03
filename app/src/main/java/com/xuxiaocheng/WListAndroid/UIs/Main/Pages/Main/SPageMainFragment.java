package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main;

import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import org.jetbrains.annotations.NotNull;

public abstract class SPageMainFragment<F extends ViewBinding> extends CFragment<F> {
    @Override
    public @NotNull PageMain page() {
        return (PageMain) super.page();
    }

    @UiThread
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
    }
}
