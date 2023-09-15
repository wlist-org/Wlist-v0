package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

public final class OperateProvidersHandler {
    private OperateProvidersHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.AddProvider, OperateProvidersHandler.doAddProvider);
        ServerHandlerManager.register(OperationType.RemoveProvider, OperateProvidersHandler.doRemoveProvider);
//        ServerHandlerManager.register(OperationType.BuildIndex, OperateProvidersHandler.doBuildIndex);
    }

    public static final @NotNull ServerHandler doAddProvider = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> operator = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate, UserPermission.ProvidersOperate);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final String identifier = ByteBufIOUtil.readUTF(buffer);
        final String reason = StorageManager.providerNameInvalidReason(name);
        final ProviderTypes type = ProviderTypes.get(identifier);
        ServerHandler.logOperation(channel, OperationType.AddProvider, operator, () -> ParametersMap.create()
                .add("name", name).add("identifier", identifier).add("type", type).optionallyAdd(reason != null, "failure", reason)
                .optionallyAdd(reason == null && type == null, "failure", "Unsupported provider type."));
        MessageProto message = null;
        if (operator.isFailure())
            message = operator.getE();
        else if (reason != null || type == null)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            StorageManager.addProvider(name, type);
            BroadcastManager.onProviderInitialized(name);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    public static final @NotNull ServerHandler doRemoveProvider = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> operator = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate, UserPermission.ProvidersOperate);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean dropCache = ByteBufIOUtil.readBoolean(buffer);
        final boolean existed = ServerConfiguration.get().providers().containsKey(name);
        ServerHandler.logOperation(channel, OperationType.RemoveProvider, operator, () -> ParametersMap.create()
                .add("name", name).add("dropCache", dropCache).add("existed", existed));
        MessageProto message = null;
        if (operator.isFailure())
            message = operator.getE();
        else if (!existed)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            StorageManager.removeProvider(name);
            BroadcastManager.onProviderUninitialized(name);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

//    public static final @NotNull ServerHandler doBuildIndex = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesBuildIndex);
//        final String driver = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, OperationType.BuildIndex, user, () -> ParametersMap.create()
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
