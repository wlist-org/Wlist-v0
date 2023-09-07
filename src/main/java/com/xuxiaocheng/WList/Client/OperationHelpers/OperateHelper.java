package com.xuxiaocheng.WList.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Exceptions.NoPermissionException;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class OperateHelper {
    private OperateHelper() {
        super();
    }

    static boolean handleState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
        return switch (state) {
            case Undefined, Broadcast -> throw new WrongStateException(state, receive.toString());
            case ServerError, FormatError -> throw new WrongStateException(state);
            case Unsupported -> throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
            case NoPermission -> {
                final int length = ByteBufIOUtil.readVariableLenInt(receive);
                final Operation.Permission[] permissions = new Operation.Permission[length];
                for (int i = 0; i < length; i++)
                    permissions[i] = Operation.valueOfPermission(ByteBufIOUtil.readUTF(receive));
                throw new NoPermissionException(permissions);
            }
            case Success -> true;
            case DataError -> false;
        };
    }

//    static void handleBroadcastState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
//        final byte ignoredCipher = ByteBufIOUtil.readByte(receive);
//        final Operation.State state = Operation.valueOfState(ByteBufIOUtil.readUTF(receive));
//        switch (state) {
//            case Undefined, Success, DataError -> throw new WrongStateException(state, receive.toString());
//            case ServerError, FormatError -> throw new WrongStateException(state);
//            case Unsupported -> throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
//            case NoPermission -> throw new NoPermissionException(Operation.valueOfPermission(ByteBufIOUtil.readUTF(receive)));
//            case Broadcast -> {}
//        }
//    }

    static @NotNull ByteBuf operate(final Operation.@NotNull Type type) throws IOException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(send, type.name());
        return send;
    }

    static @NotNull ByteBuf operateWithToken(final Operation.@NotNull Type type, final @NotNull String token) throws IOException {
        final ByteBuf send = OperateHelper.operate(type);
        ByteBufIOUtil.writeUTF(send, token);
        return send;
    }

    public static final AtomicBoolean logOperation = new AtomicBoolean(true);

    static void logOperating(final Operation.@NotNull Type operation, final @Nullable String token, final @Nullable Consumer<? super @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get()) {
            final ParametersMap parametersMap = ParametersMap.create();
            if (token != null)
                parametersMap.add("tokenHash", token.hashCode());
            if (parameters != null)
                parameters.accept(parametersMap);
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operating: ", operation, parametersMap);
        }
    }

    static void logOperated(final Operation.@NotNull Type operation, final @Nullable Consumer<? super @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get()) {
            final ParametersMap parametersMap = ParametersMap.create();
            if (parameters != null)
                parameters.accept(parametersMap);
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operated: ", operation, parametersMap);
        }
    }
}
