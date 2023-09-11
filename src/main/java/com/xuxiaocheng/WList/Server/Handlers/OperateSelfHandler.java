package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.UserTokenHelper;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class OperateSelfHandler {
    private OperateSelfHandler() {
        super();
    }

    public static final @NotNull MessageProto NoSuchUser = new MessageProto(ResponseState.NoPermission, buf -> {
        ByteBufIOUtil.writeVariableLenInt(buf, 1);
        ByteBufIOUtil.writeUTF(buf, UserPermission.Undefined.name());
        return buf;
    });
    public static @NotNull MessageProto NoPermission(final @NotNull Collection<@NotNull UserPermission> permissions) {
        assert !permissions.isEmpty();
        return new MessageProto(ResponseState.NoPermission, buf -> {
            ByteBufIOUtil.writeVariableLenInt(buf, permissions.size());
            for (final UserPermission permission: permissions)
                ByteBufIOUtil.writeUTF(buf, permission.name());
            return buf;
        });
    }
    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(ResponseState.DataError, "User");

    static @NotNull UnionPair<UserInformation, MessageProto> checkToken(final @NotNull String token, final @NotNull UserPermission... permissions) throws SQLException {
        final UserInformation user = UserTokenHelper.decodeToken(token);
        if (user == null)
            return UnionPair.fail(OperateSelfHandler.NoSuchUser);
        if (IdentifierNames.UserGroupName.Admin.getIdentifier().equals(user.group().name()))// || IdentifierNames.UserName.Admin.getIdentifier().equals(user.username()))
            return UnionPair.ok(user);
        final Set<UserPermission> required = EnumSet.noneOf(UserPermission.class);
        required.addAll(List.of(permissions));
        required.removeAll(user.group().permissions());
        if (!required.isEmpty())
            return UnionPair.fail(OperateSelfHandler.NoPermission(required));
        return UnionPair.ok(user);
    }

    static @NotNull UnionPair<UserInformation, MessageProto> checkTokenAndPassword(final @NotNull String token, final @NotNull String verifyingPassword, final @NotNull UserPermission... permissions) throws SQLException {
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, permissions);
        if (user.isFailure())
            return user;
        if (!PasswordGuard.encryptPassword(verifyingPassword).equals(user.getT().encryptedPassword()))
            return UnionPair.fail(OperateSelfHandler.NoSuchUser);
        return user;
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.Logon, OperateSelfHandler.doLogon);
        ServerHandlerManager.register(OperationType.Login, OperateSelfHandler.doLogin);
//        ServerHandlerManager.register(OperationType.Logout, OperateSelfHandler.doLogout); // Unsupported
        ServerHandlerManager.register(OperationType.Logoff, OperateSelfHandler.doLogoff);
        ServerHandlerManager.register(OperationType.ChangeUsername, OperateSelfHandler.doChangeUsername);
        ServerHandlerManager.register(OperationType.ChangePassword, OperateSelfHandler.doChangePassword);
        ServerHandlerManager.register(OperationType.GetSelfGroup, OperateSelfHandler.doGetGroup);
    }

    private static final @NotNull ServerHandler doLogon = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.Logon, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
//            final Long id = UserManager.insertUser(new UserInformation.Inserter(username, password, UserGroupManager.getDefaultId()), null);
//            if (id == null) {
//                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.UserDataError);
//                return;
//            }
//            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged on.", ServerHandler.user(null, id.longValue(), username));
//            BroadcastManager.onUserLogon(); // TODO
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doLogin = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.Login, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
//            final UserInformation user = UserManager.selectUserByName(username, null);
//            if (user == null || !PasswordGuard.encryptPassword(password).equals(user.encryptedPassword())) {
//                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.NoSuchUser);
//                return;
//            }
//            final String token = UserTokenHelper.encodeToken(user.id(), user.createTime());
//            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Logged in.", ServerHandler.user(null, user), ParametersMap.create().add("token", token));
//            WListServer.ServerChannelHandler.write(channel, MessageProto.composeMessage(ResponseState.Success, token));
        };
    };

    private static final @NotNull ServerHandler doLogoff = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        ServerHandler.logOperation(channel, OperationType.Logoff, user, () -> ParametersMap.create()
                .optionallyAdd(user.isSuccess(), "denied", IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username())));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username()))
            message = OperateSelfHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
//            UserManager.deleteUser(user.getT().id(), null);
//            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged off.", ServerHandler.user(null, user.getT()));
//            BroadcastManager.onUserLogoff(user.getT().id());
//            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangeUsername = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token);
        final String newUsername = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeUsername, user, () -> ParametersMap.create()
                .add("newUsername", newUsername).optionallyAdd(user.isSuccess(), "denied", IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username())));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username()))
            message = OperateSelfHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
//            UserManager.updateUser(new UserInformation.Updater(user.getT().id(), newUsername,
//                    user.getT().encryptedPassword(), user.getT().group().id(), user.getT().createTime()), null);
//            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed username.", ServerHandler.user(null, user.getT()),
//                    ParametersMap.create().add("new", newUsername));
//            BroadcastManager.onUserChangeName(user.getT(), newUsername); // TODO
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangePassword = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        final String newPassword = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangePassword, user, () -> ParametersMap.create()
                .add("newPassword", newPassword));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
//            UserManager.updateUser(new UserInformation.Updater(user.getT().id(), user.getT().username(),
//                    PasswordGuard.encryptPassword(newPassword), user.getT().group().id(), null), null);
//            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed password.", ServerHandler.user(null, user.getT()));
//            BroadcastManager.onUserChangePassword(user.getT(), time); // TODO
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doGetGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token);
        ServerHandler.logOperation(channel, OperationType.GetSelfGroup, user, null);
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        final UserGroupInformation information = user.getT().group();
        return () -> WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information::dumpVisible));
    };
}
