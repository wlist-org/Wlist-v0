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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ServerHandler {
    @NotNull MessageProto handle(final @NotNull Channel channel, final @NotNull ByteBuf buffer) throws IOException, ServerException;

    byte defaultCipher = MessageCiphers.doAes | MessageCiphers.doGZip;
    byte defaultFileCipher = MessageCiphers.doGZip;

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

    static void logOperation(final @NotNull Channel channel, final Operation.@NotNull Type operation, final @Nullable UnionPair<@NotNull UserSqlInformation, @NotNull MessageProto> user, final @Nullable Supplier<? extends @NotNull ParametersMap> parameters) {
        HLog.getInstance("ServerLogger").log(HLogLevel.DEBUG, "Operate: ", channel.remoteAddress(), ", type: ", operation,
                (Supplier<String>) () -> user == null ? "." :
                        user.isSuccess() ? " user: (id:" + user.getT().id() + ") '" + user.getT().username() + "'." : ". Refused because " + user.getE().state() + '.',
                (Supplier<String>) () -> parameters == null ? "" : parameters.get().toString());
    }
}
