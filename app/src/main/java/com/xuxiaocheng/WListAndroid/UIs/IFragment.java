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
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class IFragment<P extends ViewBinding> extends Fragment {
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentPageCache");
    public @NotNull P getPage() {
        return this.pageCache.getInstance();
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("FragmentAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("FragmentUsername");
    public @NotNull InetSocketAddress address() {
        return this.address.getInstance();
    }
    public @NotNull String username() {
        return this.username.getInstance();
    }
    public @NotNull WListClientInterface client() throws IOException {
        return WListClientManager.quicklyGetClient(this.address());
    }
    public @NotNull String token() {
        return TokenAssistant.getToken(this.address(), this.username());
    }

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = this.getArguments();
        if (bundle != null)
            BundleHelper.restoreClient(bundle, this.address, this.username, null);
    }

    public @NotNull ActivityMain activity() {
        return (ActivityMain) this.requireActivity();
    }

    public @Nullable ActivityMain activityNullable() {
        return (ActivityMain) this.getActivity();
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
    public void onShow(final @NotNull ActivityMain activity) {
    }

    @UiThread
    public void onHide(final @NotNull ActivityMain activity) {
    }

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
                "pageCache=" + this.pageCache.isInitialized() +
                '}';
    }
}
