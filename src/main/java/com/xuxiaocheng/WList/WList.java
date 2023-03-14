package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Internal.Server;

import java.io.File;
import java.net.InetSocketAddress;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

    public static void main(final String[] args) throws InterruptedException {
        WList.logger.log(HLogLevel.FINE, "Hello WList!");
        final Server server = Server.getInstance(new InetSocketAddress(5212));
        server.start().sync();
    }
}
