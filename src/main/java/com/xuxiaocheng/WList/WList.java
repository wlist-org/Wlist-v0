package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DebugMode = true;
    public static final boolean InIdeaMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
    static {
        if (!WList.InIdeaMode && System.getProperty("HLogLevel.color") == null) System.setProperty("HLogLevel.color", "2");
        HLog.setDebugMode(WList.DebugMode);
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.InIdeaMode ? Integer.MIN_VALUE : WList.DebugMode ? HLogLevel.LESS.getLevel() : HLogLevel.FINE.getLevel(),
            true,  WList.InIdeaMode ? null : HMergedStream.getFileOutputStreamNoException(""));

    public static void main(final String @NotNull [] args) throws IOException, SQLException, InterruptedException {
        try {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> WList.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
            WList.logger.log(HLogLevel.FINE, "Hello WList! Loading...");
            final File configuration = new File(args.length > 0 ? args[0] : "server.yaml");
            WList.logger.log(HLogLevel.LESS, "Initializing global configuration. file: ", configuration.getAbsolutePath());
            GlobalConfiguration.initialize(configuration);
            ConstantManager.initialize();
            WList.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
            WList.logger.log(HLogLevel.LESS, "Initializing user database.");
            UserGroupManager.initialize();
            UserManager.initialize();
            WList.logger.log(HLogLevel.VERBOSE, "Initialized user database.");
            WList.logger.log(HLogLevel.LESS, "Initializing driver manager.");
            DriverManager.init();
            WList.logger.log(HLogLevel.VERBOSE, "Initialized driver manager.");
            WList.logger.log(HLogLevel.VERBOSE, "Initializing WList server.");
            WListServer.init(new InetSocketAddress(GlobalConfiguration.getInstance().port()));
            WList.logger.log(HLogLevel.VERBOSE, "Initialized WList server.");
            WListServer.getInstance().start();
            WListServer.getInstance().awaitStop();
        } finally {
            WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
            final Future<?>[] futures = new Future[4];
            futures[0] = WListServer.CodecExecutors.shutdownGracefully();
            futures[1] = WListServer.ServerExecutors.shutdownGracefully();
            futures[2] = WListServer.IOExecutors.shutdownGracefully();
            futures[3] = BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            for (final Future<?> future: futures)
                future.syncUninterruptibly();
            WList.logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
        }
    }
}
