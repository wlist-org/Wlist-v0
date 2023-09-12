package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Exceptions.InvalidTokenException;
import com.xuxiaocheng.WList.Client.Exceptions.NoPermissionException;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
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

    static @Nullable String handleState(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        if (receive.readableBytes() <= 0) throw new WrongStateException(ResponseState.Undefined, receive.toString());
        if (receive.getByte(receive.readerIndex()) <= 1) throw new WrongStateException(ResponseState.Success); // Prevent broadcast.
        final ResponseState state = ResponseState.of(ByteBufIOUtil.readUTF(receive));
        return switch (state) {
            case Undefined -> throw new WrongStateException(state, receive.toString());
            case ServerError, FormatError -> throw new WrongStateException(state);
            case Unsupported -> throw new UnsupportedOperationException(ByteBufIOUtil.readUTF(receive));
            case NoPermission -> {
                final int length = ByteBufIOUtil.readVariableLenInt(receive);
                final UserPermission[] permissions = new UserPermission[length];
                for (int i = 0; i < length; ++i)
                    permissions[i] = UserPermission.of(ByteBufIOUtil.readUTF(receive));
                if (length == 1 && permissions[0] == UserPermission.Undefined)
                    throw new InvalidTokenException();
                throw new NoPermissionException(permissions);
            }
            case Success -> null;
            case DataError -> ByteBufIOUtil.readUTF(receive);
        };
    }

    static @NotNull ByteBuf operate(final @NotNull OperationType type) throws IOException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(send, type.name());
        return send;
    }

    static @NotNull ByteBuf operateWithToken(final @NotNull OperationType type, final @NotNull String token) throws IOException {
        final ByteBuf send = OperateHelper.operate(type);
        ByteBufIOUtil.writeUTF(send, token);
        return send;
    }

    public static final AtomicBoolean logOperation = new AtomicBoolean(true);

    static void logOperating(final @NotNull OperationType operation, final @Nullable String token, final @Nullable Consumer<? super @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get()) {
            final ParametersMap parametersMap = ParametersMap.create();
            if (token != null)
                parametersMap.add("tokenHash", token.hashCode());
            if (parameters != null)
                parameters.accept(parametersMap);
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operating: ", operation, parametersMap);
        }
    }

    static void logOperated(final @NotNull OperationType operation, final @Nullable Consumer<? super @NotNull ParametersMap> parameters) {
        if (OperateHelper.logOperation.get()) {
            final ParametersMap parametersMap = ParametersMap.create();
            if (parameters != null)
                parameters.accept(parametersMap);
            HLog.getInstance("ClientLogger").log(HLogLevel.DEBUG, "Operated: ", operation, parametersMap);
        }
    }

    static @NotNull Consumer<@NotNull ParametersMap> logReason(final @Nullable String reason) {
        return p -> p.add("success", reason == null).optionallyAdd(reason != null, "reason", reason);
    }

    static boolean booleanOperation(final @NotNull WListClientInterface client, final @NotNull ByteBuf send, final @NotNull OperationType type) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            OperateHelper.logOperated(type, OperateHelper.logReason(reason));
            return reason == null;
        } finally {
            receive.release();
        }
    }
}
