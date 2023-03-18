package com.xuxiaocheng.WList.Internal.Server;

import com.xuxiaocheng.WList.Internal.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;

public final class ServerHandler {
    private ServerHandler() {
        super();
    }

    public static void doActive(final ChannelId id) {

    }

    public static void doInactive(final ChannelId id) {

    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(1);
        ByteBufIOUtil.writeVariableLenInt(buffer, 1); // Unsupported operation.
        channel.writeAndFlush(buffer);
//        final String token = TokenManager.doRegister(buf);
//        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(149);
//        ByteBufIOUtil.writeVariableLenInt(buffer, 0); // Success. TODO: State
//        ByteBufIOUtil.writeUTF(buffer, token);
//        channel.writeAndFlush(buffer);
    }

    public static void doLoginIn(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws SQLException, IOException {
        final String token = TokenManager.doLoginIn(buf);
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(149);
        ByteBufIOUtil.writeVariableLenInt(buffer, 0); // Success.
        ByteBufIOUtil.writeUTF(buffer, token);
        channel.writeAndFlush(buffer);
    }

    public static void doLoginOut(final @NotNull ByteBuf buf, final Channel channel) {

    }

    public static void doList(final @NotNull ByteBuf buf, final Channel channel) {
//        if (token == null || Token.NullToken.equals(token))
//            throw new IllegalStateException("Operate without token!");
    }
}
