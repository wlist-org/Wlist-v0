package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public final class WListTest {
    private WListTest() {
        super();
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String @NotNull [] args) throws Exception {
//        if (true) return;
        WListTest.wrapServerInitialize(() -> {
//            final Driver_lanzou lanzou = (Driver_lanzou) Objects.requireNonNull(DriverManager.getDriver("test"));

        });
    }

    @SuppressWarnings({"unused", "PublicField"})
    public static @Nullable Object tempPlaceForDebug;
    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener("listener", (t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        GlobalConfiguration.initialize(new File("server.yaml"));
//        GlobalConfiguration.initialize(null);
        PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
        ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        DriverManager.initialize(new File("configs"));
        try {
            final Map<String, Exception> failures = DriverManager.getFailedDriversAPI();
            if (!failures.isEmpty()) {
                HLog.DefaultLogger.log(HLogLevel.FAULT, failures);
                return;
            }
            final Object obj = runnable.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
        }
    }

    private static void wrapServerInitialize(final @NotNull RunnableE runnable) throws Exception {
        WListTest.wrapServerInitialize(() -> {
            runnable.run();
            return null;
        });
    }
}
