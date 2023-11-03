package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main;

import androidx.annotation.UiThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragmentPart;
import com.xuxiaocheng.WListAndroid.databinding.ActivityBinding;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;

public abstract class SPageMainFragmentPart<FV extends ViewBinding, F extends SPageMainFragment<FV>> extends CFragmentPart<F> {
    protected SPageMainFragmentPart(final @NotNull F fragment) {
        super(fragment);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull FV fragmentContent() {
        return (FV) super.fragmentContent();
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

    @UiThread
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
    }
}
