package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerUserHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Random;
import java.util.random.RandomGenerator;

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

    public static final @NotNull BigInteger key;
    public static final @NotNull BigInteger vector;
    static {
        final RandomGenerator generator = new Random(5212);
        final byte[] bytes = new byte[512];
        generator.nextBytes(bytes);
        key = new BigInteger(bytes);
        generator.nextBytes(bytes);
        vector = new BigInteger(bytes);
    }

    public static void main(final String @NotNull [] args) throws IOException, SQLException, InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> WList.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
        WList.logger.log(HLogLevel.FINE, "Hello WList! Initializing...");
        final File configuration = new File(args.length > 0 ? args[0] : "server.toml");
        WList.logger.log(HLogLevel.LESS, "Initializing global configuration. file: ", configuration.getAbsolutePath());
        GlobalConfiguration.init(configuration);
        WList.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
        WList.logger.log(HLogLevel.LESS, "Initializing driver manager.");
        DriverManager.init();
        WList.logger.log(HLogLevel.VERBOSE, "Initialized driver manager.");
        WList.logger.log(HLogLevel.LESS, "Initializing user database.");
        UserSqlHelper.init(ServerUserHandler.DefaultPermission, ServerUserHandler.AdminPermission);
        WList.logger.log(HLogLevel.VERBOSE, "Initialized user database.");
        WListServer.init(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
        WList.logger.log(HLogLevel.VERBOSE, "Initialized WList server.");
        WListServer.getInstance().start();
        try {
            WListServer.getInstance().awaitStop();
        } finally {
            WList.logger.log(HLogLevel.FINE, "Shutting down the whole application...");
            WListServer.CodecExecutors.shutdownGracefully().syncUninterruptibly();
            WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
            WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
            WList.logger.log(HLogLevel.MISTAKE, "Thanks to use WList.");
        }
    }
}
