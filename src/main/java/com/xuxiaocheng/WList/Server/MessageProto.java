package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.WList.Server.Operation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record MessageProto(byte cipher, @NotNull Operation.State state, @NotNull Appender appender) {
    @FunctionalInterface
    public interface Appender {
        @NotNull ByteBuf apply(final @NotNull ByteBuf buffer) throws IOException;
    }
}
