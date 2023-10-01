package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

public class OperateServerTest extends ServerWrapper {
    @DisplayName("Prepare")
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(0)
    public void login(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final Pair.ImmutablePair<String, ZonedDateTime> token = OperateSelfHelper.login(client, this.adminUsername(), this.adminPassword());
        Assumptions.assumeTrue(token != null);
        this.adminToken(token.getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(1)
    public void broadcast(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        OperateServerHelper.broadcast(client, this.adminToken(), "Hello, world!");
        final Pair.ImmutablePair<String, String> pair = OperateServerHelper.waitBroadcast(broadcast).getE();
        Assertions.assertEquals(this.adminUsername(), pair.getFirst());
        Assertions.assertEquals("Hello, world!", pair.getSecond());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void resetConfiguration(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final ServerConfiguration old = ServerConfiguration.get(); // In test, Client and Server is in the same JVM.
        final ByteBuf buf = Unpooled.wrappedBuffer("""
                forward_download_cache_count: 0
                """.getBytes(StandardCharsets.UTF_8));
        final ServerConfiguration configuration = ServerConfiguration.parse(new ByteBufInputStream(buf));
        buf.release();
        Assumptions.assumeFalse(old.equals(configuration));

        Assertions.assertTrue(OperateServerHelper.resetConfiguration(client, this.adminToken(), configuration));
        Assertions.assertNotEquals(old, ServerConfiguration.get());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(2)
    public void closeServer(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        OperateServerHelper.closeServer(client, this.adminToken());
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        pair.getSecond().release();
        Assertions.assertEquals(OperationType.CloseServer, pair.getFirst());
        WListClientManager.quicklyUninitialize(this.address());
    }
}
