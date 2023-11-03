package com.xuxiaocheng.WListAndroid.UIs.Main;

import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CFragment<F extends ViewBinding> extends IFragment<F> implements CFragmentBase {
    @Override
    public @NotNull CActivity activity() {
        return (CActivity) super.activity();
    }

    public @NotNull CPage<?> page() {
        return (CPage<?>) super.page();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull List<? extends CFragmentPart<? extends CFragment<F>>> parts() {
        return (List<? extends CFragmentPart<? extends CFragment<F>>>) super.parts();
    }

    @Override
    public void cOnConnect() {
        CFragmentBase.super.cOnConnect();
        this.parts().forEach(CFragmentPart::cOnConnect);
    }

    @Override
    public void cOnDisconnect() {
        CFragmentBase.super.cOnDisconnect();
        this.parts().forEach(CFragmentPart::cOnDisconnect);
    }
}
