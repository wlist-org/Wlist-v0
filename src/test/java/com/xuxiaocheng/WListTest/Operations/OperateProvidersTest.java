package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OperateProvidersTest extends ServerWrapper {
    @DisplayName("Prepare")
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(0)
    public void login(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final Pair.ImmutablePair<String, ZonedDateTime> token = OperateSelfHelper.login(client, this.adminUsername(), this.adminPassword());
        Assumptions.assumeTrue(token != null);
        this.adminToken(token.getFirst());
        this.providerConfiguration().load(Map.of(), List.of());
    }

    protected static final @NotNull HInitializer<String> ProviderName = new HInitializer<>("ProviderName", "test");
    public @NotNull String providerName() {
        return OperateProvidersTest.ProviderName.getInstance();
    }
    protected static final @NotNull HInitializer<StorageTypes<LanzouConfiguration>> ProviderType = new HInitializer<>("ProviderType", StorageTypes.Lanzou);
    public @NotNull StorageTypes<LanzouConfiguration> providerType() {
        return OperateProvidersTest.ProviderType.getInstance();
    }
    protected static final @NotNull HInitializer<LanzouConfiguration> Configuration = new HInitializer<>("StorageConfiguration", StorageTypes.Lanzou.configuration().get());
    public @NotNull LanzouConfiguration providerConfiguration() {
        return OperateProvidersTest.Configuration.getInstance();
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(1)
    public void addProvider(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        OperateProvidersHelper.addProvider(client, this.adminToken(), this.providerName(), this.providerType(), this.providerConfiguration());
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.AddProvider, pair.getFirst());
            Assertions.assertEquals(this.providerName(), ByteBufIOUtil.readUTF(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
        Assertions.assertEquals(this.providerName(), Objects.requireNonNull(StorageManager.getProvider(this.providerName())).getConfiguration().getName());
        Assertions.assertTrue(StorageManager.getStorageConfigurationFile(this.providerName()).isFile());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(2)
    public void removeProvider(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        OperateProvidersHelper.removeProvider(client, this.adminToken(), this.providerName(), true);
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.RemoveProvider, pair.getFirst());
            Assertions.assertEquals(this.providerName(), ByteBufIOUtil.readUTF(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
        Assertions.assertNull(StorageManager.getProvider(this.providerName()));
    }
}
