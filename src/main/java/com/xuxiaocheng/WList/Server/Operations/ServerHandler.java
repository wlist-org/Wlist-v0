package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@FunctionalInterface
public interface ServerHandler {
    /**
     * Call {@link com.xuxiaocheng.WList.Server.WListServer.ServerChannelHandler#write(Channel, MessageProto)} to response message.
     * @return null: Invalid token/parameters etc. !null: Real handler.
     */
    @Nullable RunnableE extra(final @NotNull Channel channel, final @NotNull ByteBuf buffer) throws IOException, SQLException;

    @Contract(pure = true)
    static @NotNull String user(final @Nullable String user, final @NotNull UserInformation information) {
        return String.format(" %s: (id=%d, name='%s')", Objects.requireNonNullElse(user, "user"), information.id(), information.username());
    }

    @Contract(pure = true)
    static @NotNull String userGroup(final @Nullable String group, final @NotNull UserGroupInformation information) {
        return String.format(" %s: (id=%d, name='%s')", Objects.requireNonNullElse(group, "group"), information.id(), information.name());
    }

    AtomicBoolean LogActive = new AtomicBoolean(false);
    AtomicBoolean LogOperation = new AtomicBoolean(true);

    static void logOperation(final @NotNull Channel channel, final @NotNull OperationType operation, final @Nullable UnionPair<UserInformation, MessageProto> user, final @Nullable Supplier<? extends @NotNull ParametersMap> parameters) {
        if (ServerHandler.LogOperation.get() && HLog.getInstance("ServerLogger").getLevel() < HLogLevel.DEBUG.getLevel())
            HLog.getInstance("ServerLogger").log(HLogLevel.DEBUG, "Operate: ", channel.remoteAddress(), ", type: ", operation,
                    (Supplier<String>) () -> user == null ? "" : user.isSuccess() ?
                            ServerHandler.user(null, user.getT()) : ". Refused because " + user.getE().state(), ".",
                    (Supplier<String>) () -> parameters == null ? "" : parameters.get().toString());
    }
}
