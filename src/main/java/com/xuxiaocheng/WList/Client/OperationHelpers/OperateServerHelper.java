package com.xuxiaocheng.WList.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class OperateServerHelper {
    private OperateServerHelper() {
        super();
    }

    public static void setBroadcastMode(final @NotNull WListClientInterface client, final boolean allow) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(OperationType.SetBroadcastMode);
        ByteBufIOUtil.writeBoolean(send, allow);
        final ByteBuf receive = client.send(send);
        try {
            while (true)
                try {
                    if (OperateHelper.handleState(receive))
                        return;
                    else
                        throw new WrongStateException(ResponseState.DataError, receive.toString());
                } catch (final WrongStateException exception) {
                    if (exception.getState() == ResponseState.Success)
                        continue;
                    throw exception;
                }
        } finally {
            receive.release();
        }
    }

    public static boolean closeServer(final @NotNull WListClientInterface client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CloseServer, token);
        OperateHelper.logOperating(OperationType.CloseServer, token, null);
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.CloseServer, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static void broadcast(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String message) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.Broadcast, token);
        ByteBufIOUtil.writeUTF(send, message);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return;
            throw new WrongStateException(ResponseState.DataError, receive.toString());
        } finally {
            receive.release();
        }
    }

    public static @NotNull UnionPair<Pair.ImmutablePair<@NotNull OperationType, @NotNull ByteBuf>, Pair.ImmutablePair<@NotNull String, @NotNull String>> waitBroadcast(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf broadcast = client.send(null);
        try {
            final boolean isUserSend = ByteBufIOUtil.readBoolean(broadcast);
            if (isUserSend) {
                final String sender = ByteBufIOUtil.readUTF(broadcast);
                final String message = ByteBufIOUtil.readUTF(broadcast);
                return UnionPair.fail(Pair.ImmutablePair.makeImmutablePair(sender, message));
            }
            final OperationType type = OperationType.of(ByteBufIOUtil.readUTF(broadcast));
            return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(type, broadcast.retain()));
        } finally {
            broadcast.release();
        }
    }

    public static boolean resetConfiguration(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull ServerConfiguration configuration) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ResetConfiguration, token);
        ServerConfiguration.dump(configuration, new ByteBufOutputStream(send));
        OperateHelper.logOperating(OperationType.ResetConfiguration, token, p -> p.add("configuration", configuration));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.ResetConfiguration, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }
}
