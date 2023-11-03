package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main;

import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SPageMainFragment<F extends ViewBinding> extends CFragment<F> {
    @Override
    public @NotNull ActivityMain activity() {
        return (ActivityMain) super.activity();
    }

    @Override
    public @NotNull PageMain page() {
        return (PageMain) super.page();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull List<? extends SPageMainFragmentPart<? extends SPageMainFragment<F>>> parts() {
        return (List<? extends SPageMainFragmentPart<? extends SPageMainFragment<F>>>) super.parts();
    }

    protected PageMainAdapter.@NotNull Types currentFragmentTypes() {
        return this.page().currentTypes();
    }

    @UiThread
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
        this.parts().forEach(p -> p.sOnTypeChanged(type));
    }
}
