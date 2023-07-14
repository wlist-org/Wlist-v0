package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandlerManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
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

    public static final boolean InIdeaMode = false;//!new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
    static {
        if (!WList.InIdeaMode && System.getProperty("HLogLevel.color") == null)
            System.setProperty("HLogLevel.color", "2");
        if (WList.isDebugMode())
            System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    private static final HLog logger = WList.InIdeaMode ? HLog.createInstance("DefaultLogger", Integer.MIN_VALUE, false, true) :
            HLog.createInstance("DefaultLogger", WList.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.FINE.getLevel(), false, true, HMergedStream.getFileOutputStreamNoException(null));

    private static final @NotNull AtomicInteger mainStageAPI = new AtomicInteger(-1);

    public static int getMainStageAPI() {
        return WList.mainStageAPI.get();
    }

    public static void main(final String @NotNull ... args) throws IOException, SQLException, InterruptedException {
        if (!WList.mainStageAPI.compareAndSet(-1, 0))
            return;
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> WList.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
        File configuration = new File("server.yaml");
        for (final String arg: args) {
            if ("-Debug".equalsIgnoreCase(arg))
                WList.DebugMode = true;
            if ("-NoDebug".equalsIgnoreCase(arg))
                WList.DebugMode = false;
            if (arg.startsWith("-path:"))
                configuration = new File(arg.substring("-file:".length()), "server.yaml");
        }
        configuration = configuration.getAbsoluteFile();
        HLog.setDebugMode(WList.DebugMode);
        WList.logger.log(HLogLevel.FINE, "Hello WList! Loading...");
        WList.logger.log(HLogLevel.LESS, "Initializing global configuration. file: ", configuration);
        GlobalConfiguration.initialize(configuration);
        ConstantManager.initialize();
        WList.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
        WList.logger.log(HLogLevel.LESS, "Initializing user database.");
        UserGroupManager.initialize();
        UserManager.initialize();
        WList.logger.log(HLogLevel.VERBOSE, "Initialized user database.");
        WList.logger.log(HLogLevel.LESS, "Initializing driver manager.");
        DriverManager.initialize(new File(configuration.getParentFile(), "configs"));
        WList.logger.log(HLogLevel.VERBOSE, "Initialized driver manager.");
        try {
            WList.logger.log(HLogLevel.LESS, "Initializing WList server.");
            ServerHandlerManager.initialize();
            WListServer.initialize(new InetSocketAddress(GlobalConfiguration.getInstance().port()));
            WList.logger.log(HLogLevel.VERBOSE, "Initialized WList server.");
            WList.mainStageAPI.set(1);
            WListServer.getInstance().start();
            WListServer.getInstance().awaitStop();
        } finally {
            WList.mainStageAPI.set(2);
            WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
            if (GlobalConfiguration.getInstance().dumpConfiguration()) {
                // TODO Save in time.
                WList.logger.log(HLogLevel.INFO, "Saving driver configurations in multithreading...");
                for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
                    WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> DriverManager.dumpConfiguration(driver.getSecond().getConfiguration())));
            }
            final Future<?>[] futures = new Future[4];
            futures[0] = WListServer.CodecExecutors.shutdownGracefully();
            futures[1] = WListServer.ServerExecutors.shutdownGracefully();
            futures[2] = WListServer.IOExecutors.shutdownGracefully();
            futures[3] = BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            for (final Future<?> future: futures)
                future.sync();
            WList.logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
            WList.mainStageAPI.set(3);
        }
    }
}
