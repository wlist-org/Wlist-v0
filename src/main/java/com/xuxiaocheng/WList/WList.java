package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.HeadLibs;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.PooledSqlDatabase;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqliteHelper;
import com.xuxiaocheng.WList.Server.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Handlers.ServerHandlerManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class WList {
    private WList() {
        super();
    }

    private static final @NotNull AtomicInteger mainStageAPI = new AtomicInteger(-1);
    private static void setMainStageAPI(final int stage) {
        synchronized (WList.mainStageAPI) {
            WList.mainStageAPI.set(stage);
            WList.mainStageAPI.notifyAll();
        }
    }
    public static int getMainStageAPI() {
        return WList.mainStageAPI.get();
    }
    public static boolean waitMainStageAPI(final int stage, final boolean mayNotStart) throws InterruptedException {
        if (stage < -1 || 3 < stage)
            throw new IllegalArgumentException("Illegal target stage." + ParametersMap.create().add("stage", stage));
        int current = WList.mainStageAPI.get();
        if (current == stage)
            return true;
        if (current == -1 && mayNotStart)
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

    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener("listener", (t, e) -> HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Uncaught exception listened by WList. thread: ", t.getName(), e));
        try {
            final boolean notIde = new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
            if (notIde && System.getProperty("com.xuxiaocheng.Logger.HLogLevel.color") == null) System.setProperty("com.xuxiaocheng.Logger.HLogLevel.color", "2");
        } catch (final RuntimeException ignore) { // for Android (NPE).
        }
    }

    public static final @NotNull HInitializer<File> RuntimePath = new HInitializer<>("RuntimePath");
    private static @NotNull HLog logger = HLog.DefaultLogger;

    private static void handleArgs(final @NotNull String @NotNull ... args) {
        File runtimePath = new File("").getAbsoluteFile();
        for (final String arg: args) {
            if ("-Debug".equalsIgnoreCase(arg))
                HeadLibs.setDebugMode(true);
            if ("-NoDebug".equalsIgnoreCase(arg))
                HeadLibs.setDebugMode(false);
            if (arg.startsWith("-path:"))
                runtimePath = new File(arg.substring("-path:".length())).getAbsoluteFile();
            if ("/?".equals(arg)) {
                HLog.DefaultLogger.log(HLogLevel.FINE, "Usage: [-Debug|-NoDebug] [-path:<path>]");
                return;
            }
        }
        WList.RuntimePath.initialize(runtimePath);
    }

    public static void main(final String @NotNull ... args) {
        if (!WList.mainStageAPI.compareAndSet(-1, 0)) return;
        WList.handleArgs(args);
        if (HeadLibs.isDebugMode() && System.getProperty("io.netty.leakDetectionLevel") == null) System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        final HLog logger = HLog.create("DefaultLogger");
        WList.logger = logger;
        try {
            logger.log(HLogLevel.FINE, "Hello WList (Server v0.2.3)! Loading...");
            WList.loadServerConfiguration();
            WList.initializeServerEnvironment();
            WList.initializeServerDatabase();
            WList.initializeStorageProvider();
            try {
                WListServer.getInstance().start(ServerConfiguration.get().port());
                WList.setMainStageAPI(1);
                WListServer.getInstance().awaitStop();
            } finally {
                WList.setMainStageAPI(2);
                logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        } finally {
            WList.checkConfigurationsSaved();
            try {
                WList.shutdownAllExecutors().await();
            } catch (final InterruptedException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            } finally {
                WList.setMainStageAPI(3);
            }
        }
    }

    private static void loadServerConfiguration() throws IOException {
        final File file = new File(WList.RuntimePath.getInstance(), "server.yaml");
        WList.logger.log(HLogLevel.LESS, "Loading server configuration.", ParametersMap.create().add("file", file));
        HFileHelper.ensureFileAccessible(file, true);
        ServerConfiguration.Location.initializeIfNot(() -> file);
        ServerConfiguration.parseFromFile();
        ServerConfiguration.dumpToFile();
        WList.logger.log(HLogLevel.VERBOSE, "Loaded server configuration.");
    }

    private static void initializeServerEnvironment() {
        WList.logger.log(HLogLevel.LESS, "Initializing WList server environment.");
        NetworkTransmission.load(); // Preload to check environment is supported.
        ServerHandlerManager.load();
        WList.logger.log(HLogLevel.VERBOSE, "Initialized WList server environment.");
    }

    private static void initializeServerDatabase() throws IOException, SQLException {
        final File file = new File(WList.RuntimePath.getInstance(), "data.db");
        WList.logger.log(HLogLevel.LESS, "Initializing server databases.", ParametersMap.create().add("file", file));
        HFileHelper.ensureFileAccessible(file, true);
        final PooledSqlDatabase.PooledDatabaseInterface database = PooledSqlDatabase.quicklyOpen(file);
        ConstantManager.quicklyInitialize(new ConstantSqliteHelper(database), "initialize"); // TODO
        UserGroupManager.quicklyInitialize(new UserGroupSqliteHelper(database), "initialize");
        UserManager.quicklyInitialize(new UserSqliteHelper(database), "initialize");
        WList.logger.log(HLogLevel.VERBOSE, "Initialized server databases.");
    }

    private static void initializeStorageProvider() throws IOException {
        final File directory = new File(WList.RuntimePath.getInstance(), "configs");
        WList.logger.log(HLogLevel.LESS, "Initializing storage provider.", ParametersMap.create().add("directory", directory));
        HFileHelper.ensureDirectoryExist(directory.toPath());
        DriverManager.initialize(directory);
        WList.logger.log(HLogLevel.VERBOSE, "Initialized storage provider.");
    }

    private static void checkConfigurationsSaved() {
        WList.logger.log(HLogLevel.INFO, "Checking configurations is saved...");
        for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
            WList.logger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
    }

    @SuppressWarnings("unchecked")
    private static @NotNull CountDownLatch shutdownAllExecutors() {
        WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
        final CountDownLatch latch = new CountDownLatch(5);
        WListServer.CodecExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        WListServer.ServerExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        WListServer.IOExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        DriverNetworkHelper.CountDownExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        return latch;
    }
}
