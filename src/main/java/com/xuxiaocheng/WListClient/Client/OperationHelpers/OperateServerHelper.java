package com.xuxiaocheng.WListClient.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WListClient.Client.WListClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class OperateServerHelper {
    private OperateServerHelper() {
        super();
    }

    public static boolean closeServer(final @NotNull WListClient client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CloseServer, token);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static Pair.@NotNull ImmutablePair<@NotNull String, @NotNull ByteBuf> waitBroadcast(final @NotNull WListClient client) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf broadcast = client.send(null);
        OperateHelper.handleBroadcastState(broadcast);
        return Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readUTF(broadcast), broadcast);
    }

    public static void broadcast(final @NotNull WListClient client, final @NotNull String token, final @NotNull ByteBuf message) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf prefix = OperateHelper.operateWithToken(Operation.Type.Broadcast, token);
        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, message);
        final ByteBuf receive = client.send(send);
        try {
            if (!OperateHelper.handleState(receive))
                throw new WrongStateException(Operation.State.DataError, receive.toString());
        } finally {
            receive.release();
        }
    }

    public static void setBroadcastMode(final @NotNull WListClient client, final boolean allow) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(Operation.Type.SetBroadcastMode);
        ByteBufIOUtil.writeBoolean(send, allow);
        final ByteBuf receive = client.send(send);
        try {
            if (!OperateHelper.handleState(receive))
                throw new WrongStateException(Operation.State.DataError, receive.toString());
        } finally {
            receive.release();
        }
    }
}
