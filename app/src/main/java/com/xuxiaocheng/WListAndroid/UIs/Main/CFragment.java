package com.xuxiaocheng.WListAndroid.UIs.Main;

import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import org.jetbrains.annotations.NotNull;

public abstract class CFragment<F extends ViewBinding> extends IFragment<F> implements CFragmentBase {
    @Override
    public @NotNull CActivity activity() {
        return (CActivity) super.activity();
    }

    public @NotNull CPage<?> page() {
        return (CPage<?>) super.page();
    }
}
