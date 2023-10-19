package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.IdsHelper;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Execution(ExecutionMode.SAME_THREAD)
public class TempTest {
    private static final boolean initializeEnvironment = false;
    private static final boolean initializeServer = false;
    private static final boolean initializeClient = true;
    private static final @NotNull ConsumerE<SocketAddress> start = address -> {
        TimeUnit.SECONDS.sleep(1);
    };
    private static final @NotNull FunctionE<SocketAddress, @Nullable Object> _main = address -> {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicBoolean state = new AtomicBoolean(false);
        final Consumer<Pair.ImmutablePair<String, Boolean>> callback = p -> {
            try {
                synchronized (state) {
                    if (state.get() == p.getSecond().booleanValue())
                        return;/*error*/
                    state.set(p.getSecond().booleanValue());
                }
            } finally {
                latch.countDown();
            }
        };
        BroadcastAssistant.get(address).ProviderLogin.register(callback);
        BroadcastManager.onProviderLogin("test", true);
        BroadcastManager.onProviderLogin("test", false);
        latch.await();
        BroadcastAssistant.get(address).ProviderLogin.unregister(callback);
        Assertions.assertFalse(state.get());
        return null;
    };

    private static @Nullable SocketAddress address;
    private static final @NotNull File runtimeDirectory = new File("./run");
    @BeforeAll
    public static void initialize() throws Exception {
        StaticLoader.load();
        if (TempTest.initializeEnvironment || TempTest.initializeServer || TempTest.initializeClient) {
            StaticLoader.load();
            ServerConfiguration.Location.initialize(new File(TempTest.runtimeDirectory, "server.yaml"));
            ServerConfiguration.parseFromFile();
            final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(new File(TempTest.runtimeDirectory, "data.db"));
            ConstantManager.quicklyInitialize(database, "initialize");
            UserGroupManager.quicklyInitialize(database, "initialize");
            UserManager.quicklyInitialize(database, "initialize");
            StorageManager.initialize(new File(TempTest.runtimeDirectory, "configs"), new File(TempTest.runtimeDirectory, "caches"));
            Assumptions.assumeTrue(StorageManager.getFailedStoragesAPI().isEmpty(), () -> StorageManager.getFailedStoragesAPI().toString());
            if (TempTest.initializeServer || TempTest.initializeClient) {
                WListServer.getInstance().start(ServerConfiguration.get().port());
                final SocketAddress address = WListServer.getInstance().getAddress().getInstance();
                TempTest.address = address;
                if (TempTest.initializeClient) {
                    ClientConfiguration.Location.initialize(new File(TempTest.runtimeDirectory, "client.yaml"));
                    ClientConfiguration.parseFromFile();
                    WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                    Assumptions.assumeTrue(TokenAssistant.login(address, "admin", "", WListServer.IOExecutors));
                    BroadcastAssistant.start(address);
                    TimeUnit.MILLISECONDS.sleep(300);
                }
            }
        }
        TempTest.start.accept(TempTest.address);
    }

    @RepeatedTest(100)
    public void tempTest() throws Exception {
        final Object obj = TempTest._main.apply(TempTest.address);
        if (obj != null) {
            HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
            HLog.DefaultLogger.log(HLogLevel.VERBOSE, obj.getClass());
        }
    }

    @AfterAll
    public static void uninitialize() {
        if (TempTest.initializeEnvironment)
            for (final StorageConfiguration configuration: StorageManager.getAllConfigurations())
                try {
                    StorageManager.dumpConfigurationIfModified(configuration);
                } catch (final IOException exception) {
                    HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump provider configuration.", ParametersMap.create().add("name", configuration.getName()), exception);
                }
        HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
        WListServer.CodecExecutors.shutdownGracefully();
        WListServer.ServerExecutors.shutdownGracefully();
        WListServer.IOExecutors.shutdownGracefully();
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        HttpNetworkHelper.CountDownExecutors.shutdownGracefully();
        IdsHelper.CleanerExecutors.shutdownGracefully();
    }
}
