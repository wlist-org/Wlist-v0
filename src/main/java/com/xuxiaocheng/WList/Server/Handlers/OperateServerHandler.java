package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.BroadcastManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.group.ChannelGroupFuture;
import org.jetbrains.annotations.NotNull;

public final class OperateServerHandler {
    private OperateServerHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.SetBroadcastMode, OperateServerHandler.doSetBroadcastMode);
        ServerHandlerManager.register(Operation.Type.CloseServer, OperateServerHandler.doCloseServer);
        ServerHandlerManager.register(Operation.Type.Broadcast, OperateServerHandler.doBroadcast);
        ServerHandlerManager.register(Operation.Type.ResetConfiguration, OperateServerHandler.doResetConfiguration);
    }

    private static final @NotNull ServerHandler doSetBroadcastMode = (channel, buffer) -> {
        final boolean receive = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, Operation.Type.SetBroadcastMode, null, () -> ParametersMap.create().add("receive", receive));
        return () -> {
            if (receive) {
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
                BroadcastManager.addBroadcast(channel);
            } else {
                BroadcastManager.removeBroadcast(channel);
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
            }
        };
    };

    private static final @NotNull ServerHandler doCloseServer = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, Operation.Permission.ServerOperate);
        ServerHandler.logOperation(channel, Operation.Type.CloseServer, user, null);
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            WListServer.ServerExecutors.execute(() -> {
                try {
                    final ChannelGroupFuture future = BroadcastManager.broadcast(Operation.Type.CloseServer, null);
                    if (future != null)
                        future.await();
                } catch (final InterruptedException exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                } finally {
                    WListServer.getInstance().stop();
                }
            });
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doBroadcast = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, Operation.Permission.Broadcast);
        final String message = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.Broadcast, user, () -> ParametersMap.create()
                .add("len", message.length()).add("hash", message.hashCode()));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            BroadcastManager.broadcastUser(user.getT().username(), message);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    private static final @NotNull ServerHandler doResetConfiguration = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, Operation.Permission.ServerOperate);
        final ServerConfiguration configuration = ServerConfiguration.parse(new ByteBufInputStream(buffer));
        ServerHandler.logOperation(channel, Operation.Type.ResetConfiguration, user, () -> ParametersMap.create()
                .add("configuration", configuration).add("old", ServerConfiguration.get()));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            try {
                ServerConfiguration.set(configuration);
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
            } catch (final IllegalStateException exception) {
                WListServer.ServerChannelHandler.write(channel, MessageProto.DataError);
            }
        };
    };
}
