package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.BroadcastManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OperateGroupsHandler {
    private OperateGroupsHandler() {
        super();
    }

    public static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(ResponseState.DataError, "Group");
    public static final @NotNull MessageProto OrdersDataError = MessageProto.composeMessage(ResponseState.DataError, "Orders");
    public static final @NotNull MessageProto ChooserDataError = MessageProto.composeMessage(ResponseState.DataError, "Chooser");

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
            final UserGroupInformation information = UserGroupManager.insertGroup(groupName, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Added groupName.", ServerHandler.userGroup(null, information), ',', ServerHandler.user("changer", changer.getT()));
            BroadcastManager.onUserGroupAdded(information);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangeGroupName = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String newGroupName = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeGroupName, changer, () -> ParametersMap.create()
                .add("groupId", groupId).add("newGroupName", newGroupName).optionallyAdd(changer.isSuccess(), "denied",
                        IdentifierNames.UserGroupName.contains(newGroupName) || UserGroupManager.getAdminId() == groupId || UserGroupManager.getDefaultId() == groupId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (IdentifierNames.UserGroupName.contains(newGroupName)|| UserGroupManager.getAdminId() == groupId || UserGroupManager.getDefaultId() == groupId)
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final LocalDateTime updateTime = UserGroupManager.updateGroupName(groupId, newGroupName, null);
            if (updateTime == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed groupName name.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("groupId", groupId).add("newGroupName", newGroupName).add("updateTime", updateTime));
            BroadcastManager.onUserGroupChangeName(groupId, newGroupName, updateTime);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doChangeGroupPermissions = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate, UserPermission.ServerOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        final EnumSet<UserPermission> newPermissions = UserPermission.parse(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.ChangeGroupName, changer, () -> ParametersMap.create()
                .add("groupId", groupId).add("newPermissions", newPermissions).optionallyAdd(changer.isSuccess() && newPermissions != null, "denied",
                        UserGroupManager.getAdminId() == groupId || Objects.requireNonNull(newPermissions).contains(UserPermission.Undefined)));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (newPermissions == null || newPermissions.contains(UserPermission.Undefined))
            message = MessageProto.WrongParameters;
        else if (UserGroupManager.getAdminId() == groupId)
            message = OperateGroupsHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final LocalDateTime updateTime = UserGroupManager.updateGroupPermission(groupId, newPermissions, null);
            if (updateTime == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed groupName permissions.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("groupId", groupId).add("newPermissions", newPermissions).add("updateTime", updateTime));
            BroadcastManager.onUserGroupChangePermissions(groupId, newPermissions, updateTime);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doGetGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeGroupName, user, () -> ParametersMap.create()
                .add("groupId", groupId));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final UserGroupInformation information = UserGroupManager.selectGroup(groupId, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateGroupsHandler.GroupDataError);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information::dumpVisible));
        };
    };

    private static final @NotNull ServerHandler doListGroups = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserGroupInformation::orderBy, VisibleUserGroupInformation.Order.values().length);
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
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.selectGroups(orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    private static final @NotNull ServerHandler doListGroupsInPermissions = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final EnumMap<UserPermission, Boolean> chooser = UserPermission.parseChooser(buffer);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserGroupInformation::orderBy, VisibleUserGroupInformation.Order.values().length);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListGroups, user, () -> ParametersMap.create()
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
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.selectGroupsByPermissions(chooser, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    private static final @NotNull ServerHandler doDeleteGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.GroupsOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.DeleteGroup, changer, () -> ParametersMap.create()
                .add("groupId", groupId).optionallyAdd(changer.isSuccess(), "denied", UserGroupManager.getAdminId() == groupId || UserGroupManager.getDefaultId() == groupId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (UserGroupManager.getAdminId() == groupId || UserGroupManager.getDefaultId() == groupId)
            message = OperateUsersHandler.GroupDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            if (!UserGroupManager.deleteGroup(groupId, null)) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.GroupDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted groupName.", ServerHandler.user("changer", changer.getT()), ParametersMap.create().add("groupId", groupId));
            BroadcastManager.onUserGroupDeleted(groupId);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doSearchGroupRegex = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final String regex = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserGroupInformation::orderBy, VisibleUserGroupInformation.Order.values().length);
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
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.searchGroupsByRegex(regex, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserGroupInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    private static final @NotNull ServerHandler doSearchGroupName = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final int len = ByteBufIOUtil.readVariableLenInt(buffer);
        final Set<String> names = new HashSet<>(len);
        for (int i = 0; i < len; ++i)
            names.add(ByteBufIOUtil.readUTF(buffer));
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.SearchGroupRegex, user, () -> ParametersMap.create()
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
            final Pair.ImmutablePair<Long, List<UserGroupInformation>> list = UserGroupManager.searchGroupsByNames(names, position, limit, null);
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
