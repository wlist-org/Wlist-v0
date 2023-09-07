package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.OperationHelpers.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.PooledSqlDatabase;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqliteHelper;
import com.xuxiaocheng.WList.Server.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class OperateTest {
    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        HLog.setLogCaller(true);
        HLog.LoggerCreateCore.reinitialize(n -> HLog.createInstance(n, HLogLevel.DEBUG.getLevel(), true));
    }

    public static final @NotNull File runtimeDirectory = new File("./run");
    public static final @NotNull HInitializer<SocketAddress> address = new HInitializer<>("address");
    @BeforeAll
    public static void initialize() throws IOException, SQLException, InterruptedException {
        GlobalConfiguration.initialize(new File(OperateTest.runtimeDirectory, "server.yaml"));
        final File path = new File(OperateTest.runtimeDirectory, "data.db");
        ConstantManager.quicklyInitialize(new ConstantSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        UserManager.quicklyInitialize(new UserSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        DriverManager.initialize(new File("configs"));
        WListServer.getInstance().start(GlobalConfiguration.getInstance().port());

        com.xuxiaocheng.WList.Client.GlobalConfiguration.initialize(null);
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 5212);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        OperateTest.address.initialize(address);
    }
    @AfterAll
    public static void uninitialize() {
        WListClientManager.quicklyUninitialize(OperateTest.address.getInstance());

        for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
            HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
        HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
        WListServer.CodecExecutors.shutdownGracefully();
        WListServer.ServerExecutors.shutdownGracefully();
        WListServer.IOExecutors.shutdownGracefully();
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
    }

    private final @NotNull HInitializer<WListClientInterface> client = new HInitializer<>("Client");
    @BeforeEach
    public void borrow_() throws IOException, InterruptedException {
        this.client.initialize(WListClientManager.quicklyGetClient(OperateTest.address.getInstance()));
    }
    @AfterEach
    public void return_() {
        Objects.requireNonNull(this.client.uninitialize()).close();
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class OperateSelfTest {
        private static final @NotNull HInitializer<String> username = new HInitializer<>("Username", "tester-self");
        private static final @NotNull HInitializer<String> password = new HInitializer<>("Password", "123456");
        private static final @NotNull HInitializer<String> token = new HInitializer<>("Token");

        @Test
        @Order(1)
        public void logon() throws WrongStateException, IOException, InterruptedException {
            Assertions.assertTrue(OperateSelfHelper.logon(OperateTest.this.client.getInstance(), OperateSelfTest.username.getInstance(), OperateSelfTest.password.getInstance()));
        }

        @Test
        @Order(2)
        public void login() throws WrongStateException, IOException, InterruptedException {
            final String token = OperateSelfHelper.login(OperateTest.this.client.getInstance(), OperateSelfTest.username.getInstance(), OperateSelfTest.password.getInstance());
            Assumptions.assumeTrue(token != null);
            OperateSelfTest.token.reinitialize(token);
        }

        @Test
        @Order(5)
        public void logoff() throws WrongStateException, IOException, InterruptedException {
            Assertions.assertTrue(OperateSelfHelper.logoff(OperateTest.this.client.getInstance(), OperateSelfTest.token.getInstance(), OperateSelfTest.password.getInstance()));
        }

        @Test
        @Order(4)
        public void changeUsername() throws WrongStateException, IOException, InterruptedException {
            final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
            Assertions.assertTrue(OperateSelfHelper.changeUsername(OperateTest.this.client.getInstance(), OperateSelfTest.token.getInstance(), username));
            OperateSelfTest.username.reinitialize(username);
        }

        @Test
        @Order(3)
        public void changePassword() throws WrongStateException, IOException, InterruptedException {
            final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
            Assertions.assertTrue(OperateSelfHelper.changePassword(OperateTest.this.client.getInstance(), OperateSelfTest.token.getInstance(), OperateSelfTest.password.getInstance(), password));
            OperateSelfTest.password.reinitialize(password);
            // refresh token
            this.login();
        }

        @Test
        @Order(4)
        public void getPermissions() {
        }
    }
}
