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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IFragment<P extends ViewBinding, F extends IFragment<P, F>> extends Fragment {
    protected final @NotNull HInitializer<P> pageCache = new HInitializer<>("FragmentPageCache");
    public @NotNull P page() {
        return this.pageCache.getInstance();
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("FragmentAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("FragmentUsername");
    protected final @NotNull AtomicBoolean connected = new AtomicBoolean(false);
    public boolean isConnected() {
        return this.connected.get();
    }
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
    public @NotNull ActivityMain activity() {
        return (ActivityMain) this.requireActivity();
    }

    protected final @NotNull List<@NotNull IFragmentPart<P, F>> parts = new ArrayList<>();

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = this.getArguments();
        if (bundle != null)
            BundleHelper.restoreClient(bundle, this.address, this.username, null);
    }

    @Override
    @UiThread
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        final P page = this.inflate(inflater);
        this.pageCache.reinitialize(page);
        this.onBuild(page);
        return page.getRoot();
    }

    @UiThread
    protected abstract @NotNull P inflate(final @NotNull LayoutInflater inflater);

    @UiThread
    public boolean onBackPressed() {
        return false;
    }

    @UiThread
    protected void onBuild(final @NotNull P page) {
        this.parts.forEach(f -> f.onBuild(page));
    }

    @UiThread
    public void onShow(final @NotNull ActivityMain activity) {
        this.parts.forEach(f -> f.onShow(activity));
    }

    @UiThread
    public void onHide(final @NotNull ActivityMain activity) {
        this.parts.forEach(f -> f.onHide(activity));
    }

    @UiThread
    public void onActivityCreateHook(final @NotNull ActivityMain activity) {
        this.parts.forEach(f -> f.onActivityCreateHook(activity));
    }

    @WorkerThread
    public void onConnected(final @NotNull ActivityMain activity) {
        this.connected.set(true);
        this.parts.forEach(f -> f.onConnected(activity));
    }

    @WorkerThread
    public void onDisconnected(final @NotNull ActivityMain activity) {
        this.connected.set(false);
        this.parts.forEach(f -> f.onDisconnected(activity));
    }

    @Override
    public @NotNull String toString() {
        return "IFragment{" +
                "pageCache=" + this.pageCache.isInitialized() +
                ", address=" + this.address +
                ", username=" + this.username +
                ", connected=" + this.connected +
                '}';
    }
}
