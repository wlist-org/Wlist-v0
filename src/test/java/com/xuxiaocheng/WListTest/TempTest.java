package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class TempTest {
    private static final boolean initializeServer = true;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {
        return UserGroupManager.searchGroupsByNames(Set.of("t"), 0, 5, null);
//        return null;
    };

    private static final @NotNull File runtimeDirectory = new File("./run");
    @BeforeAll
    public static void initialize() throws IOException, SQLException {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        if (TempTest.initializeServer) {
            ServerConfiguration.Location.initialize(new File(TempTest.runtimeDirectory, "server.yaml"));
            ServerConfiguration.parseFromFile();
            final File file = new File(TempTest.runtimeDirectory, "data.db");
            final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
            ConstantManager.quicklyInitialize(database, "initialize");
            UserGroupManager.quicklyInitialize(database, "initialize");
            UserManager.quicklyInitialize(database, "initialize");
            DriverManager.initialize(new File("configs"));
        }
    }

    @Test
    public void tempTest() throws Exception {
        try {
            if (TempTest.initializeServer) {
                final Map<String, Exception> failures = DriverManager.getFailedDriversAPI();
                if (!failures.isEmpty()) {
                    HLog.DefaultLogger.log(HLogLevel.FAULT, failures);
                    return;
                }
            }
            final Object obj = TempTest._main.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            if (TempTest.initializeServer)
                for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                    HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
            HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
        }
    }
}
