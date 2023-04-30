package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Exceptions.IllegalNetworkDataException;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ServerHandler {
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> AdminPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.stream(Operation.Permission.values()).filter(p -> p != Operation.Permission.Undefined).toList()));
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(List.of(Operation.Permission.FilesList)));

    private ServerHandler() {
        super();
    }

    public static void doActive(final ChannelId ignoredId) {
    }

    public static void doInactive(final ChannelId ignoredId) {
    }

    private static void writeMessage(final @NotNull Channel channel, final @NotNull Operation.State state, final @Nullable String message) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, state.name());
        if (message != null)
            ByteBufIOUtil.writeUTF(buffer, message);
        channel.writeAndFlush(buffer);
    }

    public static void doException(final @NotNull Channel channel, final @NotNull IllegalNetworkDataException exception) throws IOException {
        ServerHandler.writeMessage(channel, Operation.State.ServerError, Objects.requireNonNullElse(exception.getMessage(), ""));
    }

    public static void doLogin(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
        if (user == null || !UserSqlHelper.checkPassword(password, user.getA())) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        final String token = UserTokenHelper.encodeToken(username, user.getC());
        ServerHandler.writeMessage(channel, Operation.State.Success, token);
    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        if (UserSqlHelper.insertUser(username, password, ServerHandler.DefaultPermission))
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        else
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
    }

    public static void doChangePassword(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final String newPassword = ByteBufIOUtil.readUTF(buf);
        final Triad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
        if (user == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        UserSqlHelper.updateUser(user.getA(), newPassword, null);
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doLogoff(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, SQLException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buf);
        final Triad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
        if (user == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Token");
            return;
        }
        if (!UserSqlHelper.checkPassword(verifyingPassword, user.getB())) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Password");
            return;
        }
        UserSqlHelper.deleteUser(user.getA());
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    private static @Nullable Triad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> checkPermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @NotNull Operation.Permission permission) throws IOException, SQLException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final Triad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
        if (user == null || !user.getC().contains(permission)) {
            ServerHandler.writeMessage(channel, Operation.State.NoPermission, null);
            return null;
        }
        return user;
    }

    public static void doChangePermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final boolean add) throws IOException, SQLException {
        final Triad<String, String, SortedSet<Operation.Permission>> user = ServerHandler.checkPermission(buf, channel, Operation.Permission.UsersChangePermissions);
        if (user == null)
            return;
        final SortedSet<Operation.Permission> permissions = user.getC();
        if (add)
            permissions.addAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        else
            permissions.removeAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        UserSqlHelper.updateUser(user.getA(), null, permissions);
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

//    public static void doList(final @NotNull ByteBuf buf, final Channel channel) {
//        if (token == null || Token.NullToken.equals(token))
//            throw new IllegalStateException("Operate without token!");
//    }
}
