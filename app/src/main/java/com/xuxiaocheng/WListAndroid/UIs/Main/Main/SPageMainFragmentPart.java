package com.xuxiaocheng.WListAndroid.UIs.Main.Main;

import androidx.annotation.UiThread;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragmentPart;
import com.xuxiaocheng.WListAndroid.databinding.ActivityBinding;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;

public abstract class SPageMainFragmentPart<F extends SPageMainFragment<?>> extends CFragmentPart<F> {
    protected SPageMainFragmentPart(final @NotNull F fragment) {
        super(fragment);
    }

    @Override
    protected @NotNull PageMain page() {
        return (PageMain) super.page();
    }

    @Override
    protected @NotNull ActivityMainBinding pageContent() {
        return (ActivityMainBinding) super.pageContent();
    }

    @Override
    protected @NotNull ActivityMain activity() {
        return (ActivityMain) super.activity();
    }

    @Override
    protected @NotNull ActivityBinding activityContent() {
        return (ActivityBinding) super.activityContent();
    }

    protected PageMainAdapter.@NotNull Types currentFragmentTypes() {
        return this.page().currentTypes();
    }

    @UiThread
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
    }
}
