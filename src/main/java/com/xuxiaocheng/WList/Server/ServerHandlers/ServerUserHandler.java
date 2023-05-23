package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.DataAccessObjects.UserInformation;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.Polymers.UserSqlInfo;
import com.xuxiaocheng.WList.Server.Polymers.UserTokenInfo;
import com.xuxiaocheng.WList.DataAccessObjects.UserSqlHelper;
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
import java.util.stream.Collectors;

public final class ServerUserHandler {
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> AdminPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.stream(Operation.Permission.values()).filter(p -> p != Operation.Permission.Undefined).toList()));
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(List.of(Operation.Permission.FilesList)));

    public static final @NotNull MessageProto NoPermission = ServerHandler.composeMessage(Operation.State.NoPermission, null);
    public static final @NotNull MessageProto WrongVerifyPassword = ServerHandler.composeMessage(Operation.State.DataError, null);
    public static final @NotNull MessageProto NoSuchUser = ServerHandler.composeMessage(Operation.State.DataError, null);

    private ServerUserHandler() {
        super();
    }

    static @NotNull Map<String, Object> getVisibleInfo(final @NotNull UserInformation u) {
        final Map<String, Object> map = new LinkedHashMap<>(3);
        map.put("id", u.id());
        map.put("name", u.username());
        map.put("permissions", u.permissions());
        return map;
    }

    public static final @NotNull ServerHandler doRegister = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final boolean success;
        try {
            success = UserSqlHelper.insertUser(username, password, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(success ? Operation.State.Success : Operation.State.DataError, null);
    };

    public static final @NotNull ServerHandler doLogin = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final UserSqlInfo user;
        try {
            user = UserSqlHelper.selectUser(username);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || UserSqlHelper.isWrongPassword(password, user.password()))
            return ServerHandler.composeMessage(Operation.State.DataError, null);
        final String token = UserTokenHelper.encodeToken(username, user.modifyTime());
        HLog.getInstance("ServerLogger").log(HLogLevel.DEBUG, "Signed token for user: ", username, " token: ", token);
        return ServerHandler.composeMessage(Operation.State.Success, token);
    };

    static @NotNull UnionPair<@NotNull UserTokenInfo, @NotNull MessageProto> checkToken(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UserTokenInfo user;
        try {
            user = UserTokenHelper.decodeToken(token);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || (permissions.length > 0 && !user.permissions().containsAll(List.of(permissions))))
            return UnionPair.fail(ServerUserHandler.NoPermission);
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<@NotNull UserTokenInfo, @NotNull MessageProto> checkTokenAndPassword(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, permissions);
        if (user.isFailure())
            return user;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        if (UserSqlHelper.isWrongPassword(verifyingPassword, user.getT().password()))
            return UnionPair.fail(ServerUserHandler.WrongVerifyPassword);
        return user;
    }

    public static final @NotNull ServerHandler doChangePassword = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        if (user.isFailure())
            return user.getE();
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        try {
            UserSqlHelper.updateUser(user.getT().username(), newPassword, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };

    public static final @NotNull ServerHandler doLogoff = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        if (user.isFailure())
            return user.getE();
        try {
            UserSqlHelper.deleteUser(user.getT().username());
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };

    static @NotNull UnionPair<Pair.@NotNull ImmutablePair<UserTokenInfo, UserTokenInfo>, @NotNull MessageProto> checkChangerTokenAndUsername(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserTokenInfo, MessageProto> changer = ServerUserHandler.checkToken(buffer, permissions);
        if (changer.isFailure())
            return UnionPair.fail(changer.getE());
        final String username = ByteBufIOUtil.readUTF(buffer);
        if (username.equals(changer.getT().username()))
            return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), changer.getT()));
        final UserSqlInfo user;
        try {
            user = UserSqlHelper.selectUser(username);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null)
            return UnionPair.fail(ServerUserHandler.NoSuchUser);
        return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(changer.getT(), new UserTokenInfo(username, user.password(), user.permissions())));
    }

    public static final @NotNull ServerHandler doListUsers = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        if (user.isFailure())
            return user.getE();
        final List<UserInformation> list;
        try {
            list = UserSqlHelper.selectAllUsers();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(list.stream()
                .map(ServerUserHandler::getVisibleInfo).collect(Collectors.toList())));
    };

    public static final @NotNull ServerHandler doDeleteUser = buffer -> {
        final UnionPair<Pair.ImmutablePair<UserTokenInfo, UserTokenInfo>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        try {
            UserSqlHelper.deleteUser(userPair.getT().getSecond().username()); // TODO may optimize. (in select)
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };

    static @NotNull MessageProto doChangePermission(final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
        final UnionPair<Pair.ImmutablePair<UserTokenInfo, UserTokenInfo>, MessageProto> userPair = ServerUserHandler.checkChangerTokenAndUsername(buffer, Operation.Permission.UsersOperate);
        if (userPair.isFailure())
            return userPair.getE();
        final SortedSet<Operation.Permission> permissions = userPair.getT().getSecond().permissions();
        if (add)
            permissions.addAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer)));
        else
            permissions.removeAll(Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer)));
        try {
            UserSqlHelper.updateUser(userPair.getT().getSecond().username(), null, permissions);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(Operation.State.Success, null);
    }

    public static final @NotNull ServerHandler doAddPermission = buffer -> ServerUserHandler.doChangePermission(buffer, true);

    public static final @NotNull ServerHandler doReducePermission = buffer -> ServerUserHandler.doChangePermission(buffer, false);
}