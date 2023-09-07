package com.xuxiaocheng.WList.Server.Handlers;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;

public final class ServerStateHandler {
    private ServerStateHandler() {
        super();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final @NotNull ChannelGroup ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void initialize() {
//        ServerHandlerManager.register(Operation.Type.CloseServer, ServerStateHandler.doCloseServer);
//        ServerHandlerManager.register(Operation.Type.Broadcast, ServerStateHandler.doBroadcast);
//        ServerHandlerManager.register(Operation.Type.SetBroadcastMode, ServerStateHandler.doSetBroadcastMode);
    }

//    private static final @NotNull ServerHandler doCloseServer = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.ServerOperate);
//        ServerHandler.logOperation(channel, Operation.Type.CloseServer, user, null);
//        if (user.isFailure())
//            return user.getE();
//        WListServer.ServerExecutors.execute(() -> WListServer.getInstance().stop());
//        return MessageProto.Success;
//    };
//
//    private static final @NotNull ServerHandler doBroadcast = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.Broadcast);
//        ServerHandler.logOperation(channel, Operation.Type.Broadcast, user, () -> ParametersMap.create()
//                .add("len", buffer.readableBytes()));
//        if (user.isFailure())
//            return user.getE();
//        final ByteBuf header = ByteBufAllocator.DEFAULT.buffer();
//        ByteBufIOUtil.writeUTF(header, Operation.State.Broadcast.name());
//        ByteBufIOUtil.writeUTF(header, user.getT().username());
//        final CompositeByteBuf msg = ByteBufAllocator.DEFAULT.compositeBuffer(2);
//        msg.addComponents(true, header, buffer.retainedDuplicate());
//        ServerStateHandler.ChannelGroup.writeAndFlush(msg);
//        buffer.readerIndex(buffer.writerIndex());
//        return MessageProto.Success;
//    };
//
//    private static final @NotNull ServerHandler doSetBroadcastMode = (channel, buffer) -> {
//        if (ByteBufIOUtil.readBoolean(buffer))
//            ServerStateHandler.ChannelGroup.add(channel);
//        else
//            ServerStateHandler.ChannelGroup.remove(channel);
//        return MessageProto.Success;
//    };
}
