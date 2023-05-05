package com.xuxiaocheng.WList.Server;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    public static void writeMessage(final @NotNull Channel channel, final @NotNull Operation.State state, final @Nullable String message) throws ServerException {
        try {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, state.name());
            if (message != null)
                ByteBufIOUtil.writeUTF(buffer, message);
            channel.writeAndFlush(buffer);
        } catch (final IOException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doException(final @NotNull Channel channel, final @Nullable String message) {
        try {
            ServerHandler.writeMessage(channel, Operation.State.ServerError, Objects.requireNonNullElse(message, ""));
        } catch (final ServerException exception) {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
            channel.close();
        }
    }

    public static void doLogin(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        try {
            final Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
            if (user == null || UserSqlHelper.isWrongPassword(password, user.getA())) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            final String token = UserTokenHelper.encodeToken(username, user.getC());
            ServerHandler.writeMessage(channel, Operation.State.Success, token);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doRegister(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        try {
            if (UserSqlHelper.insertUser(username, password, ServerHandler.DefaultPermission))
                ServerHandler.writeMessage(channel, Operation.State.Success, null);
            else
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doChangePassword(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final String newPassword = ByteBufIOUtil.readUTF(buf);
        try {
            final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
            if (user == null) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            UserSqlHelper.updateUser(user.getA(), newPassword, null);
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doLogoff(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buf);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buf);
        try {
            final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
            if (user == null) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, "Token");
                return;
            }
            if (UserSqlHelper.isWrongPassword(verifyingPassword, user.getB())) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, "Password");
                return;
            }
            UserSqlHelper.deleteUser(user.getA());
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    private static @Nullable Triad.ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> getAndCheckPermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @Nullable Operation.Permission... permission) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buf);
        try {
            final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = UserTokenHelper.decodeToken(token);
            if (user == null || (permission != null && !user.getC().containsAll(List.of(permission)))) {
                ServerHandler.writeMessage(channel, Operation.State.NoPermission, null);
                return null;
            }
            return user;
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doChangePermission(final @NotNull ByteBuf buf, final @NotNull Channel channel, final boolean add) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> changer = ServerHandler.getAndCheckPermission(buf, channel, Operation.Permission.UsersChangePermissions);
        if (changer == null)
            return;
        final String username = ByteBufIOUtil.readUTF(buf);
        try {
            final SortedSet<Operation.Permission> permissions;
            if (username.equals(changer.getA()))
                permissions = changer.getC();
            else {
                final Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
                if (user == null) {
                    ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                    return;
                }
                permissions = user.getB();
            }
            if (add)
                permissions.addAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
            else
                permissions.removeAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buf)));
            UserSqlHelper.updateUser(username, null, permissions);
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
    }

    private static @Nullable Pair.ImmutablePair<@NotNull DriverInterface<?>, @NotNull DrivePath> getDriverPath(final @NotNull ByteBuf buf, final @NotNull Channel channel, final @Nullable Operation.Permission... permission) throws IOException, ServerException {
        if (ServerHandler.getAndCheckPermission(buf, channel, permission) == null)
            return null;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final DriverInterface<?> driver = DriverManager.get(path.getRoot());
        if (driver == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return null;
        }
        return Pair.ImmutablePair.makeImmutablePair(driver, path.removedRoot());
    }

    public static void doListFiles(final @NotNull ByteBuf buf, final Channel channel) throws IOException, ServerException {
        final Pair.ImmutablePair<DriverInterface<?>, DrivePath> path = ServerHandler.getDriverPath(buf, channel, Operation.Permission.FilesList);
        if (path == null)
            return;
        final int limit = ByteBufIOUtil.readVariableLenInt(buf);
        final int page = ByteBufIOUtil.readVariableLenInt(buf);
        final String direction = ByteBufIOUtil.readUTF(buf);
        final String policy = ByteBufIOUtil.readUTF(buf);
        final OrderDirection orderDirection = OrderDirection.Map.get(direction);
        final OrderPolicy orderPolicy = OrderPolicy.Map.get(policy);
        if (limit < 1 || limit > GlobalConfiguration.getInstance().getMax_limit() || page < 0
                || (orderDirection == null && !"D".equals(direction))
                || (orderPolicy == null && !"D".equals(policy))) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Parameters");
            return;
        }
        final Pair.ImmutablePair<Integer, List<FileInformation>> list;
        try {
            list = path.getFirst().list(path.getSecond(), limit, page, orderDirection, orderPolicy);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (list == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return;
        }
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenInt(buffer, list.getFirst().intValue());
        ByteBufIOUtil.writeUTF(buffer, JSON.toJSONString(list.getSecond().stream().map(f -> {
            final Map<String, Object> map = new HashMap<>(4);
            map.put("name", f.path().getName());
            map.put("size", f.size());
            if (f.createTime() != null)
                map.put("create_time", f.createTime().format(DateTimeFormatter.ISO_DATE_TIME));
            if (f.updateTime() != null)
                map.put("update_time", f.updateTime().format(DateTimeFormatter.ISO_DATE_TIME));
            return map;
        }).collect(Collectors.toSet())));
        channel.writeAndFlush(buffer);
    }

    public static void doRequestDownloadFile(final @NotNull ByteBuf buf, final Channel channel) throws IOException, ServerException {
        final Pair.ImmutablePair<DriverInterface<?>, DrivePath> path = ServerHandler.getDriverPath(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileDownload);
        if (path == null)
            return;
        final long from = ByteBufIOUtil.readVariableLenLong(buf);
        final long to = ByteBufIOUtil.readVariableLenLong(buf);
        if (from < 0 || from >= to) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Parameters");
            return;
        }
        final Pair.ImmutablePair<InputStream, Long> url;
        try {
            url = path.getFirst().download(path.getSecond(), from, to);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return;
        }
        final long id = DownloadIdHelper.generateId(url.getFirst());
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenLong(buffer, url.getSecond().longValue());
        ByteBufIOUtil.writeLong(buffer, id);
        channel.writeAndFlush(buffer);
    }

    public static void doDownloadFile(final @NotNull ByteBuf buf, final Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerHandler.getAndCheckPermission(buf, channel, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final long id = ByteBufIOUtil.readLong(buf);
        try {
            final Pair.ImmutablePair<Integer, ByteBuf> file = DownloadIdHelper.download(id);
            if (file == null) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
            ByteBufIOUtil.writeVariableLenInt(buffer, file.getFirst().intValue());
            ByteBufIOUtil.writeVariableLenInt(buffer, file.getSecond().readableBytes());
            channel.write(buffer);
            channel.writeAndFlush(file.getSecond());
        } catch (final InterruptedException | IOException | ExecutionException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doCancelDownloadFile(final @NotNull ByteBuf buf, final Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerHandler.getAndCheckPermission(buf, channel, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final long id = ByteBufIOUtil.readLong(buf);
        if (DownloadIdHelper.cancel(id))
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        else
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
    }

    // TODO
}
