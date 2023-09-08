package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
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

public final class BroadcastManager {
    private BroadcastManager() {
        super();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final @NotNull ChannelGroup broadcastGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void addBroadcast(final @NotNull Channel channel) {
        BroadcastManager.broadcastGroup.add(channel);
    }

    public static void removeBroadcast(final @NotNull Channel channel) {
        BroadcastManager.broadcastGroup.remove(channel);
    }

    public static @Nullable ChannelGroupFuture broadcast(final Operation.@NotNull Type type, final MessageProto.@Nullable Appender message) {
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
            return;
        } finally {
            buffer.release();
        }
    }
}
