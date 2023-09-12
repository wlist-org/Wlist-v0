package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public final class BroadcastManager {
    private BroadcastManager() {
        super();
    }

    private static final @NotNull ChannelGroup broadcastGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void addBroadcast(final @NotNull Channel channel) {
        BroadcastManager.broadcastGroup.add(channel);
    }

    public static void removeBroadcast(final @NotNull Channel channel) {
        BroadcastManager.broadcastGroup.remove(channel);
    }

    public static boolean isBroadcast(final @NotNull Channel channel) {
        return BroadcastManager.broadcastGroup.contains(channel);
    }

    public static @Nullable ChannelGroupFuture broadcast(final @NotNull OperationType type, final MessageProto.@Nullable Appender message) {
        final ByteBuf buffer;
        final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufIOUtil.writeBoolean(prefix, false);
            ByteBufIOUtil.writeUTF(prefix, type.name());
            buffer = message == null ? prefix : message.apply(prefix);
            buffer.retain();
        } catch (final IOException exception) {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
            return null;
        } finally {
            prefix.release();
        }
        return BroadcastManager.broadcastGroup.writeAndFlush(buffer);
    }

    public static void broadcastUser(final @NotNull String sender, final @NotNull String message) {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufIOUtil.writeBoolean(buffer, true);
            ByteBufIOUtil.writeUTF(buffer, sender);
            ByteBufIOUtil.writeUTF(buffer, message);
            BroadcastManager.broadcastGroup.writeAndFlush(buffer.retain());
        } catch (final IOException exception) {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
        } finally {
            buffer.release();
        }
    }


    public static void onUserLogon(final @NotNull UserInformation information) {
        BroadcastManager.broadcast(OperationType.Logon, information::dumpVisible);
    }

    public static void onUserLogoff(final long id) {
        BroadcastManager.broadcast(OperationType.Logoff, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            return buf;
        });
    }

    public static void onUserChangeName(final long id, final @NotNull String newName, final @NotNull LocalDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeUsername, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, newName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        });
    }

    public static void onUserChangePassword(final long id, final @NotNull LocalDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangePassword, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        });
    }


    public static void onUserGroupAdded(final @NotNull UserGroupInformation information) {
        BroadcastManager.broadcast(OperationType.AddGroup, information::dumpVisible);
    }

    public static void onUserGroupChangeName(final long id, final @NotNull String newName, final @NotNull LocalDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeGroupName, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, newName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        });
    }

    public static void onUserGroupChangePermissions(final long id, final @NotNull Set<@NotNull UserPermission> newPermissions, final @NotNull LocalDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeGroupPermissions, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, UserPermission.dump(newPermissions));
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        });
    }

    public static void onUserGroupDeleted(final long id) {
        BroadcastManager.broadcast(OperationType.DeleteGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            return buf;
        });
    }


    public static void onUserChangeGroup(final long id, final long groupId, final @NotNull String groupName, final @NotNull LocalDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeUserGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeVariableLenLong(buf, groupId);
            ByteBufIOUtil.writeUTF(buf, groupName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        });
    }

    public static void onUsersLogoff(final long groupId) {
        BroadcastManager.broadcast(OperationType.DeleteUsersInGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, groupId);
            return buf;
        });
    }
}
