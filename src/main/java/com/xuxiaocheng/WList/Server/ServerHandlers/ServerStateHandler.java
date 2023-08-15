package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class ServerStateHandler {
    private ServerStateHandler() {
        super();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final @NotNull ChannelGroup ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.Undefined, ServerStateHandler.doUndefined);
        ServerHandlerManager.register(Operation.Type.CloseServer, ServerStateHandler.doCloseServer);
        ServerHandlerManager.register(Operation.Type.Broadcast, ServerStateHandler.doBroadcast);
        ServerHandlerManager.register(Operation.Type.SetBroadcastMode, ServerStateHandler.doSetBroadcastMode);
    }

    private static final @NotNull ServerHandler doUndefined = (channel, buffer) -> {
        ServerHandler.logOperation(channel, Operation.Type.Undefined, null, HExceptionWrapper.wrapSupplier(() -> ParametersMap.create()
                .add("type", ByteBufIOUtil.readUTF(buffer.resetReaderIndex()))));
        return ServerHandler.composeMessage(Operation.State.Unsupported, "Undefined operation!");
    };

    private static final @NotNull ServerHandler doCloseServer = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.ServerOperate);
        ServerHandler.logOperation(channel, Operation.Type.CloseServer, user, null);
        if (user.isFailure())
            return user.getE();
        // TODO refuse new connections.
        WListServer.ServerExecutors.schedule(() -> WListServer.getInstance().stop(), 3, TimeUnit.SECONDS);
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doBroadcast = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.Broadcast);
        ServerHandler.logOperation(channel, Operation.Type.Broadcast, user, () -> ParametersMap.create()
                .add("len", buffer.readableBytes()));
        if (user.isFailure())
            return user.getE();
        final ByteBuf header = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(header, Operation.State.Broadcast.name());
        ByteBufIOUtil.writeUTF(header, user.getT().username());
        final CompositeByteBuf msg = ByteBufAllocator.DEFAULT.compositeBuffer(2);
        msg.addComponents(true, header, buffer.retainedDuplicate());
        ServerStateHandler.ChannelGroup.writeAndFlush(msg);
        buffer.readerIndex(buffer.writerIndex());
        return ServerHandler.Success;
    };

    private static final @NotNull ServerHandler doSetBroadcastMode = (channel, buffer) -> {
        if (ByteBufIOUtil.readBoolean(buffer))
            ServerStateHandler.ChannelGroup.add(channel);
        else
            ServerStateHandler.ChannelGroup.remove(channel);
        return ServerHandler.Success;
    };
}
