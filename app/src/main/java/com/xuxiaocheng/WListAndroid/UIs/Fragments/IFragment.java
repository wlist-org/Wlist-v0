package com.xuxiaocheng.WListAndroid.UIs.Fragments;

import android.content.Context;
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
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMainAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IFragment<P extends ViewBinding, F extends IFragment<P, F>> extends Fragment {
    protected final @NotNull HInitializer<P> fragmentCache = new HInitializer<>("FragmentCache");
    public @NotNull P fragment() {
        return this.fragmentCache.getInstance();
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
        this.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void setArguments(final @Nullable Bundle args) {
        super.setArguments(args);
        this.onHandleArguments(args);
    }

    @UiThread
    protected void onHandleArguments(final @Nullable Bundle arguments) {
        if (arguments != null)
            BundleHelper.restoreClient(arguments, "wlist:i_fragment:client", this.address, this.username, null);
        this.parts.forEach(f -> f.onHandleArguments(arguments));
    }

    @Override
    @UiThread
    public @NotNull View onCreateView(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        final P fragment = this.inflate(inflater);
        this.fragmentCache.reinitialize(fragment);
        this.onBuild(fragment);
        return fragment.getRoot();
    }

    @UiThread
    protected abstract @NotNull P inflate(final @NotNull LayoutInflater inflater);

    @Override
    @UiThread
    public void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        this.parts.forEach(f -> f.onSaveInstanceState(outState));
    }

    @UiThread
    public void onRestoreInstanceState(final @Nullable Bundle savedInstanceState) {
        this.parts.forEach(f -> f.onRestoreInstanceState(savedInstanceState));
    }

    @UiThread
    protected void onBuild(final @NotNull P fragment) {
        this.parts.forEach(f -> f.onBuild(fragment));
    }

    @UiThread
    public void onPositionChanged(final @NotNull ActivityMain activity, final ActivityMainAdapter.@NotNull FragmentTypes position) {
        this.parts.forEach(f -> f.onPositionChanged(activity, position));
    }

    @UiThread
    public boolean onBackPressed(final @NotNull ActivityMain activity) {
        return false;
    }

    @Override
    public void onAttach(final @NotNull Context context) {
        super.onAttach(context);
        this.parts.forEach(IFragmentPart::onAttach);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.parts.forEach(IFragmentPart::onDetach);
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
                "pageCache=" + this.fragmentCache.isInitialized() +
                ", address=" + this.address +
                ", username=" + this.username +
                ", connected=" + this.connected +
                ", hash=" + super.hashCode() +
                '}';
    }
}
