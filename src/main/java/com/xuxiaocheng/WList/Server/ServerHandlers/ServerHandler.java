package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public final class ServerHandler {
    public static final byte defaultCipher = AesCipher.doAes | AesCipher.doGZip;

    private ServerHandler() {
        super();
    }

    public static void doActive(final ChannelId ignoredId) {
    }

    public static void doInactive(final ChannelId ignoredId) {
    }

    public static void writeMessage(final @NotNull Channel channel, final @NotNull Operation.State state, final @Nullable String message) throws ServerException {
        try {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByte(buffer, ServerHandler.defaultCipher);
            ByteBufIOUtil.writeUTF(buffer, state.name());
            if (message != null)
                ByteBufIOUtil.writeUTF(buffer, message);
            if (buffer.readableBytes() < 127) // Magic Number
                buffer.setByte(0, AesCipher.doAes/*ServerHandler.defaultCipher & (~AesCipher.doGZip)*/);
            channel.writeAndFlush(buffer);
        } catch (final IOException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doException(final @NotNull Channel channel, final @Nullable String message) {
        try {
            ServerHandler.writeMessage(channel, Operation.State.ServerError, Objects.requireNonNullElse(message, ""));
        } catch (final ServerException exception) {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
            channel.close();
        }
    }
}
