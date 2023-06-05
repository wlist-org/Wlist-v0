package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlInformation;
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
import java.util.EnumSet;
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
        map.put("id", u.id());
        map.put("name", u.username());
        map.put("permissions", u.group());
        return map;
    }

    public static final @NotNull ServerHandler doRegister = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final boolean success;
        try {
            success = UserManager.insertUser(new UserSqlInformation.Inserter(username, password, UserManager.getDefaultGroupId()), Thread.currentThread().getName());
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
            user = UserManager.selectUserByName(username, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || PasswordGuard.isWrongPassword(password, user.password()))
            return ServerHandler.DataError;
        final String token = UserTokenHelper.encodeToken(user.id(), user.modifyTime());
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
        if (user == null || (permissions.length > 0 && !user.group().permissions().containsAll(List.of(permissions))))
            return UnionPair.fail(ServerHandler.composeMessage(Operation.State.NoPermission, null));
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkTokenAndPassword(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, permissions);
        if (user.isFailure())
            return user;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        if (PasswordGuard.isWrongPassword(verifyingPassword, user.getT().password()))
            return UnionPair.fail(ServerHandler.DataError);
        return user;
    }

    public static final @NotNull ServerHandler doChangePassword = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        if (user.isFailure())
            return user.getE();
        try {
            UserManager.updateUser(new UserSqlInformation(user.getT().id(), user.getT().username(),
                    newPassword, user.getT().group(), user.getT().modifyTime()), Thread.currentThread().getName());
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
            UserManager.deleteUser(user.getT().id(), Thread.currentThread().getName());
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
        if (username.equals(changer.getT().username()))
            return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), changer.getT()));
        final UserSqlInformation user;
        try {
            user = UserManager.selectUserByName(username, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null)
            return UnionPair.fail(ServerUserHandler.UserNotFound);
        return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), user));
    }

    public static final @NotNull ServerHandler doListUsers = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        if (user.isFailure())
            return user.getE();
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
            return ServerHandler.WrongParameters;
        final Pair.ImmutablePair<Long, List<UserSqlInformation>> list;
        try {
            list = UserManager.selectAllUsersInPage(limit, (long) page * limit, orderDirection, Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
            for (final UserSqlInformation information: list.getSecond())
                UserSqlInformation.dumpVisible(buf, information);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDeleteUser = buffer -> {
        final UnionPair<Pair.ImmutablePair<UserSqlInformation, UserSqlInformation>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        try {
            UserManager.deleteUser(userPair.getT().getSecond().id(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static @NotNull MessageProto doChangePermission(final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
        final UnionPair<Pair.ImmutablePair<UserSqlInformation, UserSqlInformation>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        final EnumSet<Operation.Permission> modified = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
        if (modified == null)
            return ServerUserHandler.WrongPermissionsList;
        final EnumSet<Operation.Permission> permissions = userPair.getT().getSecond().group().permissions();
        if (add)
            permissions.addAll(modified);
        else
            permissions.removeAll(modified);
        try {
            UserManager.updateUser(userPair.getT().getSecond(), Thread.currentThread().getName());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    }
}
