package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UserTokenHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ServerUserHandler {
    private ServerUserHandler() {
        super();
    }

    public static final @NotNull MessageProto UserDataError = ServerHandler.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = ServerHandler.composeMessage(Operation.State.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = ServerHandler.composeMessage(Operation.State.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = ServerHandler.composeMessage(Operation.State.DataError, "Permissions");

    // TODO AnalyticalUserToken instead of UnionPair.
    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkToken(final @NotNull ByteBuf buffer, final Operation.@NotNull Permission... permissions) throws IOException, ServerException {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UserSqlInformation user;
        try {
            user = UserTokenHelper.decodeToken(token);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || (permissions.length > 0 && !user.group().permissions().containsAll(List.of(permissions))))
            return UnionPair.fail(ServerHandler.NoPermission);
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkTokenAndPassword(final @NotNull ByteBuf buffer, final Operation.@NotNull Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, permissions);
        if (user.isFailure())
            return user;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        if (!PasswordGuard.encryptPassword(verifyingPassword).equals(user.getT().password()))
            return UnionPair.fail(ServerHandler.DataError);
        return user;
    }

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.Register, ServerUserHandler.doRegister);
        ServerHandlerManager.register(Operation.Type.Login, ServerUserHandler.doLogin);
        ServerHandlerManager.register(Operation.Type.GetPermissions, ServerUserHandler.doGetPermissions);
        ServerHandlerManager.register(Operation.Type.ChangeUsername, ServerUserHandler.doChangeUsername);
        ServerHandlerManager.register(Operation.Type.ChangePassword, ServerUserHandler.doChangePassword);
        ServerHandlerManager.register(Operation.Type.Logoff, ServerUserHandler.doLogoff);
        ServerHandlerManager.register(Operation.Type.ListUsers, ServerUserHandler.doListUsers);
        ServerHandlerManager.register(Operation.Type.DeleteUser, ServerUserHandler.doDeleteUser);
        ServerHandlerManager.register(Operation.Type.ListGroups, ServerUserHandler.doListGroups);
        ServerHandlerManager.register(Operation.Type.AddGroup, ServerUserHandler.doAddGroup);
        ServerHandlerManager.register(Operation.Type.DeleteGroup, ServerUserHandler.doDeleteGroup);
        ServerHandlerManager.register(Operation.Type.ChangeGroup, ServerUserHandler.doChangeGroup);
        ServerHandlerManager.register(Operation.Type.AddPermission, (channel, buffer) -> ServerUserHandler.doChangePermission(channel, buffer, true));
        ServerHandlerManager.register(Operation.Type.RemovePermission, (channel, buffer) -> ServerUserHandler.doChangePermission(channel, buffer, false));
    }

    private static final @NotNull ServerHandler doRegister = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.Register, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        final boolean success;
        try {
            success = UserManager.insertUser(new UserSqlInformation.Inserter(username, password, UserGroupManager.getDefaultId()), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return success ? ServerHandler.Success : ServerHandler.DataError;
    };

    private static final @NotNull ServerHandler doLogin = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.Login, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        final UserSqlInformation user;
        try {
            user = UserManager.selectUserByName(username, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || !PasswordGuard.encryptPassword(password).equals(user.password()))
            return ServerHandler.DataError;
        final String token = UserTokenHelper.encodeToken(user.id(), user.modifyTime());
        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed token for user: '", username, "' token: ", token);
        return ServerHandler.composeMessage(Operation.State.Success, token);
    };

    private static final @NotNull ServerHandler doGetPermissions = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.GetPermissions, user, null);
        if (user.isFailure())
            return user.getE();
        return ServerHandler.successMessage(buf -> {
            UserGroupSqlInformation.dumpVisible(buf, user.getT().group());
            return buf;
        });
    };

    private static final @NotNull ServerHandler doChangeUsername = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer);
        final String newUsername = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.ChangeUsername, user, () -> ParametersMap.create()
                .add("newUsername", newUsername).optionallyAddSupplier(user.isSuccess(), "denied", () -> UserManager.ADMIN.equals(user.getT().username())));
        if (user.isFailure())
            return user.getE();
        if (UserManager.ADMIN.equals(user.getT().username()))
            return ServerUserHandler.UserDataError;
        try {
            UserManager.updateUser(new UserSqlInformation.Updater(user.getT().id(), newUsername,
                    user.getT().password(), user.getT().group().id(), user.getT().modifyTime()), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doChangePassword = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.ChangePassword, user, () -> ParametersMap.create()
                .add("newPassword", newPassword));
        if (user.isFailure())
            return user.getE();
        try {
            UserManager.updateUser(new UserSqlInformation.Updater(user.getT().id(), user.getT().username(),
                    PasswordGuard.encryptPassword(newPassword), user.getT().group().id(), null), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doLogoff = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.Logoff, user, () -> ParametersMap.create()
                .optionallyAddSupplier(user.isSuccess(), "denied", () -> UserManager.ADMIN.equals(user.getT().username())));
        if (user.isFailure())
            return user.getE();
        if (UserManager.ADMIN.equals(user.getT().username()))
            return ServerUserHandler.UserDataError;
        try {
            UserManager.deleteUser(user.getT().id(), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doListUsers = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.ListUsers, user, () -> ParametersMap.create()
                .add("limit", limit).add("page", page).add("orderDirection", orderDirection));
        if (user.isFailure())
            return user.getE();
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
            return ServerHandler.WrongParameters;
        final Pair.ImmutablePair<Long, List<UserSqlInformation>> list;
        try {
            list = UserManager.selectAllUsersInPage(limit, (long) page * limit, orderDirection, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
            for (final UserSqlInformation information: list.getSecond())
                UserSqlInformation.dumpVisible(buf, information);
            return buf;
        });
    };

    private static final @NotNull ServerHandler doDeleteUser = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String username = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.DeleteUser, changer, () -> ParametersMap.create()
                .add("username", username).add("denied", UserManager.ADMIN.equals(username)));
        if (changer.isFailure())
            return changer.getE();
        if (UserManager.ADMIN.equals(username))
            return ServerUserHandler.UserDataError;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.sqlInstance.getInstance().getConnection(null, connectionId)) {
            connection.setAutoCommit(false);
            final long id;
            if (username.equals(changer.getT().username()))
                id = changer.getT().id();
            else {
                final UserSqlInformation user = UserManager.selectUserByName(username, connectionId.get());
                if (user == null)
                    return ServerUserHandler.UserDataError;
                id = user.id();
            }
            UserManager.deleteUser(id, connectionId.get());
            connection.commit();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doListGroups = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.ListGroups, user, () -> ParametersMap.create()
                .add("limit", limit).add("page", page).add("orderDirection", orderDirection));
        if (user.isFailure())
            return user.getE();
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
            return ServerHandler.WrongParameters;
        final Pair.ImmutablePair<Long, List<UserGroupSqlInformation>> list;
        try {
            list = UserGroupManager.selectAllUserGroupsInPage(limit, (long) page * limit, orderDirection, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
            for (final UserGroupSqlInformation information: list.getSecond())
                UserGroupSqlInformation.dumpVisible(buf, information);
            return buf;
        });
    };

    private static final @NotNull ServerHandler doAddGroup = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.AddGroup, user, () -> ParametersMap.create()
                .add("groupName", groupName));
        if (user.isFailure())
            return user.getE();
        final boolean success;
        try {
            success = UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter(groupName, Operation.emptyPermissions()), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return success ? ServerHandler.Success : ServerHandler.DataError;
    };

    private static final @NotNull ServerHandler doDeleteGroup = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.DeleteGroup, user, () -> ParametersMap.create()
                .add("groupName", groupName).add("denied", UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName)));
        if (user.isFailure())
            return user.getE();
        if (UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName))
            return ServerUserHandler.GroupDataError;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.sqlInstance.getInstance().getConnection(null, connectionId)) {
            connection.setAutoCommit(false);
            final UserGroupSqlInformation information = UserGroupManager.selectGroupByName(groupName, connectionId.get());
            if (information == null)
                return ServerUserHandler.GroupDataError;
            final long count = UserManager.selectUserCountByGroup(information.id(), connectionId.get());
            if (count > 0)
                return ServerUserHandler.UsersDataError;
            UserGroupManager.deleteGroup(information.id(), connectionId.get());
            connection.commit();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doChangeGroup = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.ChangeGroup, changer, () -> ParametersMap.create()
                .add("username", username).add("groupName", groupName).add("denied", UserManager.ADMIN.equals(username)));
        if (changer.isFailure())
            return changer.getE();
        if (UserManager.ADMIN.equals(username))
            return ServerUserHandler.UserDataError;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.sqlInstance.getInstance().getConnection(null, connectionId)) {
            connection.setAutoCommit(false);
            final UserSqlInformation user = UserManager.selectUserByName(username, connectionId.get());
            if (user == null)
                return ServerUserHandler.UserDataError;
            final UserGroupSqlInformation group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
            if (group == null)
                return ServerUserHandler.GroupDataError;
            UserManager.updateUser(new UserSqlInformation.Updater(user.id(), user.username(),
                    user.password(), group.id(), null), connectionId.get());
            connection.commit();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    private static @NotNull MessageProto doChangePermission(final @NotNull Channel channel, final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        final EnumSet<Operation.Permission> permissions = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), add ? Operation.Type.AddPermission : Operation.Type.RemovePermission, changer, () -> ParametersMap.create()
                .add("groupName", groupName).add("permissions", permissions).add("denied", UserGroupManager.ADMIN.equals(groupName)));
        if (changer.isFailure())
            return changer.getE();
        if (UserGroupManager.ADMIN.equals(groupName))
            return ServerUserHandler.GroupDataError;
        if (permissions == null)
            return ServerUserHandler.PermissionsDataError;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.sqlInstance.getInstance().getConnection(null, connectionId)) {
            connection.setAutoCommit(false);
            final UserGroupSqlInformation group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
            if (group == null)
                return ServerUserHandler.GroupDataError;
            final EnumSet<Operation.Permission> p = group.permissions();
            if (add)
                p.addAll(permissions);
            else
                p.removeAll(permissions);
            UserGroupManager.updateGroup(group, connectionId.get());
            connection.commit();
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    }
}
