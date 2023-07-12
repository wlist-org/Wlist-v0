package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class ServerDefault {
    private ServerDefault() {
        super();
    }

    public static void initialize() throws InterruptedException, ConnectException {
        final SocketAddress address = ServerDefault.ensureServerOpened();
        ClientManager.initialize(new ClientManager.ClientManagerConfig(address, 1, 2, 64));
    }

    @NonNull
    public static SocketAddress ensureServerOpened() {
        return new InetSocketAddress("192.168.110.217", 5212);
    }
}
