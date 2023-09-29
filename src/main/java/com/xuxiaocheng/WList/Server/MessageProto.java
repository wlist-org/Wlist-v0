package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public record MessageProto(@NotNull ResponseState state, @Nullable Appender appender) {
    @FunctionalInterface
    public interface Appender {
        @NotNull ByteBuf apply(final @NotNull ByteBuf buffer) throws IOException;
    }

    public static @NotNull MessageProto composeMessage(final @NotNull ResponseState state, final @Nullable String message) {
        return new MessageProto(state, message == null ? null : buf -> {
            ByteBufIOUtil.writeUTF(buf, message);
            return buf;
        });
    }

    public static final @NotNull MessageProto Success = MessageProto.composeMessage(ResponseState.Success, null);
    public static final @NotNull MessageProto WrongParameters = MessageProto.composeMessage(ResponseState.DataError, "Parameters");

    public static final @NotNull MessageProto Undefined = MessageProto.composeMessage(ResponseState.Unsupported, I18NUtil.get("server.operation.undefined_operation"));
    public static final @NotNull MessageProto ServerError = MessageProto.composeMessage(ResponseState.ServerError, null);
    public static final @NotNull MessageProto FormatError = MessageProto.composeMessage(ResponseState.FormatError, null);

    public static @NotNull MessageProto successMessage(final @NotNull Appender appender) {
        return new MessageProto(ResponseState.Success, appender);
    }
}
