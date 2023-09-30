package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateUsersHandler
 */
public final class OperateUsersHelper {
    private OperateUsersHelper() {
        super();
    }

    public static boolean changeUserGroup(final @NotNull WListClientInterface client, final @NotNull String token, final long userId, final long groupId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeUserGroup, token);
        ByteBufIOUtil.writeVariableLenLong(send, userId);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        OperateHelper.logOperating(OperationType.ChangeUserGroup, token, p -> p.add("userId", userId).add("groupId", groupId));
        return OperateHelper.booleanOperation(client, send, OperationType.ChangeUserGroup);
    }

    public static @Nullable VisibleUserInformation getUser(final @NotNull WListClientInterface client, final @NotNull String token, final long userId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetUser, token);
        ByteBufIOUtil.writeVariableLenLong(send, userId);
        OperateHelper.logOperating(OperationType.GetUser, token, p -> p.add("userId", userId));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final VisibleUserInformation information = VisibleUserInformation.parse(receive);
                OperateHelper.logOperated(OperationType.GetUser, null, p -> p.add("information", information));
                return information;
            }
            OperateHelper.logOperated(OperationType.GetUser, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserInformation>> listUsers(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListUsers, token);
        Options.dumpOrderPolicies(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateUsersHelper.pairListOperation(client, send, OperationType.ListUsers, token, p -> p.add("orders", orders).add("position", position).add("limit", limit));
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserInformation>> listUsersInGroups(final @NotNull WListClientInterface client, final @NotNull String token, @SuppressWarnings("TypeMayBeWeakened") final @NotNull Set<@NotNull Long> chooser, final boolean blacklist, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListUsersInGroups, token);
        ByteBufIOUtil.writeVariableLenInt(send, chooser.size());
        for (final Long id: chooser)
            ByteBufIOUtil.writeVariableLenLong(send, id.longValue());
        ByteBufIOUtil.writeBoolean(send, blacklist);
        Options.dumpOrderPolicies(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateUsersHelper.pairListOperation(client, send, OperationType.ListUsersInGroups, token, p -> p.add("chooser", chooser).add("blacklist", blacklist).add("orders", orders).add("position", position).add("limit", limit));
    }

    public static boolean deleteUser(final @NotNull WListClientInterface client, final @NotNull String token, final long userId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DeleteUser, token);
        ByteBufIOUtil.writeVariableLenLong(send, userId);
        OperateHelper.logOperating(OperationType.DeleteUser, token, p -> p.add("userId", userId));
        return OperateHelper.booleanOperation(client, send, OperationType.DeleteUser);
    }

    public static long deleteUsersInGroup(final @NotNull WListClientInterface client, final @NotNull String token, final long groupId) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DeleteUsersInGroup, token);
        ByteBufIOUtil.writeVariableLenLong(send, groupId);
        OperateHelper.logOperating(OperationType.DeleteUsersInGroup, token, p -> p.add("userId", groupId));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final long count = ByteBufIOUtil.readVariableLenLong(receive);
                OperateHelper.logOperated(OperationType.DeleteUsersInGroup, null, p -> p.add("count", count));
                return count;
            }
            OperateHelper.logOperated(OperationType.DeleteUsersInGroup, reason, null);
            return -1;
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserInformation>> searchUsersRegex(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.SearchUserRegex, token);
        ByteBufIOUtil.writeUTF(send, regex);
        Options.dumpOrderPolicies(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateUsersHelper.pairListOperation(client, send, OperationType.SearchUserRegex, token, p -> p.add("regex", regex).add("orders", orders).add("position", position).add("limit", limit));
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserInformation>> searchUserName(final @NotNull WListClientInterface client, final @NotNull String token, @SuppressWarnings("TypeMayBeWeakened") final @NotNull Set<@NotNull String> names, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.SearchUserName, token);
        ByteBufIOUtil.writeVariableLenInt(send, names.size());
        for (final String name: names)
            ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        return OperateUsersHelper.pairListOperation(client, send, OperationType.SearchUserName, token, p -> p.add("names", names).add("position", position).add("limit", limit));
    }

    private static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull VisibleUserInformation>> pairListOperation(final @NotNull WListClientInterface client, final @NotNull ByteBuf send, final @NotNull OperationType type, final @Nullable String token, final @NotNull Consumer<? super @NotNull ParametersMap> parameters) throws IOException, InterruptedException, WrongStateException {
        OperateHelper.logOperating(type, token, parameters);
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final long total = ByteBufIOUtil.readVariableLenLong(receive);
                final int length = ByteBufIOUtil.readVariableLenInt(receive);
                final List<VisibleUserInformation> list = new ArrayList<>(length);
                for (int i = 0; i < length; ++i)
                    list.add(VisibleUserInformation.parse(receive));
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
