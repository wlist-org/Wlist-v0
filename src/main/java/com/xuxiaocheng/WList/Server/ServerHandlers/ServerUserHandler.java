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
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UserTokenHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

public final class ServerUserHandler {
    private ServerUserHandler() {
        super();
    }

    public static final @NotNull MessageProto UserDataError = ServerHandler.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = ServerHandler.composeMessage(Operation.State.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = ServerHandler.composeMessage(Operation.State.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = ServerHandler.composeMessage(Operation.State.DataError, "Permissions");

    public static final @NotNull ServerHandler doRegister = buffer -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        final boolean success;
        try {
            success = UserManager.insertUser(new UserSqlInformation.Inserter(username, password, UserManager.getDefaultGroupId()), null);
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
            user = UserManager.selectUserByName(username, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        if (user == null || !PasswordGuard.encryptPassword(password).equals(user.password()))
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
            return UnionPair.fail(ServerHandler.NoPermission);
        return UnionPair.ok(user);
    }

    public static final @NotNull ServerHandler doGetPermissions = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer);
        if (user.isFailure())
            return user.getE();
        return ServerHandler.successMessage(buf -> {
            UserGroupSqlInformation.dumpVisible(buf, user.getT().group());
            return buf;
        });
    };

    static @NotNull UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> checkTokenAndPassword(final @NotNull ByteBuf buffer, final @NotNull Operation.Permission... permissions) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, permissions);
        if (user.isFailure())
            return user;
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        if (!PasswordGuard.encryptPassword(verifyingPassword).equals(user.getT().password()))
            return UnionPair.fail(ServerHandler.DataError);
        return user;
    }

    public static final @NotNull ServerHandler doChangeUsername = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer);
        final String newUsername = ByteBufIOUtil.readUTF(buffer);
        if (user.isFailure())
            return user.getE();
        if ("admin".equals(user.getT().username()))
            return ServerUserHandler.UserDataError;
        try {
            UserManager.updateUser(new UserSqlInformation.Updater(user.getT().id(), newUsername,
                    user.getT().password(), user.getT().group().id(), user.getT().modifyTime()), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doChangePassword = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
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

    public static final @NotNull ServerHandler doLogoff = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkTokenAndPassword(buffer);
        if (user.isFailure())
            return user.getE();
        if ("admin".equals(user.getT().username()))
            return ServerUserHandler.UserDataError;
        try {
            UserManager.deleteUser(user.getT().id(), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

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

    public static final @NotNull ServerHandler doDeleteUser = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String username = ByteBufIOUtil.readUTF(buffer);
        if (changer.isFailure())
            return changer.getE();
        if ("admin".equals(username))
            return ServerUserHandler.UserDataError;
        final long id;
        if (username.equals(changer.getT().username()))
            id = changer.getT().id();
        else {
            final UserSqlInformation user;
            try {
                user = UserManager.selectUserByName(username, null);
            } catch (final SQLException exception1) {
                throw new ServerException(exception1);
            }
            if (user == null)
                return ServerUserHandler.UserDataError;
            id = user.id();
        }
        try {
            UserManager.deleteUser(id, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doListGroups = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
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

    public static final @NotNull ServerHandler doAddGroup = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        if (user.isFailure())
            return user.getE();
        final boolean success;
        try {
            success = UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter(groupName, Operation.emptyPermissions()), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.composeMessage(success ? Operation.State.Success : Operation.State.DataError, null);
    };

    public static final @NotNull ServerHandler doDeleteGroup = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        if (user.isFailure())
            return user.getE();
        if ("admin".equals(groupName) || "default".equals(groupName))
            return ServerUserHandler.GroupDataError;
        try {
            final UserGroupSqlInformation information = UserGroupManager.selectGroupByName(groupName, null);
            if (information == null)
                return ServerUserHandler.GroupDataError;
            final long count = UserManager.selectUserCountByGroup(information.id(), null);
            if (count > 0)
                return ServerUserHandler.UsersDataError;
            UserGroupManager.deleteGroup(information.id(), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doChangeGroup = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        if (changer.isFailure())
            return changer.getE();
        if ("admin".equals(username))
            return ServerUserHandler.UserDataError;
        try {
            final UserSqlInformation user = UserManager.selectUserByName(username, null);
            if (user == null)
                return ServerUserHandler.UserDataError;
            final UserGroupSqlInformation group = UserGroupManager.selectGroupByName(groupName, null);
            if (group == null)
                return ServerUserHandler.GroupDataError;
            UserManager.updateUser(new UserSqlInformation.Updater(user.id(), user.username(),
                    user.password(), group.id(), null), null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static @NotNull MessageProto doChangePermission(final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
        final UnionPair<UserSqlInformation, MessageProto> userPair = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        final EnumSet<Operation.Permission> modified = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
        if (userPair.isFailure())
            return userPair.getE();
        if ("admin".equals(groupName))
            return ServerUserHandler.GroupDataError;
        if (modified == null)
            return ServerUserHandler.PermissionsDataError;
        try {
            final UserGroupSqlInformation group = UserGroupManager.selectGroupByName(groupName, null);
            if (group == null)
                return ServerUserHandler.GroupDataError;
            final EnumSet<Operation.Permission> permissions = group.permissions();
            if (add)
                permissions.addAll(modified);
            else
                permissions.removeAll(modified);
            UserGroupManager.updateGroup(group, null);
        } catch (final SQLException exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    }
}
