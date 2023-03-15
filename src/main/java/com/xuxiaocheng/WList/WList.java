package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Configurations.GlobalConfiguration;
import com.xuxiaocheng.WList.Internal.Server.Token.TokenManager;
import com.xuxiaocheng.WList.Internal.Server.WListServer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String[] args) throws Exception {
        WList.logger.log(HLogLevel.FINE, "Hello WList!");
        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("config.yml")));
        TokenManager.init();
        final WListServer server = WListServer.getInstance(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
        server.start().syncUninterruptibly();
    }
}
