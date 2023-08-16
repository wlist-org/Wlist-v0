package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.GlobalConfiguration;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;

import java.io.File;
import java.net.InetSocketAddress;

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

    public static void main(final String[] args) throws Exception {
        Main.logger.log(HLogLevel.FINE, "Hello WList Client Java Library v0.2.2!");
        GlobalConfiguration.initialize(null);
        try (final WListClientInterface client = new WListClient(new InetSocketAddress(5212))) {
            final String token = OperateUserHelper.login(client, "admin", "123456");
            HLog.DefaultLogger.log("", "Got token: ", token);
            if (token != null) {
                if (OperateServerHelper.closeServer(client, token))
                    return;
                assert false;
            }
        } finally {
            WListClient.ClientThreadPool.shutdownGracefully();
        }
    }
}
