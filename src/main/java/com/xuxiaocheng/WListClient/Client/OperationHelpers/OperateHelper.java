package com.xuxiaocheng.WListClient.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.Exceptions.NoPermissionException;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Server.MessageCiphers;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class OperateHelper {
    private OperateHelper() {
        super();
    }

    static boolean handleState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        final byte ignoredCipher = ByteBufIOUtil.readByte(receive);
        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
        return switch (state) {
            case Undefined, Broadcast -> throw new WrongStateException(state, receive.toString());
            case ServerError, FormatError -> throw new WrongStateException(state);
            case Unsupported -> throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
            case NoPermission -> throw new NoPermissionException();
            case Success -> true;
            case DataError -> false;
        };
    }

    static void handleBroadcastState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        final byte ignoredCipher = ByteBufIOUtil.readByte(receive);
        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
        switch (state) {
            case Undefined, Success, DataError -> throw new WrongStateException(state, receive.toString());
            case ServerError, FormatError -> throw new WrongStateException(state);
            case Unsupported -> throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
            case NoPermission -> throw new NoPermissionException();
            case Broadcast -> {}
        }
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

    public static final AtomicBoolean logOperation = new AtomicBoolean(true);

    static void logOperating(final Operation.@NotNull Type operation, final @Nullable Supplier<? extends @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get())
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operating: ", operation,
                    (Supplier<String>) () -> parameters == null ? "" : parameters.get().toString());
    }

    static void logOperated(final Operation.@NotNull Type operation, final @Nullable Supplier<? extends @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get())
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operated: ", operation,
                    (Supplier<String>) () -> parameters == null ? "" : parameters.get().toString());
    }
}
