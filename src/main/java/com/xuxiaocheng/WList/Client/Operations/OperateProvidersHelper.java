package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateProvidersHandler
 */
public final class OperateProvidersHelper {
    private OperateProvidersHelper() {
        super();
    }

    public static <C extends StorageConfiguration> @Nullable @Unmodifiable List<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> addProvider(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String name, final @NotNull StorageTypes<C> type, final @NotNull C configuration) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.AddProvider, token);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeUTF(send, type.identifier());
        YamlHelper.dumpYaml(configuration.dump(), new ByteBufOutputStream(send));
        OperateHelper.logOperating(OperationType.AddProvider, token, p -> p.add("name", name).add("type", type).add("configuration", configuration));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.AddProvider, null, null);
                return null;
            }
            if ("Configuration".equals(reason)) {
                final int length = ByteBufIOUtil.readVariableLenInt(receive);
                final List<Pair.ImmutablePair<String, String>> errors = new ArrayList<>(length);
                for (int i = 0; i < length; ++i)
                    errors.add(Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readUTF(receive), ByteBufIOUtil.readUTF(receive)));
                OperateHelper.logOperated(OperationType.AddProvider, "Configuration", p -> p.add("errors", errors));
                return errors;
            }
            OperateHelper.logOperated(OperationType.AddProvider, reason, null);
            return List.of();
        } finally {
            receive.release();
        }
    }

    public static void removeProvider(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String name, final boolean dropIndex) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RemoveProvider, token);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeBoolean(send, dropIndex);
        OperateHelper.logOperating(OperationType.RemoveProvider, token, p -> p.add("name", name).add("dropIndex", dropIndex));
        OperateHelper.booleanOperation(client, send, OperationType.RemoveProvider);
    }
}
