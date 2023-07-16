package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import org.jetbrains.annotations.NotNull;

public final class ServerDriverHandler {
    private ServerDriverHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.BuildIndex, ServerDriverHandler.doBuildIndex);
    }

    public static final @NotNull ServerHandler doBuildIndex = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesBuildIndex);
        final String driver = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.BuildIndex, user, () -> ParametersMap.create()
                .add("driver", driver));
        if (user.isFailure())
            return user.getE();
        final boolean success;
        try {
            success = RootDriver.getInstance().buildIndex(driver);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        return success ? ServerHandler.Success : ServerHandler.DataError;
    };
}
