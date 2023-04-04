package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
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

    public static void doException(final @NotNull Channel channel, final @NotNull IllegalNetworkDataException exception) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(256);
        ByteBufIOUtil.writeByte(buffer, Operation.State.DataError.getId());
        ByteBufIOUtil.writeUTF(buffer, exception.getMessage());
        channel.writeAndFlush(buffer);
    }

    public static void doLoginIn(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws SQLException, IOException {
        final String token = UserManager.doLoginIn(buf);
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(149);
        ByteBufIOUtil.writeByte(buffer, Operation.State.Success.getId());
        ByteBufIOUtil.writeUTF(buffer, token);
        channel.writeAndFlush(buffer);
    }

    public static void doLoginOut(final @NotNull ByteBuf buf, final Channel channel) throws IOException {
        ServerHandler.writeOnlyState(channel, Operation.State.Success);
    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String token = ByteBufIOUtil.readUTF(buf);
        if (!UserManager.getPermissions(token).contains(Operation.Permission.UsersAdd)) {
            ServerHandler.writeOnlyState(channel, Operation.State.NoPermission);
            return;
        }
        UserManager.doRegister(buf);
        ServerHandler.writeOnlyState(channel, Operation.State.Success);
    }

//    public static void doList(final @NotNull ByteBuf buf, final Channel channel) {
//        if (token == null || Token.NullToken.equals(token))
//            throw new IllegalStateException("Operate without token!");
//    }

    private static void writeOnlyState(final @NotNull Channel channel, final @NotNull Operation.State state) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(1);
        ByteBufIOUtil.writeByte(buffer, state.getId());
        channel.writeAndFlush(buffer);
    }
}
