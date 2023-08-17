package com.xuxiaocheng.WListClient.Client;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketAddress;

public interface WListClientInterface extends AutoCloseable {
    void open() throws IOException, InterruptedException;
    @NotNull SocketAddress getAddress();
    @NotNull ByteBuf send(final @Nullable ByteBuf msg) throws IOException, InterruptedException;
    void close();
    boolean isActive();
}
