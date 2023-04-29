package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Exceptions.IllegalNetworkDataException;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.SortedSet;

final class ServerHandler {
    private ServerHandler() {
        super();
    }

    public static void doActive(final ChannelId ignoredId) {
    }

    public static void doInactive(final ChannelId ignoredId) {
    }

    private static void writeOnlyState(final @NotNull Channel channel, final @NotNull Operation.State state) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, state.name());
        channel.writeAndFlush(buffer);
    }

    public static void doException(final @NotNull Channel channel, final @NotNull IllegalNetworkDataException exception) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, Operation.State.ServerError.name());
        ByteBufIOUtil.writeUTF(buffer, exception.getMessage());
        channel.writeAndFlush(buffer);
    }

    public static void doLogin(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String token = UserManager.doLogin(buf);
        if (token == null) {
            ServerHandler.writeOnlyState(channel, Operation.State.DataError);
            return;
        }
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeUTF(buffer, token);
        channel.writeAndFlush(buffer);
    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        if (UserManager.doRegister(buf))
            ServerHandler.writeOnlyState(channel, Operation.State.Success);
        else
            ServerHandler.writeOnlyState(channel, Operation.State.DataError);
    }

    private static @Nullable Pair<@NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> checkPermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @NotNull Operation.Permission permission) throws IOException, SQLException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final Pair<String, SortedSet<Operation.Permission>> user = UserTokenHelper.resolveToken(token);
        if (user == null || !user.getSecond().contains(permission)) {
            ServerHandler.writeOnlyState(channel, Operation.State.NoPermission);
            return null;
        }
        return user;
    }

    public static void doChangePermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final boolean add) throws IOException, SQLException {
        final Pair<@NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> user = ServerHandler.checkPermission(buf, channel, Operation.Permission.UsersChangePermissions);
        if (user == null)
            return;
        final SortedSet<Operation.Permission> permissions = user.getSecond();
        if (add)
            permissions.addAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        else
            permissions.removeAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        UserSqlHelper.updateUser(user.getFirst(), null, permissions);
        ServerHandler.writeOnlyState(channel, Operation.State.Success);
    }

//    public static void doList(final @NotNull ByteBuf buf, final Channel channel) {
//        if (token == null || Token.NullToken.equals(token))
//            throw new IllegalStateException("Operate without token!");
//    }
}
