package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OperateGroupsHandler {
    private OperateGroupsHandler() {
        super();
    }

    private static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(ResponseState.DataError, "Group");
    private static final @NotNull MessageProto OrdersDataError = MessageProto.composeMessage(ResponseState.DataError, "Orders");
    private static final @NotNull MessageProto ChooserDataError = MessageProto.composeMessage(ResponseState.DataError, "Chooser");
    private static final @NotNull MessageProto UsersDataError = MessageProto.composeMessage(ResponseState.DataError, "Users");

    public static void initialize() {
        ServerHandlerManager.register(OperationType.AddGroup, OperateGroupsHandler.doAddGroup);
        ServerHandlerManager.register(OperationType.ChangeGroupName, OperateGroupsHandler.doChangeGroupName);
        ServerHandlerManager.register(OperationType.ChangeGroupPermissions, OperateGroupsHandler.doChangeGroupPermissions);
        ServerHandlerManager.register(OperationType.GetGroup, OperateGroupsHandler.doGetGroup);
        ServerHandlerManager.register(OperationType.ListGroups, OperateGroupsHandler.doListGroups);
        ServerHandlerManager.register(OperationType.ListGroupsInPermissions, OperateGroupsHandler.doListGroupsInPermissions);
        ServerHandlerManager.register(OperationType.DeleteGroup, OperateGroupsHandler.doDeleteGroup);
        ServerHandlerManager.register(OperationType.SearchGroupRegex, OperateGroupsHandler.doSearchGroupRegex);
        ServerHandlerManager.register(OperationType.SearchGroupName, OperateGroupsHandler.doSearchGroupName);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#addGroup(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doAddGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.UsersOperate);
        final String groupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.AddGroup, changer, () -> ParametersMap.create()
                .add("groupName", groupName).optionallyAdd(changer.isSuccess(), "denied", IdentifierNames.UserGroupName.contains(groupName)));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (IdentifierNames.UserGroupName.contains(groupName))
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final UserGroupInformation information = UserGroupManager.getInstance().insertGroup(groupName, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Added group.", ServerHandler.userGroup(null, information), ',', ServerHandler.user("changer", changer.getT()));
            BroadcastManager.onUserGroupAdded(information);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#changeGroupName(WListClientInterface, String, long, String)
     */
    private static final @NotNull ServerHandler doChangeGroupName = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String newGroupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeGroupName, changer, () -> ParametersMap.create()
                .add("groupId", groupId).add("newGroupName", newGroupName).optionallyAdd(changer.isSuccess(), "denied",
                        IdentifierNames.UserGroupName.contains(newGroupName) || UserGroupManager.getInstance().getAdminId() == groupId || UserGroupManager.getInstance().getDefaultId() == groupId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (IdentifierNames.UserGroupName.contains(newGroupName)|| UserGroupManager.getInstance().getAdminId() == groupId || UserGroupManager.getInstance().getDefaultId() == groupId)
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final ZonedDateTime updateTime = UserGroupManager.getInstance().updateGroupName(groupId, newGroupName, null);
            if (updateTime == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed group name.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("groupId", groupId).add("newGroupName", newGroupName).add("updateTime", updateTime));
            BroadcastManager.onUserGroupChangeName(groupId, newGroupName, updateTime);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#changeGroupPermissions(WListClientInterface, String, long, Set)
     */
    private static final @NotNull ServerHandler doChangeGroupPermissions = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate, UserPermission.ServerOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        final EnumSet<UserPermission> newPermissions = UserPermission.parse(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.ChangeGroupPermissions, changer, () -> ParametersMap.create()
                .add("groupId", groupId).add("newPermissions", newPermissions).optionallyAddSupplier(changer.isSuccess() && newPermissions != null, "denied", () ->
                        UserGroupManager.getInstance().getAdminId() == groupId || Objects.requireNonNull(newPermissions).contains(UserPermission.Undefined)));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (newPermissions == null || newPermissions.contains(UserPermission.Undefined))
            message = MessageProto.WrongParameters;
        else if (UserGroupManager.getInstance().getAdminId() == groupId)
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final ZonedDateTime updateTime = UserGroupManager.getInstance().updateGroupPermission(groupId, newPermissions, null);
            if (updateTime == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed group permissions.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("groupId", groupId).add("newPermissions", newPermissions).add("updateTime", updateTime));
            BroadcastManager.onUserGroupChangePermissions(groupId, newPermissions, updateTime);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#getGroup(WListClientInterface, String, long)
     */
    private static final @NotNull ServerHandler doGetGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.GetGroup, user, () -> ParametersMap.create()
                .add("groupId", groupId));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final UserGroupInformation information = UserGroupManager.getInstance().selectGroup(groupId, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information::dumpVisible));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#listGroups(WListClientInterface, String, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doListGroups = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, OrderDirection>, String> orders =
                OrderPolicies.parse(buffer, VisibleUserGroupInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListGroups, user, () -> ParametersMap.create()
                .add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (orders == null || orders.isFailure())
            message = OperateGroupsHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.getInstance().selectGroups(orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#listGroupsInPermissions(WListClientInterface, String, Map, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doListGroupsInPermissions = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final EnumMap<UserPermission, Boolean> chooser = UserPermission.parseChooser(buffer);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, OrderDirection>, String> orders =
                OrderPolicies.parse(buffer, VisibleUserGroupInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListGroupsInPermissions, user, () -> ParametersMap.create()
                .add("chooser", chooser).add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (chooser == null || chooser.isEmpty())
            message = OperateGroupsHandler.ChooserDataError;
        else if (orders == null || orders.isFailure())
            message = OperateGroupsHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.getInstance().selectGroupsByPermissions(chooser, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#deleteGroup(WListClientInterface, String, long)
     */
    private static final @NotNull ServerHandler doDeleteGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.DeleteGroup, changer, () -> ParametersMap.create()
                .add("groupId", groupId).optionallyAdd(changer.isSuccess(), "denied", UserGroupManager.getInstance().getAdminId() == groupId || UserGroupManager.getInstance().getDefaultId() == groupId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (UserGroupManager.getInstance().getAdminId() == groupId || UserGroupManager.getInstance().getDefaultId() == groupId)
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final long count = UserManager.getInstance().selectUsersByGroups(Set.of(groupId), false, VisibleUserInformation.emptyOrder(), 0, 0, null).getFirst().longValue();
            if (count > 0) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.UsersDataError);
                return;
            }
            if (!UserGroupManager.getInstance().deleteGroup(groupId, null)) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted group.", ServerHandler.user("changer", changer.getT()), ParametersMap.create().add("groupId", groupId));
            BroadcastManager.onUserGroupDeleted(groupId);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#searchGroupsRegex(WListClientInterface, String, String, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doSearchGroupRegex = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final String regex = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, OrderDirection>, String> orders =
                OrderPolicies.parse(buffer, VisibleUserGroupInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.SearchGroupRegex, user, () -> ParametersMap.create()
                .add("regex", regex).add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (orders == null || orders.isFailure())
            message = OperateGroupsHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.getInstance().searchGroupsByRegex(regex, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper#searchGroupsName(WListClientInterface, String, Set, long, int)
     */
    private static final @NotNull ServerHandler doSearchGroupName = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final Set<String> names = new HashSet<>(length);
        for (int i = 0; i < length; ++i)
            names.add(ByteBufIOUtil.readUTF(buffer));
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.SearchGroupName, user, () -> ParametersMap.create()
                .add("names", names).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (names.isEmpty())
            message = OperateGroupsHandler.ChooserDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.getInstance().searchGroupsByNames(names, position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };
}
