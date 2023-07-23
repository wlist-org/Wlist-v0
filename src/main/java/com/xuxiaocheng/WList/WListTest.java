package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
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
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

public final class WListTest {
    private WListTest() {
        super();
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String @NotNull [] args) throws Exception {
        PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
        try (final Connection connection = PooledDatabase.instance.getInstance().getConnection(null, null)) {
            HLog.DefaultLogger.log("", connection.isClosed());
            HLog.DefaultLogger.log("", connection.isValid(0));
            PooledDatabase.quicklyUninitialize();
            HLog.DefaultLogger.log("", connection.isClosed());
            HLog.DefaultLogger.log("", connection.isValid(0));
        }
        if (true) return;
        WListTest.wrapServerInitialize(() -> {
            final DriverInterface<?> driver = Objects.requireNonNull(DriverManager.getDriver("123pan_136"));
//            HLog.DefaultLogger.log("",
//                    DriverManager_123pan.getFileInformation((DriverConfiguration_123Pan) driver.getConfiguration(), 2345490, null, null)
//            );
//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
            return null;
        });
    }

    static {
        HUncaughtExceptionHelper.putIfAbsentUncaughtExceptionListener("listener", (t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        GlobalConfiguration.initialize(null);
        PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
        ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        DriverManager.initialize(new File("configs"));
        try {
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
        }
    }
}
