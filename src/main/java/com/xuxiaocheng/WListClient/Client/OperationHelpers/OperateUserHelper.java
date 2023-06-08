package com.xuxiaocheng.WListClient.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleUserGroupInformation;
import com.xuxiaocheng.WListClient.Server.VisibleUserInformation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OperateUserHelper {
    private OperateUserHelper() {
        super();
    }

    public static boolean register(final @NotNull WListClient client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(Operation.Type.Register);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @Nullable String login(final @NotNull WListClient client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(Operation.Type.Login);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return ByteBufIOUtil.readUTF(receive);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleUserGroupInformation getPermissions(final @NotNull WListClient client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.GetPermissions, token);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return VisibleUserGroupInformation.parse(receive);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean changeUsername(final @NotNull WListClient client, final @NotNull String token, final @NotNull String newUsername) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ChangeUsername, token);
        ByteBufIOUtil.writeUTF(send, newUsername);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static boolean changePassword(final @NotNull WListClient client, final @NotNull String token, final @NotNull String oldPassword, final @NotNull String newPassword) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ChangePassword, token);
        ByteBufIOUtil.writeUTF(send, oldPassword);
        ByteBufIOUtil.writeUTF(send, newPassword);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static boolean logoff(final @NotNull WListClient client, final @NotNull String token, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.Logoff, token);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserInformation>> listUsers(final @NotNull WListClient client, final @NotNull String token, final int limit, final int page, final Options.@NotNull OrderDirection direction) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListUsers, token);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        ByteBufIOUtil.writeVariableLenInt(send, page);
        ByteBufIOUtil.writeUTF(send, direction.name());
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive)) {
                final long total = ByteBufIOUtil.readVariableLenLong(receive);
                final int count = ByteBufIOUtil.readVariableLenInt(receive);
                final List<VisibleUserInformation> list = new ArrayList<>(count);
                for (int i = 0; i < count; ++i)
                    list.add(VisibleUserInformation.parse(receive));
                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
            }
            assert "Parameters".equals(ByteBufIOUtil.readUTF(receive));
            throw new IllegalArgumentException();
        } finally {
            receive.release();
        }
    }

    public static boolean deleteUser(final @NotNull WListClient client, final @NotNull String token, final @NotNull String username) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteUser, token);
        ByteBufIOUtil.writeUTF(send, username);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return true;
            assert "User".equals(ByteBufIOUtil.readUTF(receive));
            return false;
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserGroupInformation>> listGroups(final @NotNull WListClient client, final @NotNull String token, final int limit, final int page, final Options.@NotNull OrderDirection direction) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListGroups, token);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        ByteBufIOUtil.writeVariableLenInt(send, page);
        ByteBufIOUtil.writeUTF(send, direction.name());
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive)) {
                final long total = ByteBufIOUtil.readVariableLenLong(receive);
                final int count = ByteBufIOUtil.readVariableLenInt(receive);
                final List<VisibleUserGroupInformation> list = new ArrayList<>(count);
                for (int i = 0; i < count; ++i)
                    list.add(VisibleUserGroupInformation.parse(receive));
                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
            }
            assert "Parameters".equals(ByteBufIOUtil.readUTF(receive));
            throw new IllegalArgumentException();
        } finally {
            receive.release();
        }
    }

    public static boolean addGroup(final @NotNull WListClient client, final @NotNull String token, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.AddGroup, token);
        ByteBufIOUtil.writeUTF(send, groupName);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @Nullable Boolean deleteGroup(final @NotNull WListClient client, final @NotNull String token, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteGroup, token);
        ByteBufIOUtil.writeUTF(send, groupName);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return true;
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Users".equals(reason))
                return false;
            assert "Group".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable Boolean changeGroup(final @NotNull WListClient client, final @NotNull String token, final @NotNull String username, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ChangeGroup, token);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, groupName);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return true;
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("User".equals(reason))
                return false;
            assert "Group".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean changePermission(final @NotNull WListClient client, final @NotNull String token, final @NotNull String groupName, final boolean add, final @NotNull Iterable<Operation.@NotNull Permission> permissions) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(add ? Operation.Type.AddPermission : Operation.Type.RemovePermission, token);
        ByteBufIOUtil.writeUTF(send, groupName);
        ByteBufIOUtil.writeUTF(send, Operation.dumpPermissions(permissions));
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return true;
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Permissions".equals(reason))
                throw new IllegalArgumentException();
            assert "Group".equals(reason);
            return false;
        } finally {
            receive.release();
        }
    }
}
