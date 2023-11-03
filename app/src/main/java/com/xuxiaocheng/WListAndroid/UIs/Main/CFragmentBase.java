package com.xuxiaocheng.WListAndroid.UIs.Main;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;

@FunctionalInterface
public interface CFragmentBase {
    @NotNull CActivity activity();

    @AnyThread
    default boolean isConnected() {
        return this.activity().isConnected();
    }
    @WorkerThread
    default @NotNull InetSocketAddress address() {
        return this.activity().address();
    }
    @WorkerThread
    default @NotNull String username() {
        return this.activity().username();
    }
    @WorkerThread
    default @NotNull WListClientInterface client() throws IOException {
        return this.activity().client();
    }
    @WorkerThread
    default @NotNull String token() {
        return this.activity().token();
    }

    @WorkerThread
    default void cOnConnect() {
    }

    @WorkerThread
    default void cOnDisconnect() {
    }
}
