package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    /**
     * @see #onProviderInitialized(String, long)
     * @see #onProviderUninitialized(String)
     * @see #onProviderLogin(String, boolean)
     */
    public static final @NotNull Set<@NotNull OperationType> OrderedBroadcastType = Set.of(OperationType.AddProvider, OperationType.RemoveProvider, OperationType.Login);

    private static final @NotNull Lock lock = new ReentrantLock(true);
    public static @NotNull Future<?> broadcast(final @NotNull OperationType type, final MessageProto.@Nullable Appender message) {
        final ByteBuf buffer;
        final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufIOUtil.writeBoolean(prefix, false);
            ByteBufIOUtil.writeUTF(prefix, type.name());
            buffer = message == null ? prefix : message.apply(prefix);
            if (BroadcastManager.OrderedBroadcastType.contains(type)) {
                BroadcastManager.lock.lock();
                try {
                    return BroadcastManager.broadcastGroup.writeAndFlush(buffer.retain()).addListener(MiscellaneousUtil.exceptionListener()).await();
                } finally {
                    BroadcastManager.lock.unlock();
                }
            }
            return BroadcastManager.broadcastGroup.writeAndFlush(buffer.retain()).addListener(MiscellaneousUtil.exceptionListener());
        } catch (final IOException | InterruptedException exception) {
            return new FailedFuture<>(BackgroundTaskManager.BackgroundExecutors.next(), exception);
        } finally {
            prefix.release();
        }
    }

    public static void broadcastUser(final @NotNull String sender, final @NotNull String message) {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufIOUtil.writeBoolean(buffer, true);
            ByteBufIOUtil.writeUTF(buffer, sender);
            ByteBufIOUtil.writeUTF(buffer, message);
            BroadcastManager.broadcastGroup.writeAndFlush(buffer.retain()).addListener(MiscellaneousUtil.exceptionListener());
        } catch (final IOException exception) {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
        } finally {
            buffer.release();
        }
    }


    public static void onUserLogon(final @NotNull UserInformation information) {
        BroadcastManager.broadcast(OperationType.Logon, information::dumpVisible).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserLogoff(final long id) {
        BroadcastManager.broadcast(OperationType.Logoff, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserChangeName(final long id, final @NotNull String newName, final @NotNull ZonedDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeUsername, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, newName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserChangePassword(final long id, final @NotNull ZonedDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangePassword, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }


    public static void onUserGroupAdded(final @NotNull UserGroupInformation information) {
        BroadcastManager.broadcast(OperationType.AddGroup, information::dumpVisible).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserGroupChangeName(final long id, final @NotNull String newName, final @NotNull ZonedDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeGroupName, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, newName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserGroupChangePermissions(final long id, final @NotNull Set<@NotNull UserPermission> newPermissions, final @NotNull ZonedDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeGroupPermissions, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeUTF(buf, UserPermission.dump(newPermissions));
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUserGroupDeleted(final long id) {
        BroadcastManager.broadcast(OperationType.DeleteGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }


    public static void onUserChangeGroup(final long id, final long groupId, final @NotNull String groupName, final @NotNull ZonedDateTime updateTime) {
        BroadcastManager.broadcast(OperationType.ChangeUserGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            ByteBufIOUtil.writeVariableLenLong(buf, groupId);
            ByteBufIOUtil.writeUTF(buf, groupName);
            ByteBufIOUtil.writeUTF(buf, updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onUsersLogoff(final long groupId) {
        BroadcastManager.broadcast(OperationType.DeleteUsersInGroup, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, groupId);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }


    public static void onProviderInitialized(final @NotNull String storage, final long id) {
        BroadcastManager.broadcast(OperationType.AddProvider, buf -> {
            ByteBufIOUtil.writeUTF(buf, storage);
            ByteBufIOUtil.writeVariableLenLong(buf, id);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onProviderUninitialized(final @NotNull String storage) {
        BroadcastManager.broadcast(OperationType.RemoveProvider, buf -> {
            ByteBufIOUtil.writeUTF(buf, storage);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onProviderLogin(final @NotNull String storage, final boolean enter) {
        BroadcastManager.broadcast(OperationType.Login, buf -> { // Not user login.
            ByteBufIOUtil.writeUTF(buf, storage);
            ByteBufIOUtil.writeBoolean(buf, enter);
            return buf;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }


    private static MessageProto.@NotNull Appender fileDumper(final @NotNull FileLocation location, final boolean isDirectory) {
        return buffer -> {
            location.dump(buffer);
            ByteBufIOUtil.writeBoolean(buffer, isDirectory);
            return buffer;
        };
    }

    public static void onFileTrash(final @NotNull FileLocation location, final boolean isDirectory) {
        BroadcastManager.broadcast(OperationType.TrashFileOrDirectory, BroadcastManager.fileDumper(location, isDirectory)).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onFileUpdate(final @NotNull FileLocation location, final boolean isDirectory) {
        BroadcastManager.broadcast(OperationType.GetFileOrDirectory, BroadcastManager.fileDumper(location, isDirectory)).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void onFileUpload(final @NotNull String storage, final @NotNull FileInformation information) {
        BroadcastManager.broadcast(OperationType.UploadFile, buffer -> {
            ByteBufIOUtil.writeUTF(buffer, storage);
            information.dumpVisible(buffer);
            return buffer;
        }).addListener(MiscellaneousUtil.exceptionListener());
    }
}
