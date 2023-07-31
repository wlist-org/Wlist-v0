package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandlerManager;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class WList {
    private WList() {
        super();
    }

    private static final @NotNull AtomicInteger mainStageAPI = new AtomicInteger(-1);
    private static final @NotNull HInitializer<Throwable> mainStageExceptionAPI = new HInitializer<>("MainStageException");
    private static void setMainStageAPI(final int stage) {
        synchronized (WList.mainStageAPI) {
            WList.mainStageAPI.set(stage);
            WList.mainStageAPI.notifyAll();
        }
    }
    public static int getMainStageAPI() {
        return WList.mainStageAPI.get();
    }
    public static boolean waitMainStageAPI(final int stage, final boolean mayNotBoot) throws InterruptedException {
        if (stage < -1 || 3 < stage)
            throw new IllegalArgumentException("Illegal target stage." + ParametersMap.create().add("stage", stage));
        int current = WList.mainStageAPI.get();
        if (current == stage)
            return true;
        if (current == -1 && mayNotBoot)
            return false;
        synchronized (WList.mainStageAPI) {
            while (current != stage) {
                if (current == 3)
                    break;
                WList.mainStageAPI.wait();
                current = WList.mainStageAPI.get();
            }
        }
        if (current == 3 && stage == 3)
            return true;
        return current != 3;
    }
    public static @NotNull HInitializer<Throwable> getMainStageExceptionAPI() {
        return WList.mainStageExceptionAPI;
    }

    static {
        try {
            final boolean notIde = new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
            if (notIde && System.getProperty("com.xuxiaocheng.Logger.HLogLevel.color") == null)
                System.setProperty("com.xuxiaocheng.Logger.HLogLevel.color", "2");
        } catch (final RuntimeException ignore) { // for Android (NPE).
        }
    }

    public static void main(final String @NotNull ... args) {
        if (!WList.mainStageAPI.compareAndSet(-1, 0)) return;
        File runtimePath = new File("").getAbsoluteFile();
        for (final String arg : args) {
            if ("-Debug".equalsIgnoreCase(arg))
                HLog.setDebugMode(true);
            if ("-NoDebug".equalsIgnoreCase(arg))
                HLog.setDebugMode(false);
            if (arg.startsWith("-path:"))
                runtimePath = new File(arg.substring("-path:".length())).getAbsoluteFile();
            if ("/?".equals(arg)) {
                // TODO help.
                return;
            }
        }
        if (HLog.isDebugMode()) System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        final HLog logger = HLog.createInstance("DefaultLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, false, true, HMergedStream.getFileOutputStreamNoException(null));
        HUncaughtExceptionHelper.putIfAbsentUncaughtExceptionListener("listener", (t, e) -> logger.log(HLogLevel.FAULT, "Uncaught exception listened by WList. thread: ", t.getName(), e));
        try {
            logger.log(HLogLevel.FINE, "Hello WList (Server v0.2.2)! Loading...");
            final File configurationPath = new File(runtimePath, "server.yaml");
            logger.log(HLogLevel.LESS, "Initializing global configuration.", ParametersMap.create().add("file", configurationPath));
            GlobalConfiguration.initialize(configurationPath);
            logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
            final File databasePath = new File(runtimePath, "data.db");
            logger.log(HLogLevel.LESS, "Initializing databases.", ParametersMap.create().add("file", databasePath));
            PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(databasePath));
            try {
                ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
                UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
                UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
            } catch (final RuntimeException exception) {
                throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            }
            logger.log(HLogLevel.VERBOSE, "Initialized databases.");
            final File driversPath = new File(runtimePath, "configs");
            logger.log(HLogLevel.LESS, "Initializing driver manager.", ParametersMap.create().add("directory", driversPath));
            DriverManager.initialize(driversPath);
            logger.log(HLogLevel.VERBOSE, "Initialized driver manager.");
            try {
                logger.log(HLogLevel.LESS, "Initializing WList server.");
                ServerHandlerManager.initialize();
                logger.log(HLogLevel.VERBOSE, "Initialized WList server.");
                WListServer.getInstance().start(GlobalConfiguration.getInstance().port());
                WList.setMainStageAPI(1);
                WListServer.getInstance().awaitStop();
            } finally {
                WList.setMainStageAPI(2);
                logger.log(HLogLevel.INFO, "Saving driver configurations in multithreading...");
                for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                    logger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
                logger.log(HLogLevel.FINE, "Shutting down the whole application...");
                final Future<?>[] futures = new Future[4];
                futures[0] = WListServer.CodecExecutors.shutdownGracefully();
                futures[1] = WListServer.ServerExecutors.shutdownGracefully();
                futures[2] = WListServer.IOExecutors.shutdownGracefully();
                futures[3] = BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
                for (final Future<?> future : futures)
                    future.sync();
                logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            WList.mainStageExceptionAPI.initialize(throwable);
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        } finally {
            WList.setMainStageAPI(3);
        }
    }
}
