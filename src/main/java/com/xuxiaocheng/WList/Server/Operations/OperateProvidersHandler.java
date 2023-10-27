package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OperateProvidersHandler {
    private OperateProvidersHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.AddProvider, OperateProvidersHandler.doAddProvider);
        ServerHandlerManager.register(OperationType.RemoveProvider, OperateProvidersHandler.doRemoveProvider);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper#addProvider(WListClientInterface, String, String, StorageTypes, StorageConfiguration)
     */
    private static final @NotNull ServerHandler doAddProvider = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> operator = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate, UserPermission.ProvidersOperate);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final String identifier = ByteBufIOUtil.readUTF(buffer);
        final String reason = StorageManager.providerNameInvalidReason(name);
        final StorageTypes<?> type = StorageTypes.get(identifier);
        final Map<String, Object> config;
        if (operator.isSuccess() && reason == null && type != null) {
            try (final InputStream inputStream = new ByteBufInputStream(buffer)) {
                config = YamlHelper.loadYaml(inputStream);
            }
        } else config = null;
        ServerHandler.logOperation(channel, OperationType.AddProvider, operator, () -> ParametersMap.create()
                .add("name", name).add("identifier", identifier).add("type", type).add("config", config).optionallyAdd(reason != null, "failure", reason)
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
        assert config != null;
        return () -> {
            final List<Pair.ImmutablePair<String, String>> errors = StorageManager.addStorage(name, type, config);
            if (errors != null) {
                WListServer.ServerChannelHandler.write(channel, new MessageProto(ResponseState.DataError, buf -> {
                    ByteBufIOUtil.writeUTF(buf, "Configuration");
                    ByteBufIOUtil.writeVariableLenInt(buf, errors.size());
                    for (final Pair.ImmutablePair<String, String> error: errors) {
                        ByteBufIOUtil.writeUTF(buf, error.getFirst());
                        ByteBufIOUtil.writeUTF(buf, error.getSecond());
                    }
                    return buf;
                }));
                return;
            }
            BroadcastManager.onProviderInitialized(name, Objects.requireNonNull(StorageManager.getProvider(name)).getConfiguration().getRootDirectoryId());
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper#removeProvider(WListClientInterface, String, String, boolean)
     */
    private static final @NotNull ServerHandler doRemoveProvider = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> operator = OperateSelfHandler.checkToken(token, UserPermission.ServerOperate, UserPermission.ProvidersOperate);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean dropIndex = ByteBufIOUtil.readBoolean(buffer);
        final boolean existed = ServerConfiguration.get().providers().containsKey(name);
        ServerHandler.logOperation(channel, OperationType.RemoveProvider, operator, () -> ParametersMap.create()
                .add("name", name).add("dropIndex", dropIndex).add("existed", existed));
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
            if (StorageManager.removeStorage(name, dropIndex))
                BroadcastManager.onProviderUninitialized(name);
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };
}
