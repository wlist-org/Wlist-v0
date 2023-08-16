package com.xuxiaocheng.WListClient.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Server.MessageCiphers;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Supplier;

public final class OperateHelper {
    private OperateHelper() {
        super();
    }

    static boolean handleState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        final byte ignoredCipher = ByteBufIOUtil.readByte(receive);
        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
        if (state == Operation.State.Undefined)
            throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
        if (state == Operation.State.Broadcast)
            throw new WrongStateException(Operation.State.Broadcast, receive.toString());
        if (state == Operation.State.ServerError)
            throw new WrongStateException(Operation.State.ServerError);
        if (state == Operation.State.NoPermission)
            throw new NoPermissionException();
        if (state == Operation.State.Unsupported)
            throw new WrongStateException(Operation.State.Unsupported, ByteBufIOUtil.readUTF(receive));
        return state == Operation.State.Success;
    }

    static void handleBroadcastState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        final byte ignoredCipher = ByteBufIOUtil.readByte(receive);
        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
        if (state == Operation.State.Undefined)
            throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
        if (state == Operation.State.ServerError || state == Operation.State.Unsupported)
            throw new WrongStateException(state, ByteBufIOUtil.readUTF(receive));
        if (state != Operation.State.Broadcast)
            throw new WrongStateException(state, receive.toString());
    }

    static @NotNull ByteBuf operate(final Operation.@NotNull Type type) throws IOException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, MessageCiphers.defaultCipher);
        ByteBufIOUtil.writeUTF(send, type.name());
        return send;
    }

    static @NotNull ByteBuf operateWithToken(final Operation.@NotNull Type type, final @NotNull String token) throws IOException {
        final ByteBuf send = OperateHelper.operate(type);
        ByteBufIOUtil.writeUTF(send, token);
        return send;
    }

    // TODO
    static void logOperation(final Operation.@NotNull Type operation, final @Nullable Supplier<? extends @NotNull ParametersMap> parameters) {
        HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operate: ", operation,
                (Supplier<String>) () -> parameters == null ? "" : parameters.get().toString());
    }
}
