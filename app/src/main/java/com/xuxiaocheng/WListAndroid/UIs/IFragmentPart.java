package com.xuxiaocheng.WListAndroid.UIs;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class IFragmentPart<P extends ViewBinding, F extends IFragment<P, F>> {
    protected final @NotNull F fragment;

    protected IFragmentPart(final @NotNull F fragment) {
        super();
        this.fragment = fragment;
    }

    protected @NotNull P page() {
        return this.fragment.page();
    }
    protected boolean isConnected() {
        return this.fragment.isConnected();
    }
    protected @NotNull InetSocketAddress address() {
        return this.fragment.address();
    }
    protected @NotNull String username() {
        return this.fragment.username();
    }
    protected @NotNull WListClientInterface client() throws IOException {
        return this.fragment.client();
    }
    protected @NotNull String token() {
        return this.fragment.token();
    }
    protected @NotNull ActivityMain activity() {
        return this.fragment.activity();
    }

    @UiThread
    protected void onBuild(final @NotNull P page) {
    }

    @UiThread
    protected void onShow(final @NotNull ActivityMain activity) {
    }

    @UiThread
    protected void onHide(final @NotNull ActivityMain activity) {
    }

    @UiThread
    protected void onActivityCreateHook(final @NotNull ActivityMain activity) {
    }

    @WorkerThread
    protected void onConnected(final @NotNull ActivityMain activity) {
    }

    @WorkerThread
    protected void onDisconnected(final @NotNull ActivityMain activity) {
    }

    @Override
    public @NotNull String toString() {
        return "IFragmentPart{" +
                "super=" + super.toString() +
                '}';
    }
}
