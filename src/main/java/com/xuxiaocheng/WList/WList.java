package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.FileDownloadIdHelper;
import com.xuxiaocheng.WList.Server.ServerHandler;
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
        if (!WList.InIdeaMode) System.setProperty("HLogLevel.color", "2");
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.InIdeaMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
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

    public static void main(final String @NotNull [] args) throws IOException, InterruptedException, SQLException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> WList.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
        WList.logger.log(HLogLevel.FINE, "Hello WList! Initializing...");
        final File configuration = new File(args.length > 0 ? args[0] : "server.yml");
        WList.logger.log(HLogLevel.LESS, "Initializing global configuration. file: ", configuration.getAbsolutePath());
        GlobalConfiguration.init(configuration);
        WList.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
        if(true)return;
        DriverManager.init();
        UserSqlHelper.init(ServerHandler.DefaultPermission, ServerHandler.AdminPermission);
        WListServer.init(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
        WListServer.getInstance().start().syncUninterruptibly();
        WList.logger.log(HLogLevel.FINE, "Shutting down the whole server...");
        WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
        WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
        FileDownloadIdHelper.cleaner.interrupt();
    }

}
