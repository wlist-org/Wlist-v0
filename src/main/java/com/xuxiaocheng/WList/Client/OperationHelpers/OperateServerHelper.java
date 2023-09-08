package com.xuxiaocheng.WList.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operation;
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
        final ByteBuf send = OperateHelper.operate(Operation.Type.SetBroadcastMode);
        ByteBufIOUtil.writeBoolean(send, allow);
        final ByteBuf receive = client.send(send);
        try {
            while (true)
                try {
                    if (OperateHelper.handleState(receive))
                        return;
                    else
                        throw new WrongStateException(Operation.State.DataError, receive.toString());
                } catch (final WrongStateException exception) {
                    if (exception.getState() == Operation.State.Success)
                        continue;
                    throw exception;
                }
        } finally {
            receive.release();
        }
    }

    public static boolean closeServer(final @NotNull WListClientInterface client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CloseServer, token);
        OperateHelper.logOperating(Operation.Type.CloseServer, token, null);
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(Operation.Type.CloseServer, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static void broadcast(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String message) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.Broadcast, token);
        ByteBufIOUtil.writeUTF(send, message);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return;
            throw new WrongStateException(Operation.State.DataError, receive.toString());
        } finally {
            receive.release();
        }
    }

    public static @NotNull UnionPair<Pair.ImmutablePair<Operation.@NotNull Type, @NotNull ByteBuf>, Pair.ImmutablePair<@NotNull String, @NotNull String>> waitBroadcast(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf broadcast = client.send(null);
        try {
            final boolean isUserSend = ByteBufIOUtil.readBoolean(broadcast);
            if (isUserSend) {
                final String sender = ByteBufIOUtil.readUTF(broadcast);
                final String message = ByteBufIOUtil.readUTF(broadcast);
                return UnionPair.fail(Pair.ImmutablePair.makeImmutablePair(sender, message));
            }
            final Operation.Type type = Operation.valueOfType(ByteBufIOUtil.readUTF(broadcast));
            return UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(type, broadcast.retain()));
        } finally {
            broadcast.release();
        }
    }

    public static boolean resetConfiguration(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull ServerConfiguration configuration) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ResetConfiguration, token);
        ServerConfiguration.dump(configuration, new ByteBufOutputStream(send));
        OperateHelper.logOperating(Operation.Type.ResetConfiguration, token, p -> p.add("configuration", configuration));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(Operation.Type.ResetConfiguration, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }
}
