package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Server.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Server.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class TempTest {
    private static final boolean initializeServer = false;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {

        return null;
    };

    @BeforeAll
    public static void initialize() throws IOException, SQLException {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        if (TempTest.initializeServer) {
            GlobalConfiguration.initialize(new File("server.yaml"));
//            GlobalConfiguration.initialize(null);
            PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
            ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
            UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
            UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
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
