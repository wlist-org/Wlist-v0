package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateProvidersHandler
 */
public final class OperateProvidersHelper {
    private OperateProvidersHelper() {
        super();
    }

    public static <C extends StorageConfiguration> void addProvider(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String name, final @NotNull StorageTypes<C> type, final @NotNull C configuration) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.AddProvider, token);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeUTF(send, type.getIdentifier());
        YamlHelper.dumpYaml(configuration.dump(), new ByteBufOutputStream(send));
        OperateHelper.logOperating(OperationType.AddProvider, token, p -> p.add("name", name).add("type", type).add("configuration", configuration));
        OperateHelper.booleanOperation(client, send, OperationType.AddProvider);
    }

    public static void removeProvider(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String name, final boolean dropIndex) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RemoveProvider, token);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeBoolean(send, dropIndex);
        OperateHelper.logOperating(OperationType.RemoveProvider, token, p -> p.add("name", name).add("dropIndex", dropIndex));
        OperateHelper.booleanOperation(client, send, OperationType.RemoveProvider);
    }
}
