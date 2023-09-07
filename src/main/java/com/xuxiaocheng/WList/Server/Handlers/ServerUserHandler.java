package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Commons.Operation;
import org.jetbrains.annotations.NotNull;

public final class ServerUserHandler {
    private ServerUserHandler() {
        super();
    }

    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = MessageProto.composeMessage(Operation.State.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(Operation.State.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = MessageProto.composeMessage(Operation.State.DataError, "Permissions");

    public static void initialize() {
//        ServerHandlerManager.register(Operation.Type.ListUsers, ServerUserHandler.doListUsers);
//        ServerHandlerManager.register(Operation.Type.DeleteUser, ServerUserHandler.doDeleteUser);
//        ServerHandlerManager.register(Operation.Type.ListGroups, ServerUserHandler.doListGroups);
//        ServerHandlerManager.register(Operation.Type.AddGroup, ServerUserHandler.doAddGroup);
//        ServerHandlerManager.register(Operation.Type.DeleteGroup, ServerUserHandler.doDeleteGroup);
//        ServerHandlerManager.register(Operation.Type.ChangeGroup, ServerUserHandler.doChangeGroup);
//        ServerHandlerManager.register(Operation.Type.AddPermission, (channel, buffer) -> ServerUserHandler.doChangePermission(channel, buffer, true));
//        ServerHandlerManager.register(Operation.Type.RemovePermission, (channel, buffer) -> ServerUserHandler.doChangePermission(channel, buffer, false));
    }

//    private static final @NotNull ServerHandler doListUsers = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
//        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
//        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
//        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, Operation.Type.ListUsers, user, () -> ParametersMap.create()
//                .add("limit", limit).add("page", page).add("orderDirection", orderDirection));
//        if (user.isFailure())
//            return user.getE();
//        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
//            return MessageProto.WrongParameters;
//        final Pair.ImmutablePair<Long, List<UserSqlInformation>> list;
//        try {
//            list = UserManager.selectAllUsersInPage(limit, (long) page * limit, orderDirection, null);
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
//            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
//            for (final UserSqlInformation information: list.getSecond())
//                UserSqlInformation.dumpVisible(buf, information);
//            return buf;
//        });
//    };
//
//    private static final @NotNull ServerHandler doDeleteUser = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.DeleteUser, changer, () -> ParametersMap.create()
//                .add("username", username).add("denied", UserManager.ADMIN.equals(username)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserManager.ADMIN.equals(username))
//            return ServerUserHandler.UserDataError;
//        final long id;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            if (username.equals(changer.getT().username()))
//                id = changer.getT().id();
//            else {
//                final UserSqlInformation user = UserManager.selectUserByName(username, connectionId.get());
//                if (user == null)
//                    return ServerUserHandler.UserDataError;
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
//
//    private static final @NotNull ServerHandler doListGroups = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersList);
//        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
//        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
//        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, Operation.Type.ListGroups, user, () -> ParametersMap.create()
//                .add("limit", limit).add("page", page).add("orderDirection", orderDirection));
//        if (user.isFailure())
//            return user.getE();
//        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0 || orderDirection == null)
//            return MessageProto.WrongParameters;
//        final Pair.ImmutablePair<Long, List<UserGroupSqlInformation>> list;
//        try {
//            list = UserGroupManager.selectAllUserGroupsInPage(limit, (long) page * limit, orderDirection, null);
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
//            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
//            for (final UserGroupSqlInformation information: list.getSecond())
//                UserGroupSqlInformation.dumpVisible(buf, information);
//            return buf;
//        });
//    };
//
//    private static final @NotNull ServerHandler doAddGroup = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.AddGroup, changer, () -> ParametersMap.create()
//                .add("groupName", groupName));
//        if (changer.isFailure())
//            return changer.getE();
//        final Long id;
//        try {
//            id = UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter(groupName, Operation.emptyPermissions()), null);
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        if (id == null)
//            return MessageProto.DataError;
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Added group.", ServerHandler.buildUserGroupString(id.longValue(), groupName), ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()));
//        return MessageProto.Success;
//    };
//
//    private static final @NotNull ServerHandler doDeleteGroup = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.DeleteGroup, changer, () -> ParametersMap.create()
//                .add("groupName", groupName).add("denied", UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName))
//            return ServerUserHandler.GroupDataError;
//        final UserGroupSqlInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return ServerUserHandler.GroupDataError;
//            final long count = UserManager.selectUserCountByGroup(group.id(), connectionId.get());
//            if (count > 0)
//                return ServerUserHandler.UsersDataError;
//            UserGroupManager.deleteGroup(group.id(), connectionId.get());
//            connection.commit();
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted group.", ServerHandler.buildUserGroupString(group.id(), group.name()), ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()));
//        return MessageProto.Success;
//    };
//
//    private static final @NotNull ServerHandler doChangeGroup = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.ChangeGroup, changer, () -> ParametersMap.create()
//                .add("username", username).add("groupName", groupName).add("denied", UserManager.ADMIN.equals(username)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserManager.ADMIN.equals(username))
//            return ServerUserHandler.UserDataError;
//        final UserSqlInformation user;
//        final UserGroupSqlInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            user = UserManager.selectUserByName(username, connectionId.get());
//            if (user == null)
//                return ServerUserHandler.UserDataError;
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return ServerUserHandler.GroupDataError;
//            UserManager.updateUser(new UserSqlInformation.Updater(user.id(), user.username(),
//                    user.password(), group.id(), null), connectionId.get());
//            connection.commit();
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed group.", ServerHandler.buildUserString(user.id(), user.username()),
//                " from ", ServerHandler.buildUserGroupString(user.group().id(), user.group().name()), ", to", ServerHandler.buildUserGroupString(group.id(), group.name()),
//                ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()));
//        return MessageProto.Success;
//    };
//
//    private static @NotNull MessageProto doChangePermission(final @NotNull Channel channel, final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
//        final UnionPair<UserSqlInformation, MessageProto> changer = ServerUserHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        final EnumSet<Operation.Permission> permissions = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, add ? Operation.Type.AddPermission : Operation.Type.RemovePermission, changer, () -> ParametersMap.create()
//                .add("groupName", groupName).add("permissions", permissions).add("denied", UserGroupManager.ADMIN.equals(groupName)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserGroupManager.ADMIN.equals(groupName))
//            return ServerUserHandler.GroupDataError;
//        if (permissions == null)
//            return ServerUserHandler.PermissionsDataError;
//        final UserGroupSqlInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return ServerUserHandler.GroupDataError;
//            final EnumSet<Operation.Permission> p = group.permissions();
//            if (add)
//                p.addAll(permissions);
//            else
//                p.removeAll(permissions);
//            UserGroupManager.updateGroup(group, connectionId.get());
//            connection.commit();
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed permission.", ServerHandler.buildUserGroupString(group.id(), group.name()),
//                ", changer", ServerHandler.buildUserString(changer.getT().id(), changer.getT().username()), ParametersMap.create().add("permissions", group.permissions()));
//        return MessageProto.Success;
//    }
}
