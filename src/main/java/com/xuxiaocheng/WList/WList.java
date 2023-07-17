package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
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
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandlerManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public final class WList {
    private WList() {
        super();
    }

    private static boolean DebugMode = true;
    public static boolean isDebugMode() {
        return WList.DebugMode;
    }

    private static final @NotNull AtomicInteger mainStageAPI = new AtomicInteger(-1);
    public static int getMainStageAPI() {
        return WList.mainStageAPI.get();
    }
    private static void setMainStageAPI(final int stage) {
        synchronized (WList.mainStageAPI) {
            WList.mainStageAPI.set(stage);
            WList.mainStageAPI.notifyAll();
        }
    }
    public static boolean waitMainStageAPI(final int stage) throws InterruptedException {
        int current = WList.mainStageAPI.get();
        if (current == -1)
            return false;
        synchronized (WList.mainStageAPI) {
            while (current != stage) {
                WList.mainStageAPI.wait();
                current = WList.mainStageAPI.get();
                if (current == 3)
                    break;
            }
        }
        if (current == 3 && stage == 3)
            return true;
        return current != 3;
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger", WList.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, false, true, HMergedStream.getFileOutputStreamNoException(null));

    public static void main(final String @NotNull ... args) throws IOException, SQLException, InterruptedException {
        if (!WList.mainStageAPI.compareAndSet(-1, 0)) return;
        HExceptionWrapper.addUncaughtExceptionListener((t, e) -> WList.logger.log(HLogLevel.FAULT, "Uncaught exception by WList. thread: ", t.getName(), e));
        try {
            File runtimePath = new File("/").getAbsoluteFile();
            for (final String arg : args) {
                if ("-Debug".equalsIgnoreCase(arg))
                    WList.DebugMode = true;
                if ("-NoDebug".equalsIgnoreCase(arg))
                    WList.DebugMode = false;
                if (arg.startsWith("-path:"))
                    runtimePath = new File(arg.substring("-path:".length())).getAbsoluteFile();
            }
            if (WList.DebugMode) System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
            HLog.setDebugMode(WList.DebugMode);
            WList.logger.log(HLogLevel.FINE, "Hello WList! Loading...");
            final File configurationPath = new File(runtimePath, "server.yaml");
            WList.logger.log(HLogLevel.LESS, "Initializing global configuration.", ParametersMap.create().add("file", configurationPath));
            GlobalConfiguration.initialize(configurationPath);
            WList.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
            final File databasePath = new File(runtimePath, "data.db");
            WList.logger.log(HLogLevel.LESS, "Initializing databases.", ParametersMap.create().add("file", databasePath));
            DatabaseUtil.initialize(databasePath);
            try {
                ConstantManager.quicklyInitialize(new ConstantSqlHelper(DatabaseUtil.getInstance()), "initialize");
                UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(DatabaseUtil.getInstance()), "initialize");
                UserManager.quicklyInitialize(new UserSqlHelper(DatabaseUtil.getInstance()), "initialize");
            } catch (final RuntimeException exception) {
                throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            }
            WList.logger.log(HLogLevel.VERBOSE, "Initialized databases.");
            final File driversPath = new File(runtimePath, "configs");
            WList.logger.log(HLogLevel.LESS, "Initializing driver manager.", ParametersMap.create().add("directory", driversPath));
            DriverManager.initialize(driversPath);
            WList.logger.log(HLogLevel.VERBOSE, "Initialized driver manager.");
            try {
                WList.logger.log(HLogLevel.LESS, "Initializing WList server.");
                ServerHandlerManager.initialize();
                WListServer.initialize(new InetSocketAddress(GlobalConfiguration.getInstance().port()));
                WList.logger.log(HLogLevel.VERBOSE, "Initialized WList server.");
                WList.setMainStageAPI(1);
                WListServer.getInstance().start();
                WListServer.getInstance().awaitStop();
            } finally {
                WList.setMainStageAPI(2);
                WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
                // TODO Save in time.
                WList.logger.log(HLogLevel.INFO, "Saving driver configurations in multithreading...");
                for (final DriverInterface<?> driver: DriverManager.getAllDrivers())
                    WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> DriverManager.dumpConfigurationIfModified(driver.getConfiguration())));
                final Future<?>[] futures = new Future[4];
                futures[0] = WListServer.CodecExecutors.shutdownGracefully();
                futures[1] = WListServer.ServerExecutors.shutdownGracefully();
                futures[2] = WListServer.IOExecutors.shutdownGracefully();
                futures[3] = BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
                for (final Future<?> future : futures)
                    future.sync();
                WList.logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
            }
        } finally {
            WList.setMainStageAPI(3);
        }
    }
}
