package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public final class ServerStateHandler {
    private ServerStateHandler() {
        super();
    }

    public static void doCloseServer(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.ServerOperate) == null)
            return;
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().stop(), 3, TimeUnit.SECONDS);
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doBroadcast(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.Broadcast);
        if (user == null)
            return;
        buf.retain();
        final ByteBuf head = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(head, Operation.State.Broadcast.name());
        ByteBufIOUtil.writeUTF(head, user.getA());
        final CompositeByteBuf msg = ByteBufAllocator.DEFAULT.compositeBuffer(2);
        msg.addComponents(true, head, buf);
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().writeChannels(msg), 1, TimeUnit.SECONDS);
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }
}
