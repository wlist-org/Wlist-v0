package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.UserTokenHelper;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ServerSelfHandler {
    private ServerSelfHandler() {
        super();
    }

    public static final @NotNull MessageProto NoSuchUser = new MessageProto(Operation.State.NoPermission, buf -> {
        ByteBufIOUtil.writeVariableLenInt(buf, 1);
        ByteBufIOUtil.writeUTF(buf, Operation.Permission.Undefined.name());
        return buf;
    });
    public static @NotNull MessageProto NoPermission(final @NotNull Collection<Operation.@NotNull Permission> permissions) {
        assert !permissions.isEmpty();
        return new MessageProto(Operation.State.NoPermission, buf -> {
            ByteBufIOUtil.writeVariableLenInt(buf, permissions.size());
            for (final Operation.Permission permission: permissions)
                ByteBufIOUtil.writeUTF(buf, permission.name());
            return buf;
        });
    }
    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = MessageProto.composeMessage(Operation.State.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(Operation.State.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = MessageProto.composeMessage(Operation.State.DataError, "Permissions");

    static @NotNull UnionPair<UserInformation, MessageProto> checkToken(final @NotNull String token, final Operation.@NotNull Permission... permissions) throws SQLException {
        final UserInformation user = UserTokenHelper.decodeToken(token);
        if (user == null)
            return UnionPair.fail(ServerSelfHandler.NoSuchUser);
        final Set<Operation.Permission> required = EnumSet.noneOf(Operation.Permission.class);
        required.addAll(List.of(permissions));
        required.removeAll(user.group().permissions());
        if (!required.isEmpty())
            return UnionPair.fail(ServerSelfHandler.NoPermission(required));
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<UserInformation, MessageProto> checkTokenAndPassword(final @NotNull String token, final @NotNull String verifyingPassword, final Operation.@NotNull Permission... permissions) throws SQLException {
        final UnionPair<UserInformation, MessageProto> user = ServerSelfHandler.checkToken(token, permissions);
        if (user.isFailure())
            return user;
        if (!PasswordGuard.encryptPassword(verifyingPassword).equals(user.getT().password()))
            return UnionPair.fail(MessageProto.DataError);
        return user;
    }

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.Logon, ServerSelfHandler.doLogon);
        ServerHandlerManager.register(Operation.Type.Login, ServerSelfHandler.doLogin);
//        ServerHandlerManager.register(Operation.Type.Logout, ServerSelfHandler.doLogout); // Unsupported
        ServerHandlerManager.register(Operation.Type.Logoff, ServerSelfHandler.doLogoff);
        ServerHandlerManager.register(Operation.Type.ChangeUsername, ServerSelfHandler.doChangeUsername);
        ServerHandlerManager.register(Operation.Type.ChangePassword, ServerSelfHandler.doChangePassword);
        ServerHandlerManager.register(Operation.Type.GetGroup, ServerSelfHandler.doGetGroup);
    }

    private static final @NotNull ServerHandler doLogon = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.Logon, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
            final Long id = UserManager.insertUser(new UserInformation.Inserter(username, password, UserGroupManager.getDefaultId()), null);
            if (id == null) {
                WListServer.ServerChannelHandler.write(channel, MessageProto.DataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged on." + ServerHandler.user(null, id.longValue(), username));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doLogin = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.Login, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
            final UserInformation user = UserManager.selectUserByName(username, null);
            if (user == null || !PasswordGuard.encryptPassword(password).equals(user.password())) {
                WListServer.ServerChannelHandler.write(channel, MessageProto.DataError);
                return;
            }
            final String token = UserTokenHelper.encodeToken(user.id(), user.modifyTime());
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Logged in.", ServerHandler.user(null, user), ParametersMap.create().add("token", token));
            WListServer.ServerChannelHandler.write(channel, MessageProto.composeMessage(Operation.State.Success, token));
        };
    };

    private static final @NotNull ServerHandler doLogoff = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = ServerSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        ServerHandler.logOperation(channel, Operation.Type.Logoff, user, () -> ParametersMap.create()
                .optionallyAdd(user.isSuccess(), "denied", UserManager.ADMIN.equals(user.getT().username())));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (UserManager.ADMIN.equals(user.getT().username()))
            message = ServerSelfHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            UserManager.deleteUser(user.getT().id(), null);
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged off.", ServerHandler.user(null, user.getT()));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangeUsername = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = ServerSelfHandler.checkToken(token);
        final String newUsername = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.ChangeUsername, user, () -> ParametersMap.create()
                .add("newUsername", newUsername).optionallyAdd(user.isSuccess(), "denied", UserManager.ADMIN.equals(user.getT().username())));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (UserManager.ADMIN.equals(user.getT().username()))
            message = ServerSelfHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            UserManager.updateUser(new UserInformation.Updater(user.getT().id(), newUsername,
                    user.getT().password(), user.getT().group().id(), user.getT().modifyTime()), null);
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed username.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("new", newUsername));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangePassword = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = ServerSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.ChangePassword, user, () -> ParametersMap.create()
                .add("newPassword", newPassword));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            UserManager.updateUser(new UserInformation.Updater(user.getT().id(), user.getT().username(),
                    PasswordGuard.encryptPassword(newPassword), user.getT().group().id(), null), null);
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed password.", ServerHandler.user(null, user.getT()));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doGetGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = ServerSelfHandler.checkToken(token);
        ServerHandler.logOperation(channel, Operation.Type.GetGroup, user, null);
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
            UserGroupInformation.dumpVisible(buf, user.getT().group());
            return buf;
        }));
    };
}
