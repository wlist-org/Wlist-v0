package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Configurations.GlobalConfiguration;
import com.xuxiaocheng.WList.Internal.Server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

    public static void main(final String[] args) throws FileNotFoundException {
        WList.logger.log(HLogLevel.FINE, "Hello WList!");
        GlobalConfiguration.getInstance(new BufferedInputStream(new FileInputStream("config.yml")));
        final Server server = Server.getInstance(new InetSocketAddress(GlobalConfiguration.getInstance(null).getPort()));
        server.start().syncUninterruptibly();
    }
}
