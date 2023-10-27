package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public abstract class IFragment<P extends ViewBinding> extends Fragment {
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentPageCache");

    public @NotNull ActivityMain activity() {
        return (ActivityMain) this.requireActivity();
    }
    public @NotNull P getPage() {
        return this.pageCache.getInstance();
    }
    public @NotNull InetSocketAddress address() {
        return this.activity().address();
    }
    public @NotNull String username() {
        return this.activity().username();
    }

    @Override
    @UiThread
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        final P page = this.onCreate(inflater);
        this.pageCache.reinitialize(page);
        this.onBuild(page);
        return page.getRoot();
    }

    @UiThread
    protected abstract @NotNull P onCreate(final @NotNull LayoutInflater inflater);

    @UiThread
    protected abstract void onBuild(final @NotNull P page);

    @UiThread
    public boolean onBackPressed() {
        return false;
    }

    @UiThread
    public void onActivityCreateHook(final @NotNull ActivityMain activity) {
    }

    @WorkerThread
    public void onConnected(final @NotNull ActivityMain activity) {
    }

    @WorkerThread
    public void onDisconnected(final @NotNull ActivityMain activity) {
    }

    @Override
    public @NotNull String toString() {
        return "IFragment{" +
                "pageCache=" + this.pageCache +
                '}';
    }
}
