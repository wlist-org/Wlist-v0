package com.xuxiaocheng.WList.Internal.Server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;

public final class ServerHandler {
    private ServerHandler() {
        super();
    }

    public static void doActive(final ChannelId id) {

    }

    public static void doInactive(final ChannelId id) {

    }

    public static void doRegister(final @NotNull ByteBuf buf, final Channel channel) {

    }

    public static void doLoginIn(final @NotNull ByteBuf buf, final Channel channel) {

    }

    public static void doLoginOut(final @NotNull ByteBuf buf, final Channel channel) {

    }

    public static void doList(final @NotNull ByteBuf buf, final Channel channel) {
//                if (token == null || Token.NullToken.equals(token))
//                    throw new IllegalStateException("Operate without token!");
    }
}
