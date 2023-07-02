package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageCiphers;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ServerHandler {
    @NotNull MessageProto handle(final @NotNull Channel channel, final @NotNull ByteBuf buffer) throws IOException, ServerException;

    byte defaultCipher = MessageCiphers.doAes | MessageCiphers.doGZip;
    byte defaultFileCipher = MessageCiphers.defaultDoGZip;

    static @NotNull MessageProto composeMessage(final Operation.@NotNull State state, final @Nullable String message) {
        return new MessageProto(ServerHandler.defaultCipher, state, buf -> {
            if (message != null)
                ByteBufIOUtil.writeUTF(buf, message);
            return buf;
        });
    }

    static @NotNull MessageProto successMessage(final MessageProto.@NotNull Appender appender) {
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, appender);
    }

    @NotNull MessageProto Success = ServerHandler.composeMessage(Operation.State.Success, null);
    @NotNull MessageProto DataError = ServerHandler.composeMessage(Operation.State.DataError, null);
    @NotNull MessageProto WrongParameters = ServerHandler.composeMessage(Operation.State.DataError, "Parameters");
    @NotNull MessageProto NoPermission = ServerHandler.composeMessage(Operation.State.NoPermission, null);
    @NotNull Function<@NotNull UnsupportedOperationException, @NotNull MessageProto> Unsupported = e ->
            ServerHandler.composeMessage(Operation.State.Unsupported, e.getMessage());

    static void logOperation(final @NotNull ChannelId channelId, final Operation.@NotNull Type operation, final @Nullable UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> user, final @Nullable Supplier<@NotNull ParametersMap<@NotNull String, @Nullable Object>> parameters) {
        HLog.getInstance("ServerLogger").log(HLogLevel.DEBUG, "Operate: ", channelId.asLongText(), ", type: ", operation,
                (Supplier<String>) () -> user == null ? "." :
                        user.isSuccess() ? " user: (" + user.getT().id() + ")'" + user.getT().username() + "'." : ". Refused because " + user.getE().state() + '.',
                (Supplier<String>) () -> {
                    if (parameters == null) return "";
                    final ParametersMap<String, Object> p = parameters.get();
                    if (p.isEmpty()) return "";
                    final StringBuilder builder = new StringBuilder(" (");
                    for (final Map.Entry<String, Object> entry: p.entrySet())
                        builder.append(entry.getKey()).append('=').append(HLog.expandString(entry.getValue())).append(", ");
                    return builder.delete(builder.length() - 2, builder.length()).append(')').toString();
                });
    }
}
