package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperateServerTest extends ServerWrapper {
    protected static final @NotNull HInitializer<String> username = new HInitializer<>("Username", "admin");
    protected static final @NotNull HInitializer<String> password = new HInitializer<>("Password", "");
    protected static final @NotNull HInitializer<String> token = new HInitializer<>("Token");

//    @Test
//    @Order(0)
//    @DisplayName("Prepare")
//    public void login() throws WrongStateException, IOException, InterruptedException {
//        final String token = OperateSelfHelper.login(this.client.getInstance(), OperateServerTest.username.getInstance(), OperateServerTest.password.getInstance());
//        Assumptions.assumeTrue(token != null);
//        OperateServerTest.token.initialize(token);
//    }
//
//    @Test
//    @Order(1)
//    public void broadcast() throws WrongStateException, IOException, InterruptedException {
//        try (final WListClientInterface client = WListClientManager.quicklyGetClient(ServerWrapper.address.getInstance())) {
//            OperateServerHelper.setBroadcastMode(client, true);
//            OperateServerHelper.broadcast(this.client.getInstance(), OperateServerTest.token.getInstance(), "Hello, world!");
//            Assertions.assertEquals("Hello, world!", OperateServerHelper.waitBroadcast(client).getE().getSecond());
//        }
//    }
//
//    @Test
//    @Order(1)
//    public void resetConfiguration() throws WrongStateException, IOException, InterruptedException {
//        final ServerConfiguration old = ServerConfiguration.get(); // In test, Client and Server is in the same JVM.
//        final ServerConfiguration configuration = ServerConfiguration.parse(new ByteBufInputStream(Unpooled.wrappedBuffer("""
//                forward_download_cache_count: 0
//                """.getBytes(StandardCharsets.UTF_8))));
//        Assumptions.assumeFalse(old.equals(configuration));
//        Assertions.assertTrue(OperateServerHelper.resetConfiguration(this.client.getInstance(), OperateServerTest.token.getInstance(), configuration));
//        Assertions.assertNotEquals(old, ServerConfiguration.get());
//
//        ServerConfiguration.set(old);
//    }
//
//    @Test
//    @Order(2)
//    public void closeServer() throws WrongStateException, IOException, InterruptedException {
//        try (final WListClientInterface client = WListClientManager.quicklyGetClient(ServerWrapper.address.getInstance())) {
//            OperateServerHelper.setBroadcastMode(client, true);
//            OperateServerHelper.closeServer(this.client.getInstance(), OperateServerTest.token.getInstance());
//            final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(client).getT();
//            pair.getSecond().release();
//            Assertions.assertEquals(OperationType.CloseServer, pair.getFirst());
//        } catch (final IOException exception) {
//            if (!"Closed client.".equals(exception.getMessage()))
//                throw exception;
//        }
//        WListClientManager.quicklyUninitialize(ServerWrapper.address.getInstance());
//    }
}
