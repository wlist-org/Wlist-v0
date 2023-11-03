package com.xuxiaocheng.WListAndroid.UIs.Main;

import androidx.annotation.WorkerThread;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WListAndroid.UIs.IFragmentPart;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class CFragmentPart<F extends CFragment<?>> extends IFragmentPart<F> {
    protected CFragmentPart(final @NotNull F fragment) {
        super(fragment);
    }

    @Override
    protected @NotNull CPage<?> page() {
        return (CPage<?>) super.page();
    }

    @Override
    protected @NotNull CActivity activity() {
        return (CActivity) super.activity();
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

    @WorkerThread
    public void cOnConnect() {
    }

    @WorkerThread
    public void cOnDisconnect() {
    }
}
