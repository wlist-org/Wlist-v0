package com.xuxiaocheng.WList.Client.OperationHelpers;

public final class OperateServerHelper {
    private OperateServerHelper() {
        super();
    }

//    public static boolean closeServer(final @NotNull WListClientInterface client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CloseServer, token);
//        OperateHelper.logOperating(Operation.Type.CloseServer, () -> ParametersMap.create().add("tokenHash", token.hashCode()));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(Operation.Type.CloseServer, () -> ParametersMap.create().add("success", success));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static Pair.@NotNull ImmutablePair<@NotNull String, @NotNull ByteBuf> waitBroadcast(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf broadcast = client.send(null);
//        OperateHelper.handleBroadcastState(broadcast);
//        return Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readUTF(broadcast), broadcast);
//    }
//
//    // TODO: broadcast
//    public static void broadcast(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull ByteBuf message) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf prefix = OperateHelper.operateWithToken(Operation.Type.Broadcast, token);
//        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, message);
//        final ByteBuf receive = client.send(send);
//        try {
//            if (!OperateHelper.handleState(receive))
//                throw new WrongStateException(Operation.State.DataError, receive.toString());
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static void setBroadcastMode(final @NotNull WListClientInterface client, final boolean allow) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operate(Operation.Type.SetBroadcastMode);
//        ByteBufIOUtil.writeBoolean(send, allow);
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive))
//                return;
//            throw new WrongStateException(Operation.State.DataError, ByteBufIOUtil.readUTF(receive));
//        } finally {
//            receive.release();
//        }
//    }
}
