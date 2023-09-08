package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;

public record MessageProto(Operation.@NotNull State state, @Nullable Appender appender) {
    @FunctionalInterface
    public interface Appender {
        @NotNull ByteBuf apply(final @NotNull ByteBuf buffer) throws IOException;
    }

    public static @NotNull MessageProto composeMessage(final Operation.@NotNull State state, final @Nullable String message) {
        return new MessageProto(state, message == null ? null : buf -> {
            ByteBufIOUtil.writeUTF(buf, message);
            return buf;
        });
    }

    public static final @NotNull MessageProto Success = MessageProto.composeMessage(Operation.State.Success, null);
    public static final @NotNull MessageProto DataError = MessageProto.composeMessage(Operation.State.DataError, null);

    public static final @NotNull MessageProto WrongParameters = MessageProto.composeMessage(Operation.State.DataError, "Parameters");
    public static final @NotNull Function<@NotNull UnsupportedOperationException, @NotNull MessageProto> Unsupported = e -> MessageProto.composeMessage(Operation.State.Unsupported, e.getMessage());

    public static final @NotNull MessageProto Undefined = MessageProto.composeMessage(Operation.State.Unsupported, "Undefined operation!");
    public static final @NotNull MessageProto ServerError = MessageProto.composeMessage(Operation.State.ServerError, null);
    public static final @NotNull MessageProto FormatError = MessageProto.composeMessage(Operation.State.FormatError, null);

    public static @NotNull MessageProto successMessage(final @NotNull Appender appender) {
        return new MessageProto(Operation.State.Success, appender);
    }
}
