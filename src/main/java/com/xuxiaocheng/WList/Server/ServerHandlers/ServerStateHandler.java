package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.Polymers.UserTokenInfo;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class ServerStateHandler {
    private ServerStateHandler() {
        super();
    }

    public static final @NotNull ServerHandler doCloseServer = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.ServerOperate);
        if (user.isFailure())
            return user.getE();
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().stop(), 3, TimeUnit.SECONDS);
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };

    public static final @NotNull ServerHandler doBroadcast = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.Broadcast);
        if (user.isFailure())
            return user.getE();
        buffer.retain();
        final ByteBuf head = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(head, Operation.State.Broadcast.name());
        ByteBufIOUtil.writeUTF(head, user.getT().username());
        final CompositeByteBuf msg = ByteBufAllocator.DEFAULT.compositeBuffer(2);
        msg.addComponents(true, head, buffer);
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().writeChannels(msg), 1, TimeUnit.SECONDS);
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };
}
