package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateGroupsHandler
 */
public final class OperateGroupsHelper {
    private OperateGroupsHelper() {
        super();
    }

    public static boolean addGroup(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.AddGroup, token);
        ByteBufIOUtil.writeUTF(send, groupName);
        OperateHelper.logOperating(OperationType.AddGroup, token, p -> p.add("groupName", groupName));
        return OperateHelper.booleanOperation(client, send, OperationType.AddGroup);
    }

    public static boolean changeGroupName(final @NotNull WListClientInterface client, final @NotNull String token, final long groupId, final @NotNull String newGroupName) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeGroupName, token);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        ByteBufIOUtil.writeUTF(send, newGroupName);
        OperateHelper.logOperating(OperationType.ChangeGroupName, token, p -> p.add("groupId", groupId).add("newGroupName", newGroupName));
        return OperateHelper.booleanOperation(client, send, OperationType.ChangeGroupName);
    }

    public static boolean changeGroupPermissions(final @NotNull WListClientInterface client, final @NotNull String token, final long groupId, final @NotNull Set<@NotNull UserPermission> permissions) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeGroupPermissions, token);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        ByteBufIOUtil.writeUTF(send, UserPermission.dump(permissions));
        OperateHelper.logOperating(OperationType.ChangeGroupPermissions, token, p -> p.add("groupId", groupId).add("permissions", permissions));
        return OperateHelper.booleanOperation(client, send, OperationType.ChangeGroupPermissions);
    }

    public static @Nullable VisibleUserGroupInformation getGroup(final @NotNull WListClientInterface client, final @NotNull String token, final long groupId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetGroup, token);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        OperateHelper.logOperating(OperationType.GetGroup, token, p -> p.add("groupId", groupId));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final VisibleUserGroupInformation information = VisibleUserGroupInformation.parse(receive);
                OperateHelper.logOperated(OperationType.GetGroup, null, p -> p.add("information", information));
                return information;
            }
            OperateHelper.logOperated(OperationType.GetGroup, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserGroupInformation>> listGroups(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListGroups, token);
        OrderPolicies.dump(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateGroupsHelper.pairListOperation(client, send, OperationType.ListGroups, token, p -> p.add("orders", orders).add("position", position).add("limit", limit));
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserGroupInformation>> listGroupsInPermissions(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull Map<@NotNull UserPermission, @Nullable Boolean> chooser, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListGroupsInPermissions, token);
        UserPermission.dumpChooser(send, chooser);
        OrderPolicies.dump(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateGroupsHelper.pairListOperation(client, send, OperationType.ListGroupsInPermissions, token, p -> p.add("chooser", chooser).add("orders", orders).add("position", position).add("limit", limit));
    }

    public static boolean deleteGroup(final @NotNull WListClientInterface client, final @NotNull String token, final long groupId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DeleteGroup, token);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        OperateHelper.logOperating(OperationType.DeleteGroup, token, p -> p.add("groupId", groupId));
        return OperateHelper.booleanOperation(client, send, OperationType.DeleteGroup);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserGroupInformation>> searchGroupsRegex(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.SearchGroupRegex, token);
        ByteBufIOUtil.writeUTF(send, regex);
        OrderPolicies.dump(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateGroupsHelper.pairListOperation(client, send, OperationType.SearchGroupRegex, token, p -> p.add("regex", regex).add("orders", orders).add("position", position).add("limit", limit));
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserGroupInformation>> searchGroupsName(final @NotNull WListClientInterface client, final @NotNull String token, @SuppressWarnings("TypeMayBeWeakened") final @NotNull Set<@NotNull String> names, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.SearchGroupName, token);
        ByteBufIOUtil.writeVariableLenInt(send, names.size());
        for (final String name: names)
            ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateGroupsHelper.pairListOperation(client, send, OperationType.SearchGroupName, token, p -> p.add("names", names).add("position", position).add("limit", limit));
    }

    private static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserGroupInformation>> pairListOperation(final @NotNull WListClientInterface client, final @NotNull ByteBuf send, final @NotNull OperationType type, final @Nullable String token, final @NotNull Consumer<? super @NotNull ParametersMap> parameters) throws IOException, InterruptedException, WrongStateException {
        OperateHelper.logOperating(type, token, parameters);
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final long total = ByteBufIOUtil.readVariableLenLong(receive);
                final int length = ByteBufIOUtil.readVariableLenInt(receive);
                final List<VisibleUserGroupInformation> list = new ArrayList<>(length);
                for (int i = 0; i < length; ++i)
                    list.add(VisibleUserGroupInformation.parse(receive));
                OperateHelper.logOperated(type, null, p -> p.add("total", total).add("list", list));
                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
            }
            OperateHelper.logOperated(type, reason, null);
            final ParametersMap map = ParametersMap.create();
            parameters.accept(map);
            throw new WrongStateException(ResponseState.DataError, reason + map);
        } finally {
            receive.release();
        }
    }
}
