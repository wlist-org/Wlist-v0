package com.xuxiaocheng.WListClient.Client;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;

public interface WListClientInterface extends AutoCloseable {
    @NotNull SocketAddress getAddress();
    @NotNull ByteBuf send(final @Nullable ByteBuf msg) throws InterruptedException;
    void close();
    boolean isActive();
}
