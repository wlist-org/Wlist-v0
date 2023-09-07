package com.xuxiaocheng.WList.Server.Handlers;

public final class ServerDriverHandler {
    private ServerDriverHandler() {
        super();
    }

    public static void initialize() {
//        ServerHandlerManager.register(Operation.Type.BuildIndex, ServerDriverHandler.doBuildIndex);
    }

//    public static final @NotNull ServerHandler doBuildIndex = (channel, buffer) -> {
//        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesBuildIndex);
//        final String driver = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.BuildIndex, user, () -> ParametersMap.create()
//                .add("driver", driver));
//        if (user.isFailure())
//            return user.getE();
//        final boolean success;
//        try {
//            success = RootDriver.getInstance().buildIndex(driver);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        return success ? MessageProto.Success : MessageProto.DataError;
//    };
}
