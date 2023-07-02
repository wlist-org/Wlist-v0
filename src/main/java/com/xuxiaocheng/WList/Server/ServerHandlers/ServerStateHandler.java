package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public final class ServerStateHandler {
    private ServerStateHandler() {
        super();
    }

    public static final @NotNull ServerHandler doCloseServer = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.ServerOperate);
        if (user.isFailure())
            return user.getE();
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().stop(), 3, TimeUnit.SECONDS);
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doBroadcast = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.Broadcast);
        if (user.isFailure())
            return user.getE();
        buffer.retain();
        final ByteBuf head = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(head, Operation.State.Broadcast.name());
        ByteBufIOUtil.writeUTF(head, user.getT().username());
        final CompositeByteBuf msg = ByteBufAllocator.DEFAULT.compositeBuffer(2);
        msg.addComponents(true, head, buffer);
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().broadcast(msg), 1, TimeUnit.SECONDS);
        return ServerHandler.Success;
    };

    public static @NotNull MessageProto doSetBroadcastMode(final @NotNull ByteBuf buffer, final @NotNull Collection<? super @NotNull Channel> group, final @NotNull Channel channel) throws IOException {
        final boolean allow = ByteBufIOUtil.readBoolean(buffer);
        if (allow)
            group.add(channel);
        else
            group.remove(channel);
        return ServerHandler.Success;
    }
}
