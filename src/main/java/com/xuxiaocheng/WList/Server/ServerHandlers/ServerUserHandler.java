package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlInformationUpdater;
import com.xuxiaocheng.WList.Server.Databases.User.VisibleUserInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.UserTokenHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ServerUserHandler {
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> AdminPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.stream(Operation.Permission.values()).filter(p -> p != Operation.Permission.Undefined).toList()));
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(List.of(Operation.Permission.FilesList)));

    public static final @NotNull MessageProto UserNotFound = ServerHandler.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto WrongPermissionsList = ServerHandler.composeMessage(Operation.State.DataError, "Permissions");

    private ServerUserHandler() {
        super();
    }

    static @NotNull Map<String, Object> getVisibleInfo(final @NotNull UserSqlInformation u) {
        final Map<String, Object> map = new LinkedHashMap<>(3);
        map.put("id", u.getId());
        map.put("name", u.getUsername());
        map.put("permissions", u.getPermissions());
        return map;
    }

    public static final @NotNull ServerHandler doRegister = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final boolean success;
        try {
            success = UserSqlHelper.insertUser(new UserSqlInformationUpdater(username, password, null), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(success ? Operation.State.Success : Operation.State.DataError, null);
    };

    public static final @NotNull ServerHandler doLogin = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final UserSqlInformation user;
        try {
            user = UserSqlHelper.selectUserByName(username, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || PasswordGuard.isWrongPassword(password, user.getPassword()))
            return ServerHandler.DataError;
        final String token = UserTokenHelper.encodeToken(user.getId(), user.getModifyTime());
        HLog.getInstance("ServerLogger").log(HLogLevel.DEBUG, "Signed token for user: ", username, " token: ", token);
        return ServerHandler.composeMessage(Operation.State.Success, token);
    };

    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkToken(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UserSqlInformation user;
        try {
            user = UserTokenHelper.decodeToken(token);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || (permissions.length > 0 && !user.getPermissions().containsAll(List.of(permissions))))
            return UnionPair.fail(ServerHandler.composeMessage(Operation.State.NoPermission, null));
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkTokenAndPassword(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, permissions);
        if (user.isFailure())
            return user;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        if (PasswordGuard.isWrongPassword(verifyingPassword, user.getT().getPassword()))
            return UnionPair.fail(ServerHandler.DataError);
        return user;
    }

    public static final @NotNull ServerHandler doChangePassword = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        if (user.isFailure())
            return user.getE();
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        user.getT().setPassword(newPassword);
        try {
            UserSqlHelper.updateUser(user.getT(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doLogoff = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        if (user.isFailure())
            return user.getE();
        try {
            UserSqlHelper.deleteUser(user.getT().getId(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    static @NotNull UnionPair<Pair.@NotNull ImmutablePair<@NotNull UserSqlInformation, @NotNull UserSqlInformation>, @NotNull MessageProto> checkChangerTokenAndUsername(final @NotNull ByteBuf buffer, final Operation.@NotNull Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, permissions);
        if (changer.isFailure())
            return UnionPair.fail(changer.getE());
        final String username = ByteBufIOUtil.readUTF(buffer);
        if (username.equals(changer.getT().getUsername()))
            return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), changer.getT()));
        final UserSqlInformation user;
        try {
            user = UserSqlHelper.selectUserByName(username, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null)
            return UnionPair.fail(ServerUserHandler.UserNotFound);
        return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), user));
    }

    public static final @NotNull ServerHandler doListUsers = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        if (user.isFailure())
            return user.getE();
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
            return ServerHandler.WrongParameters;
        final Pair.ImmutablePair<Long, List<UserSqlInformation>> list;
        try {
            list = UserSqlHelper.selectAllUsersInPage(limit, (long) page * limit, orderDirection, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
            for (final UserSqlInformation information: list.getSecond())
                VisibleUserInformation.dump(buf, information);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDeleteUser = buffer -> {
        final UnionPair<Pair.ImmutablePair<UserSqlInformation, UserSqlInformation>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        try {
            UserSqlHelper.deleteUserByName(userPair.getT().getSecond().getUsername(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static @NotNull MessageProto doChangePermission(final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
        final UnionPair<Pair.ImmutablePair<UserSqlInformation, UserSqlInformation>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        final SortedSet<Operation.Permission> modified = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
        if (modified == null)
            return ServerUserHandler.WrongPermissionsList;
        final SortedSet<Operation.Permission> permissions = userPair.getT().getSecond().getPermissions();
        if (add)
            permissions.addAll(modified);
        else
            permissions.removeAll(modified);
        try {
            UserSqlHelper.updateUser(userPair.getT().getSecond(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    }
}
