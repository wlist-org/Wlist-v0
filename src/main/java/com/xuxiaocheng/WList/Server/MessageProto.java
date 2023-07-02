package com.xuxiaocheng.WList.Server;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record MessageProto(byte cipher, Operation.@NotNull State state, @NotNull Appender appender) {
    @FunctionalInterface
    public interface Appender {
        @NotNull ByteBuf apply(final @NotNull ByteBuf buffer) throws IOException;
    }
}
