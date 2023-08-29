package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public final class WListTest {
    private WListTest() {
        super();
    }

    private static final boolean initializeServer = true;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {
        final UnionPair<FileSqlInformation, FailureReason> directory = RootDriver.getInstance().createDirectory(new FileLocation("test", -1), "a", Options.DuplicatePolicy.ERROR);
        if (directory.isFailure()) return directory;
        final UnionPair<FileSqlInformation, FailureReason> file = RootDriver.getInstance().copy(new FileLocation("test", 131325697), new FileLocation("test", directory.getT().id()), "0.txt", Options.DuplicatePolicy.ERROR);
        if (file.isFailure()) return file;
        return RootDriver.getInstance().list(new FileLocation("test", directory.getT().id()), Options.DirectoriesOrFiles.Both, 20, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND);
//        return null;
    };

    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.listenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static @Nullable Object wrapServerInitialize() throws Exception {
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
                return null;
            }
            return WListTest._main.get();
        } finally {
            for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
        }
    }

    public static void main(final String @NotNull [] args) {
        try {
            final Object obj = WListTest.initializeServer ? WListTest.wrapServerInitialize() : WListTest._main.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        } finally {
            HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
        }
    }
}
