package com.xuxiaocheng.WListAndroid.UIs;

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

public abstract class IFragment<P extends ViewBinding> extends Fragment {
    protected final @NotNull ActivityMain mainActivity;
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentPageCache");

    protected IFragment(final @NotNull ActivityMain mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    public @NotNull ActivityMain getMainActivity() {
        return this.mainActivity;
    }

    public @NotNull P getPage() {
        final P cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache;
        final P page = this.onCreate(this.mainActivity.getLayoutInflater());
        this.pageCache.initialize(page);
        this.onShow(page);
        return page;
    }

    @Override
    @UiThread
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        return this.getPage().getRoot();
    }

    @UiThread
    protected abstract @NotNull P onCreate(final @NotNull LayoutInflater inflater);

    @UiThread
    protected abstract void onShow(final @NotNull P page);

    @UiThread
    public void onHide() {
    }

    @UiThread
    public boolean onBackPressed() {
        return false;
    }

    @UiThread
    public void onActivityCreateHook() {
    }

    @Override
    public @NotNull String toString() {
        return "IFragment{" +
                "pageCache=" + this.pageCache +
                '}';
    }
}
