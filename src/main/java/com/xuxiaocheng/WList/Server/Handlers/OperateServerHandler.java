package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
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
        ServerHandlerManager.register(OperationType.SetBroadcastMode, OperateServerHandler.doSetBroadcastMode);
        ServerHandlerManager.register(OperationType.CloseServer, OperateServerHandler.doCloseServer);
        ServerHandlerManager.register(OperationType.Broadcast, OperateServerHandler.doBroadcast);
        ServerHandlerManager.register(OperationType.ResetConfiguration, OperateServerHandler.doResetConfiguration);
    }

    private static final @NotNull ServerHandler doSetBroadcastMode = (channel, buffer) -> {
        final boolean receive = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, OperationType.SetBroadcastMode, null, () -> ParametersMap.create().add("receive", receive));
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
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate);
        ServerHandler.logOperation(channel, OperationType.CloseServer, user, null);
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            WListServer.ServerExecutors.execute(() -> {
                try {
                    final ChannelGroupFuture future = BroadcastManager.broadcast(OperationType.CloseServer, null);
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
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.Broadcast);
        final String message = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.Broadcast, user, () -> ParametersMap.create()
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
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate);
        final ServerConfiguration configuration = ServerConfiguration.parse(new ByteBufInputStream(buffer));
        ServerHandler.logOperation(channel, OperationType.ResetConfiguration, user, () -> ParametersMap.create()
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
                WListServer.ServerChannelHandler.write(channel, MessageProto.WrongParameters);
            }
        };
    };
}
