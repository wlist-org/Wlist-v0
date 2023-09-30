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
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Operations.ServerHandlerManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Util.IdsHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WList {
    private WList() {
        super();
    }

    private static final @NotNull AtomicBoolean isStarting = new AtomicBoolean(true);
    private static void setStarted() {
        synchronized (WList.isStarting) {
            WList.isStarting.set(false);
            WList.isStarting.notifyAll();
        }
    }
    public static void waitStarted() throws InterruptedException {
        if (!WList.isStarting.get())
            return;
        synchronized (WList.isStarting) {
            while (WList.isStarting.get())
                WList.isStarting.wait();
        }
    }

    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) -> HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Uncaught exception listened by WList. thread: ", t.getName(), e));
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
        final File path = runtimePath;
        WList.RuntimePath.initializeIfNot(() -> path);
    }

    public static void main(final String @NotNull ... args) {
        WList.handleArgs(args);
        if (HeadLibs.isDebugMode() && System.getProperty("io.netty.leakDetectionLevel") == null) System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        final HLog logger = HLog.create("DefaultLogger");
        WList.logger = logger;
        try {
            logger.log(HLogLevel.FINE, "Hello WList (Server v0.3.0)! Loading...");
            WList.loadServerConfiguration();
            WList.initializeServerEnvironment();
            WList.initializeServerDatabase();
            WList.initializeStorageProvider();
            try {
                WListServer.getInstance().start(ServerConfiguration.get().port());
                WList.setStarted();
                WListServer.getInstance().awaitStop();
            } finally {
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
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
        ConstantManager.quicklyInitialize(database, "initialize");
        UserGroupManager.quicklyInitialize(database, "initialize");
        UserManager.quicklyInitialize(database, "initialize");
        WList.logger.log(HLogLevel.VERBOSE, "Initialized server databases.");
    }

    private static void initializeStorageProvider() throws IOException {
        final File configuration = new File(WList.RuntimePath.getInstance(), "configs");
        final File cache = new File(WList.RuntimePath.getInstance(), "caches");
        WList.logger.log(HLogLevel.LESS, "Initializing storage provider.", ParametersMap.create().add("directory", configuration));
        HFileHelper.ensureDirectoryExist(configuration.toPath());
        StorageManager.initialize(configuration, cache);
        WList.logger.log(HLogLevel.VERBOSE, "Initialized storage provider.");
    }

    private static void checkConfigurationsSaved() {
        WList.logger.log(HLogLevel.INFO, "Checking configurations is saved...");
        for (final Map.Entry<String, ProviderInterface<?>> provider: StorageManager.getAllProviders().entrySet())
            try {
                StorageManager.dumpConfigurationIfModified(provider.getValue().getConfiguration());
            } catch (final IOException exception) {
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump provider configuration.", ParametersMap.create().add("name", provider.getKey()), exception);
            }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull CountDownLatch shutdownAllExecutors() {
        WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
        final CountDownLatch latch = new CountDownLatch(6);
        WListServer.CodecExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        WListServer.ServerExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        WListServer.IOExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        HttpNetworkHelper.CountDownExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        IdsHelper.CleanerExecutors.shutdownGracefully().addListeners(f -> latch.countDown());
        return latch;
    }
}
