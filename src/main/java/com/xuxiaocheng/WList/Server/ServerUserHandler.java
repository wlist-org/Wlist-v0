package com.xuxiaocheng.WList.Server;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ServerUserHandler {
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> AdminPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.stream(Operation.Permission.values()).filter(p -> p != Operation.Permission.Undefined).toList()));
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(List.of(Operation.Permission.FilesList)));

    private ServerUserHandler() {
        super();
    }

    public static void doLogin(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime> user;
        try {
            user = UserSqlHelper.selectUser(username);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || UserSqlHelper.isWrongPassword(password, user.getA())) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        final String token = UserTokenHelper.encodeToken(username, user.getC());
        ServerHandler.writeMessage(channel, Operation.State.Success, token);
    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final boolean conflict;
        try {
            conflict = !UserSqlHelper.insertUser(username, password, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (conflict) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    static Triad.@Nullable ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> checkToken(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @NotNull Operation.Permission... permission) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user;
        try {
            user = UserTokenHelper.decodeToken(token);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || (permission.length > 0 && !user.getC().containsAll(List.of(permission)))) {
            ServerHandler.writeMessage(channel, Operation.State.NoPermission, null);
            return null;
        }
        return user;
    }

    static Triad.@Nullable ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> checkTokenAndPassword(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @NotNull Operation.Permission... permission) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, permission);
        if (user == null)
            return null;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buf);
        if (UserSqlHelper.isWrongPassword(verifyingPassword, user.getB())) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return null;
        }
        return user;
    }

    public static void doChangePassword(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkTokenAndPassword(buf, channel);
        if (user == null)
            return;
        final String newPassword = ByteBufIOUtil.readUTF(buf);
        try {
            UserSqlHelper.updateUser(user.getA(), newPassword, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doLogoff(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkTokenAndPassword(buf, channel);
        if (user == null)
            return;
        try {
            UserSqlHelper.deleteUser(user.getA());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doListUsers(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkTokenAndPassword(buf, channel);
        if (user == null)
            return;
        final List<Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime>> list;
        try {
            list = UserSqlHelper.selectAllUsers();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(list.stream()
                .map(u -> {
                    final Map<String, Object> map = new HashMap<>(2);
                    map.put("name", u.getA());
                    map.put("permissions", u.getB());
                    return map;
                }).collect(Collectors.toSet())));
    }

    static Pair.@Nullable ImmutablePair<Triad.@Nullable ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>>, Triad.@Nullable ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>>> checkChangerTokenAndUsername(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @NotNull Operation.Permission... permission) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> changer = ServerUserHandler.checkToken(buf, channel, permission);
        if (changer == null)
            return null;
        final String username = ByteBufIOUtil.readUTF(buf);
        if (username.equals(changer.getA()))
            return Pair.ImmutablePair.makeImmutablePair(changer, changer);
        final Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime> user;
        try {
            user = UserSqlHelper.selectUser(username);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return null;
        }
        return Pair.ImmutablePair.makeImmutablePair(changer, Triad.ImmutableTriad.makeImmutableTriad(username, user.getA(), user.getB()));
    }

    public static void doDeleteUser(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Pair.ImmutablePair<Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>>, Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>>> userPair = ServerUserHandler.checkChangerTokenAndUsername(buf, channel, Operation.Permission.UsersOperate);
        if (userPair == null)
            return;
        try {
            UserSqlHelper.deleteUser(userPair.getSecond().getA());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doChangePermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final boolean add) throws IOException, ServerException {
        final Pair.ImmutablePair<Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>>, Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>>> userPair = ServerUserHandler.checkChangerTokenAndUsername(buf, channel, Operation.Permission.UsersOperate);
        if (userPair == null)
            return;
        final SortedSet<Operation.Permission> permissions = userPair.getSecond().getC();
        if (add)
            permissions.addAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        else
            permissions.removeAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
        try {
            UserSqlHelper.updateUser(userPair.getSecond().getA(), null, permissions);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }
}
