package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String @NotNull [] args) throws Exception {
//        if (true) return;
        WListTest.wrapServerInitialize(() -> {
            final DriverInterface<?> driver = Objects.requireNonNull(DriverManager.get("123pan_136"));
//            HLog.DefaultLogger.log("",
//                    DriverManager_123pan.getFileInformation((DriverConfiguration_123Pan) driver.getConfiguration(), 2345490, null, null)
//            );
//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
            return null;
        });
    }


    @SuppressWarnings("OverlyBroadThrowsClause")
    private static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        GlobalConfiguration.initialize(new File("server.yaml"));
        DatabaseUtil.initialize(new File("data.db"));
        ConstantManager.sqlInstance.initialize(new ConstantSqlHelper(DatabaseUtil.getInstance(), "initialize"));
        UserGroupManager.sqlInstance.initialize(new UserGroupSqlHelper(DatabaseUtil.getInstance(), "initialize"));
        UserManager.sqlInstance.initialize(new UserSqlHelper(DatabaseUtil.getInstance(), "initialize"));
        DriverManager.initialize(new File("configs"));
        try {
            final Object obj = runnable.get();
            if (obj != null)
                HLog.DefaultLogger.log("", obj);
        } finally {
            if (GlobalConfiguration.getInstance().dumpConfiguration())
                for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
                    WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> DriverManager.dumpConfiguration(driver.getSecond().getConfiguration())));
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        }
    }
}
