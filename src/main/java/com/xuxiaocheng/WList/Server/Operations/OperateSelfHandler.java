package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.BroadcastManager;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.UserTokenHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class OperateSelfHandler {
    private OperateSelfHandler() {
        super();
    }

    public static final @NotNull MessageProto TokenDataError = new MessageProto(ResponseState.NoPermission, buf -> {
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

    static @NotNull UnionPair<UserInformation, MessageProto> checkToken(final @NotNull String token, final @NotNull UserPermission... permissions) throws SQLException {
        final UserInformation user = UserTokenHelper.decodeToken(token);
        if (user == null)
            return UnionPair.fail(OperateSelfHandler.TokenDataError);
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
            return UnionPair.fail(OperateSelfHandler.TokenDataError);
        return user;
    }

    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(ResponseState.DataError, "User");

    public static void initialize() {
        ServerHandlerManager.register(OperationType.Logon, OperateSelfHandler.doLogon);
        ServerHandlerManager.register(OperationType.Login, OperateSelfHandler.doLogin);
//        ServerHandlerManager.register(OperationType.Logout, OperateSelfHandler.doLogout); // Unsupported
        ServerHandlerManager.register(OperationType.Logoff, OperateSelfHandler.doLogoff);
        ServerHandlerManager.register(OperationType.ChangeUsername, OperateSelfHandler.doChangeUsername);
        ServerHandlerManager.register(OperationType.ChangePassword, OperateSelfHandler.doChangePassword);
        ServerHandlerManager.register(OperationType.GetSelfGroup, OperateSelfHandler.doGetSelfGroup);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#logon(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doLogon = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = PasswordGuard.encryptPassword(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.Logon, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
            final UserInformation information = UserManager.insertUser(username, password, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.UserDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged on.", ServerHandler.user(null, information));
            BroadcastManager.onUserLogon(information);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#login(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doLogin = (channel, buffer) -> {
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String password = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.Login, null, () -> ParametersMap.create()
                .add("username", username).add("password", password));
        return () -> {
            final UserInformation user = UserManager.selectUserByName(username, null);
            if (user == null || !PasswordGuard.encryptPassword(password).equals(user.encryptedPassword())) {
                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.UserDataError);
                return;
            }
            final String token = UserTokenHelper.encodeToken(user.id(), user.modifyTime());
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Logged in.", ServerHandler.user(null, user), ParametersMap.create().add("token", token));
            WListServer.ServerChannelHandler.write(channel, MessageProto.composeMessage(ResponseState.Success, token));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#logoff(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doLogoff = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        ServerHandler.logOperation(channel, OperationType.Logoff, user, () -> ParametersMap.create()
                .optionallyAddSupplier(user.isSuccess(), "denied", () -> IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username())));
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
            if (UserManager.deleteUser(user.getT().id(), null)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Logged off.", ServerHandler.user(null, user.getT()));
                BroadcastManager.onUserLogoff(user.getT().id());
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#changeUsername(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doChangeUsername = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token);
        final String newUsername = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeUsername, user, () -> ParametersMap.create()
                .add("newUsername", newUsername).optionallyAddSupplier(user.isSuccess(), "denied", () -> IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username())
                        || IdentifierNames.UserName.contains(newUsername)));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (IdentifierNames.UserName.Admin.getIdentifier().equals(user.getT().username()) || IdentifierNames.UserName.contains(newUsername))
            message = OperateSelfHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final LocalDateTime time = UserManager.updateUserName(user.getT().id(), newUsername, null);
            if (time == null) {
                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.UserDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed username.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("newUsername", newUsername));
            BroadcastManager.onUserChangeName(user.getT().id(), newUsername, time);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#changePassword(WListClientInterface, String, String, String)
     */
    private static final @NotNull ServerHandler doChangePassword = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final String verifyingPassword = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkTokenAndPassword(token, verifyingPassword);
        final String newPassword = PasswordGuard.encryptPassword(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.ChangePassword, user, () -> ParametersMap.create()
                .add("newPassword", newPassword));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final LocalDateTime time = UserManager.updateUserPassword(user.getT().id(), newPassword, null);
            if (time == null) {
                WListServer.ServerChannelHandler.write(channel, OperateSelfHandler.UserDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed password.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("newPassword", newPassword));
            BroadcastManager.onUserChangePassword(user.getT().id(), time);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper#getSelfGroup(WListClientInterface, String)
     */
    private static final @NotNull ServerHandler doGetSelfGroup = (channel, buffer) -> {
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
