package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

public final class OperateUsersHandler {
    private OperateUsersHandler() {
        super();
    }

    public static final @NotNull MessageProto PoliciesLengthError = MessageProto.composeMessage(Operation.State.DataError, "PoliciesLength");
    public static final @NotNull MessageProto PoliciesDataError = MessageProto.composeMessage(Operation.State.DataError, "PoliciesName");
    public static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(Operation.State.DataError, "User");
    public static final @NotNull MessageProto UsersDataError = MessageProto.composeMessage(Operation.State.DataError, "Users");
    public static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(Operation.State.DataError, "Group");
    public static final @NotNull MessageProto PermissionsDataError = MessageProto.composeMessage(Operation.State.DataError, "Permissions");

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.ListGroups, OperateUsersHandler.doListGroups);
//        ServerHandlerManager.register(Operation.Type.AddGroup, OperateUsersHandler.doAddGroup);
//        ServerHandlerManager.register(Operation.Type.DeleteGroup, OperateUsersHandler.doDeleteGroup);
//        ServerHandlerManager.register(Operation.Type.ChangeGroup, OperateUsersHandler.doChangeGroup);
//        ServerHandlerManager.register(Operation.Type.ListUsers, OperateUsersHandler.doListUsers);
//        ServerHandlerManager.register(Operation.Type.DeleteUser, OperateUsersHandler.doDeleteUser);
//        ServerHandlerManager.register(Operation.Type.AddPermission, (channel, buffer) -> OperateUsersHandler.doChangePermission(channel, buffer, true));
//        ServerHandlerManager.register(Operation.Type.RemovePermission, (channel, buffer) -> OperateUsersHandler.doChangePermission(channel, buffer, false));
    }

    private static final @NotNull ServerHandler doListGroups = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, Operation.Permission.UsersList);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection>, String> policies =
                Options.parseOrderPolicies(buffer, VisibleUserGroupInformation::orderBy, VisibleUserGroupInformation.Order.values().length);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, Operation.Type.ListGroups, user, () -> ParametersMap.create()
                .add("policies", policies).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (policies == null)
            message = OperateUsersHandler.PoliciesLengthError;
        else if (policies.isFailure())
            message = OperateUsersHandler.PoliciesDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> { // TODO
//        final Pair.ImmutablePair<Long, List<UserGroupInformation>> list;
//        try {
//            list = UserGroupManager.selectAllUserGroupsInPage(limit, (long) page * limit, orderDirection, null);
//        } catch (final SQLException exception) {
//            throw new ServerException(exception);
//        }
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
//            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
//            for (final UserGroupInformation information: list.getSecond())
//                UserGroupInformation.dumpVisible(buf, information);
//            return buf;
//        });
        };
    };

//    private static final @NotNull ServerHandler doAddGroup = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.AddGroup, changer, () -> ParametersMap.create()
//                .add("groupName", groupName));
//        if (changer.isFailure())
//            return changer.getE();
//        final Long id;
//        try {
//            id = UserGroupManager.insertGroup(new UserGroupInformation.Inserter(groupName, Operation.emptyPermissions()), null);
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
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.DeleteGroup, changer, () -> ParametersMap.create()
//                .add("groupName", groupName).add("denied", UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserGroupManager.ADMIN.equals(groupName) || UserGroupManager.DEFAULT.equals(groupName))
//            return OperateUsersHandler.GroupDataError;
//        final UserGroupInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return OperateUsersHandler.GroupDataError;
//            final long count = UserManager.selectUserCountByGroup(group.id(), connectionId.get());
//            if (count > 0)
//                return OperateUsersHandler.UsersDataError;
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
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.ChangeGroup, changer, () -> ParametersMap.create()
//                .add("username", username).add("groupName", groupName).add("denied", UserManager.ADMIN.equals(username)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserManager.ADMIN.equals(username))
//            return OperateUsersHandler.UserDataError;
//        final UserInformation user;
//        final UserGroupInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            user = UserManager.selectUserByName(username, connectionId.get());
//            if (user == null)
//                return OperateUsersHandler.UserDataError;
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return OperateUsersHandler.GroupDataError;
//            UserManager.updateUser(new UserInformation.Updater(user.id(), user.username(),
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
//    private static final @NotNull ServerHandler doListUsers = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersList);
//        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
//        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
//        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, Operation.Type.ListUsers, user, () -> ParametersMap.create()
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
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String username = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.DeleteUser, changer, () -> ParametersMap.create()
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
//
//    private static @NotNull MessageProto doChangePermission(final @NotNull Channel channel, final @NotNull ByteBuf buffer, final boolean add) throws IOException, ServerException {
//        final UnionPair<UserInformation, MessageProto> changer = OperateUsersHandler.checkToken(buffer, Operation.Permission.UsersOperate);
//        final String groupName = ByteBufIOUtil.readUTF(buffer);
//        final EnumSet<Operation.Permission> permissions = Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, add ? Operation.Type.AddPermission : Operation.Type.RemovePermission, changer, () -> ParametersMap.create()
//                .add("groupName", groupName).add("permissions", permissions).add("denied", UserGroupManager.ADMIN.equals(groupName)));
//        if (changer.isFailure())
//            return changer.getE();
//        if (UserGroupManager.ADMIN.equals(groupName))
//            return OperateUsersHandler.GroupDataError;
//        if (permissions == null)
//            return OperateUsersHandler.PermissionsDataError;
//        final UserGroupInformation group;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = UserManager.getConnection(null, connectionId)) {
//            group = UserGroupManager.selectGroupByName(groupName, connectionId.get());
//            if (group == null)
//                return OperateUsersHandler.GroupDataError;
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
