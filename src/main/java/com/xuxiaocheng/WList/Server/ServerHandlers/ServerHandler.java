package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageCiphers;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;

@FunctionalInterface
public interface ServerHandler {
    @NotNull MessageProto handle(final @NotNull ByteBuf buffer) throws IOException, ServerException;

    byte defaultCipher = MessageCiphers.doAes | MessageCiphers.doGZip;
    byte defaultFileCipher = MessageCiphers.defaultDoGZip;

    @NotNull MessageProto Success = ServerHandler.composeMessage(Operation.State.Success, null);
    @NotNull MessageProto DataError = ServerHandler.composeMessage(Operation.State.DataError, null);
    @NotNull MessageProto WrongParameters = ServerHandler.composeMessage(Operation.State.DataError, "Parameters");
    @NotNull MessageProto NoPermission = ServerHandler.composeMessage(Operation.State.NoPermission, null);
    @NotNull Function<@NotNull UnsupportedOperationException, @NotNull MessageProto> Unsupported = e ->
            ServerHandler.composeMessage(Operation.State.Unsupported, e.getMessage());

    static @NotNull MessageProto composeMessage(final @NotNull Operation.State state, final @Nullable String message) {
        return new MessageProto(ServerHandler.defaultCipher, state, buf -> {
            if (message != null)
                ByteBufIOUtil.writeUTF(buf, message);
            return buf;
        });
    }

    static @NotNull MessageProto successMessage(final @NotNull MessageProto.Appender appender) {
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, appender);
    }
}
