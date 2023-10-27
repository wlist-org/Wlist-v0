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
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class IFragment<P extends ViewBinding> extends Fragment {
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentPageCache");

    public @NotNull P getPage() {
        return this.pageCache.getInstance();
    }
    public @NotNull InetSocketAddress address(final @NotNull ActivityMain activity) {
        return activity.address();
    }
    public @NotNull String username(final @NotNull ActivityMain activity) {
        return activity.username();
    }
    public @NotNull WListClientInterface client(final @NotNull ActivityMain activity) throws IOException {
        return WListClientManager.quicklyGetClient(activity.address());
    }
    public @NotNull String token(final @NotNull ActivityMain activity) {
        return TokenAssistant.getToken(activity.address(), activity.username());
    }

    /**
     * Inject Only.
     */
    @Deprecated
    protected @NotNull ActivityMain activity() {
        return (ActivityMain) this.requireActivity();
    }

    @Override
    @UiThread
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        final P page = this.onCreate(inflater);
        this.pageCache.reinitialize(page);
        this.onBuild(this.activity(), page);
        return page.getRoot();
    }

    @UiThread
    protected abstract @NotNull P onCreate(final @NotNull LayoutInflater inflater);

    @UiThread
    protected abstract void onBuild(final @NotNull ActivityMain activity, final @NotNull P page);

    @UiThread
    public void onShow(final @NotNull ActivityMain activity) {
    }

    @UiThread
    public void onHide(final @NotNull ActivityMain activity) {
    }

    @UiThread
    public boolean onBackPressed(final @NotNull ActivityMain activity) {
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
