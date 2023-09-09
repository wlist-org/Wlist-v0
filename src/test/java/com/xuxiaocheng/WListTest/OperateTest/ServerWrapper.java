package com.xuxiaocheng.WListTest.OperateTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.PooledSqlDatabase;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqliteHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

@Execution(ExecutionMode.CONCURRENT)
public class ServerWrapper {
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
        ServerConfiguration.Location.initialize(new File(ServerWrapper.runtimeDirectory, "server.yaml"));
        ServerConfiguration.parseFromFile();
        final File path = new File(ServerWrapper.runtimeDirectory, "data.db");
        ConstantManager.quicklyInitialize(new ConstantSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        UserManager.quicklyInitialize(new UserSqliteHelper(PooledSqlDatabase.quicklyOpen(path)), "initialize");
        DriverManager.initialize(new File("configs"));
        WListServer.getInstance().start(ServerConfiguration.get().port());
        final SocketAddress address = WListServer.getInstance().getAddress().getInstance();

        com.xuxiaocheng.WList.Client.GlobalConfiguration.initialize(null);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        ServerWrapper.address.initialize(address);
    }
    @AfterAll
    public static void uninitialize() {
        WListClientManager.quicklyUninitialize(ServerWrapper.address.getInstance());

        for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
            HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
        HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
        WListServer.CodecExecutors.shutdownGracefully();
        WListServer.ServerExecutors.shutdownGracefully();
        WListServer.IOExecutors.shutdownGracefully();
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
    }

    protected final @NotNull HInitializer<WListClientInterface> client = new HInitializer<>("Client");
    @BeforeEach
    public void borrow_() throws IOException, InterruptedException {
        this.client.initialize(WListClientManager.quicklyGetClient(ServerWrapper.address.getInstance()));
    }
    @AfterEach
    public void return_() {
        Objects.requireNonNull(this.client.uninitialize()).close();
    }

    @Override
    public String toString() {
        return "ServerWrapper{" +
                "client=" + this.client +
                '}';
    }
}
