package com.xuxiaocheng.WListAndroid.UIs.Main;

import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.IPage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public abstract class CPage<P extends ViewBinding> extends IPage<P> implements CFragmentBase {
    @Override
    public @NotNull CActivity activity() {
        return (CActivity) super.activity();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<? extends @NotNull CFragment<?>> existingFragments() {
        return (List<CFragment<?>>) super.existingFragments().stream().filter(f -> f instanceof CFragment<?>).collect(Collectors.toList());
    }

    @Override
    public void cOnConnect() {
        CFragmentBase.super.cOnConnect();
        this.existingFragments().forEach(CFragment::cOnConnect);
    }

    @Override
    public void cOnDisconnect() {
        CFragmentBase.super.cOnDisconnect();
        this.existingFragments().forEach(CFragment::cOnDisconnect);
    }
}
