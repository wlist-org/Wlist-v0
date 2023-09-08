package com.xuxiaocheng.WList.Server.Handlers;

public final class OperateDriverHandler {
    private OperateDriverHandler() {
        super();
    }

    public static void initialize() {
//        ServerHandlerManager.register(Operation.Type.BuildIndex, OperateDriverHandler.doBuildIndex);
    }

//    public static final @NotNull ServerHandler doBuildIndex = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUserHandler.checkToken(buffer, Operation.Permission.FilesBuildIndex);
//        final String driver = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, Operation.Type.BuildIndex, user, () -> ParametersMap.create()
//                .add("driver", driver));
//        if (user.isFailure())
//            return user.getE();
//        final boolean success;
//        try {
//            success = RootSelector.getInstance().buildIndex(driver);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        return success ? MessageProto.Success : MessageProto.DataError;
//    };
}
