package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.ConsoleMenus;
import com.xuxiaocheng.WListClient.Client.GlobalConfiguration;
import com.xuxiaocheng.WListClient.Client.WListClient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

public final class Main {
    private Main() {
        super();
    }

    public static final boolean DebugMode = true;
    public static final boolean InIdeaMode = !new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
    static {
        if (!Main.InIdeaMode && System.getProperty("HLogLevel.color") == null) System.setProperty("HLogLevel.color", "2");
        HLog.setDebugMode(Main.DebugMode);
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
            true);

    public static void main(final String[] args) throws IOException, InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Main.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e);
            WListClient.ClientEventLoop.shutdownGracefully().syncUninterruptibly();
        });
        Main.logger.log(HLogLevel.FINE, "Hello WList Client (Console Version)! Initializing...");
        final File configuration = new File(args.length > 0 ? args[0] : "client.yaml");
        Main.logger.log(HLogLevel.LESS, "Initializing global configuration. file: ", configuration.getAbsolutePath());
        GlobalConfiguration.init(configuration);
        Main.logger.log(HLogLevel.VERBOSE, "Initialized global configuration.");
        final SocketAddress address = new InetSocketAddress(GlobalConfiguration.getInstance().host(), GlobalConfiguration.getInstance().port());
        Main.logger.log(HLogLevel.LESS, "Connecting to WList Server...");
        Main.logger.log(HLogLevel.INFO, "Address: ", address);
        final WListClient client = new WListClient(address);
//        final WListClient broadcast = new WListClient(new InetSocketAddress(GlobalConfiguration.getInstance().host(), GlobalConfiguration.getInstance().port()));
//        OperateServerHelper.setBroadcastMode(broadcast, true);
        Main.logger.log(HLogLevel.VERBOSE, "Initialized WList clients.");
//        broadcast.stop();// TODO broadcast
        final AtomicReference<String> token = new AtomicReference<>(null);
        //noinspection StatementWithEmptyBody
        while (ConsoleMenus.chooseMenu(client, token));
        Main.logger.log(HLogLevel.FINE, "Shutting down the clients...");
        client.stop();
        WListClient.ClientEventLoop.shutdownGracefully().sync();
        Main.logger.log(HLogLevel.INFO, "WList clients stopped gracefully.");
        Main.logger.log(HLogLevel.MISTAKE, "Thanks to use WList Client (Console Version).");
    }
}
