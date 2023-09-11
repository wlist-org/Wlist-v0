package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Server.MessageProto;
import org.jetbrains.annotations.NotNull;

public final class OperateUsersHandler {
    private OperateUsersHandler() {
        super();
    }

    public static final @NotNull MessageProto PoliciesLengthError = MessageProto.composeMessage(ResponseState.DataError, "PoliciesLength");
    public static final @NotNull MessageProto PoliciesDataError = MessageProto.composeMessage(ResponseState.DataError, "PoliciesName");
    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(ResponseState.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = MessageProto.composeMessage(ResponseState.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(ResponseState.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = MessageProto.composeMessage(ResponseState.DataError, "Permissions");

    public static void initialize() {
//        ServerHandlerManager.register(OperationType.ChangeGroup, OperateUsersHandler.doChangeGroup);
//        ServerHandlerManager.register(OperationType.ListUsers, OperateUsersHandler.doListUsers);
//        ServerHandlerManager.register(OperationType.DeleteUser, OperateUsersHandler.doDeleteUser);
    }
//
//    private static final @NotNull ServerHandler doChangeGroup = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, UserPermission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, OperationType.ChangeGroup, changer, () -> ParametersMap.create()
//                .add("username", username).add("groupName", groupName).add("denied", UserManager.ADMIN.equals(username)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserManager.ADMIN.equals(username))
//            return OperateUsersHandler.UserDataError;
//        final UserInformation user;
//        final UserGroupInformation groupName;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            user = UserManager.selectUserByName(username, connectionId.get());
//            if (user == null)
//                return OperateUsersHandler.UserDataError;
//            groupName = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (groupName == null)
//                return OperateUsersHandler.GroupDataError;
//            UserManager.updateUser(new UserInformation.Updater(user.id(), user.username(),
//                    user.encryptedPassword(), groupName.id(), null), connectionId.get());
//            connection.commit();
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed groupName.", ServerHandler.buildUserString(user.id(), user.username()),
//                " from ", ServerHandler.buildUserGroupString(user.groupName().id(), user.groupName().name()), ", to", ServerHandler.buildUserGroupString(groupName.id(), groupName.name()),
//                ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()));
//        return MessageProto.Success;
//    };
//
//    private static final @NotNull ServerHandler doListUsers = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.UsersList);
//        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
//        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
//        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.ListUsers, user, () -> ParametersMap.create()
//                .add("limit", limit).add("page", page).add("orderDirection", orderDirection));
//        if (user.isFailure())
//            return user.getE();
//        if (limit < 1 || limit > ServerConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
//            return MessageProto.WrongParameters;
//        final Pair.ImmutablePair<Long, List<UserInformation>> list;
//        try {
//            list = UserManager.selectAllUsersInPage(limit, (long) page * limit, orderDirection, null);
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
//            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
//            for (final UserInformation information: list.getSecond())
//                UserInformation.dumpVisible(buf, information);
//            return buf;
//        });
//    };
//
//    private static final @NotNull ServerHandler doDeleteUser = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, UserPermission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, OperationType.DeleteUser, changer, () -> ParametersMap.create()
//                .add("username", username).add("denied", UserManager.ADMIN.equals(username)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserManager.ADMIN.equals(username))
//            return OperateUsersHandler.UserDataError;
//        final long id;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            if (username.equals(changer.getT().username()))
//                id = changer.getT().id();
//            else {
//                final UserInformation user = UserManager.selectUserByName(username, connectionId.get());
//                if (user == null)
//                    return OperateUsersHandler.UserDataError;
//                id = user.id();
//            }
//            UserManager.deleteUser(id, connectionId.get());
//            connection.commit();
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted user.", ServerHandler.buildUserString(id, username), ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()));
//        return MessageProto.Success;
//    };
}
