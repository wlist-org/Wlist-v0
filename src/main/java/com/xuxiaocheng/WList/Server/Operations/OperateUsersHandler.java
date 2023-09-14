package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public final class OperateUsersHandler {
    private OperateUsersHandler() {
        super();
    }

    private static final @NotNull MessageProto UserDataError = MessageProto.composeMessage(ResponseState.DataError, "User");
    private static final @NotNull MessageProto GroupDataError = MessageProto.composeMessage(ResponseState.DataError, "Group");
    private static final @NotNull MessageProto OrdersDataError = MessageProto.composeMessage(ResponseState.DataError, "Orders");
    private static final @NotNull MessageProto ChooserDataError = MessageProto.composeMessage(ResponseState.DataError, "Chooser");

    public static void initialize() {
        ServerHandlerManager.register(OperationType.ChangeUserGroup, OperateUsersHandler.doChangeUserGroup);
        ServerHandlerManager.register(OperationType.GetUser, OperateUsersHandler.doGetUser);
        ServerHandlerManager.register(OperationType.ListUsers, OperateUsersHandler.doListUsers);
        ServerHandlerManager.register(OperationType.ListUsersInGroups, OperateUsersHandler.doListUsersInGroups);
        ServerHandlerManager.register(OperationType.DeleteUser, OperateUsersHandler.doDeleteUser);
        ServerHandlerManager.register(OperationType.DeleteUsersInGroup, OperateUsersHandler.doDeleteUsersInGroup);
        ServerHandlerManager.register(OperationType.SearchUserRegex, OperateUsersHandler.doSearchUserRegex);
        ServerHandlerManager.register(OperationType.SearchUserName, OperateUsersHandler.doSearchUserName);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#changeUserGroup(WListClientInterface, String, long, long)
     */
    private static final @NotNull ServerHandler doChangeUserGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.UsersOperate, UserPermission.ServerOperate);
        final long userId = ByteBufIOUtil.readVariableLenLong(buffer);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.ChangeUserGroup, changer, () -> ParametersMap.create()
                .add("userId", userId).add("groupId", groupId).optionallyAdd(changer.isSuccess(), "denied", UserManager.getAdminId() == userId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (UserManager.getAdminId() == userId)
            message = OperateUsersHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final UserGroupInformation group = UserGroupManager.selectGroup(groupId, null);
            if (group == null) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.GroupDataError);
                return;
            }
            final LocalDateTime time = UserManager.updateUserGroup(userId, groupId, null);
            if (time == null) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.UserDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Changed group.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("userId", userId).add("group", group));
            BroadcastManager.onUserChangeGroup(userId, groupId, group.name(), time);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#getUser(WListClientInterface, String, long)
     */
    private static final @NotNull ServerHandler doGetUser = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final long userId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.GetUser, user, () -> ParametersMap.create()
                .add("userId", userId));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final UserInformation information = UserManager.selectUser(userId, null);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.UserDataError);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information::dumpVisible));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#listUsers(WListClientInterface, String, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doListUsers = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final UnionPair<LinkedHashMap<VisibleUserInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListUsers, user, () -> ParametersMap.create()
                .add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (orders == null || orders.isFailure())
            message = OperateUsersHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserInformation>> list = UserManager.selectUsers(orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#listUsersInGroups(WListClientInterface, String, Set, boolean, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doListUsersInGroups = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final Set<Long> chooser = new HashSet<>(length);
        for (int i = 0; i < length; ++i)
            chooser.add(ByteBufIOUtil.readVariableLenLong(buffer));
        final boolean blacklist = ByteBufIOUtil.readBoolean(buffer);
        final UnionPair<LinkedHashMap<VisibleUserInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListUsersInGroups, user, () -> ParametersMap.create()
                .add("chooser", chooser).add("blacklist", blacklist).add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (orders == null || orders.isFailure())
            message = OperateUsersHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserInformation>> list = UserManager.selectUsersByGroups(chooser, blacklist, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#deleteUser(WListClientInterface, String, long)
     */
    private static final @NotNull ServerHandler doDeleteUser = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.UsersOperate);
        final long userId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.DeleteUser, changer, () -> ParametersMap.create()
                .add("userId", userId).optionallyAdd(changer.isSuccess(), "denied", UserManager.getAdminId() == userId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (UserManager.getAdminId() == userId)
            message = OperateUsersHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            if (!UserManager.deleteUser(userId, null)) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.UserDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted user.", ServerHandler.user("changer", changer.getT()),
                    ParametersMap.create().add("userId", userId));
            BroadcastManager.onUserLogoff(userId);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#deleteUsersInGroup(WListClientInterface, String, long)
     */
    private static final @NotNull ServerHandler doDeleteUsersInGroup = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> changer = OperateSelfHandler.checkToken(token, UserPermission.UsersOperate);
        final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.DeleteUsersInGroup, changer, () -> ParametersMap.create()
                .add("groupId", groupId).optionallyAddSupplier(changer.isSuccess(), "denied", () -> changer.getT().group().id() == groupId));
        MessageProto message = null;
        if (changer.isFailure())
            message = changer.getE();
        else if (changer.getT().group().id() == groupId)
            message = OperateUsersHandler.UserDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            if (UserGroupManager.selectGroup(groupId, null) == null) {
                WListServer.ServerChannelHandler.write(channel, OperateUsersHandler.GroupDataError);
                return;
            }
            final long count = UserManager.deleteUsersByGroup(groupId, null);
            if (count > 0)
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted users in group.", ServerHandler.user("changer", changer.getT()),
                        ParametersMap.create().add("groupId", groupId).add("count", count));
            BroadcastManager.onUsersLogoff(groupId);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, count);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#searchUsersRegex(WListClientInterface, String, String, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doSearchUserRegex = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final String regex = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<LinkedHashMap<VisibleUserInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleUserInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.SearchUserRegex, user, () -> ParametersMap.create()
                .add("regex", regex).add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (orders == null || orders.isFailure())
            message = OperateUsersHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserInformation>> list = UserManager.searchUsersByRegex(regex, orders.getT(), position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper#searchUserpsName(WListClientInterface, String, Set, long, int)
     */
    private static final @NotNull ServerHandler doSearchUserName = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.UsersList);
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final Set<String> names = new HashSet<>(length);
        for (int i = 0; i < length; ++i)
            names.add(ByteBufIOUtil.readUTF(buffer));
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.SearchUserName, user, () -> ParametersMap.create()
                .add("names", names).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (names.isEmpty())
            message = OperateUsersHandler.ChooserDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final Pair.ImmutablePair<Long, List<UserInformation>> list = UserManager.searchUsersByNames(names, position, limit, null);
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
                ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
                for (final UserInformation information: list.getSecond())
                    information.dumpVisible(buf);
                return buf;
            }));
        };
    };
}
