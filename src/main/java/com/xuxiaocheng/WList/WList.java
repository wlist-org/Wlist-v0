package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.DownloadIdHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DeepDebugMode = false;
    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, HMergedStream.createNoException(true, WList.DebugMode ? null : ""));

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

    public static void main(final String[] args) throws InterruptedException, IOException, ExecutionException {
        GlobalConfiguration.init(null);
        GlobalConfiguration.getInstance().setDownload_id_expire_time(3);
        final long id = DownloadIdHelper.generateId(new ByteArrayInputStream("AString tester.".getBytes(StandardCharsets.UTF_8)));
        if (DownloadIdHelper.download(id) == null) throw new AssertionError();
        TimeUnit.SECONDS.sleep(5);
        if (DownloadIdHelper.download(id) != null) throw new AssertionError();

        DownloadIdHelper.cleaner.interrupt();
        WListServer.ServerExecutors.shutdownGracefully().sync();
        WListServer.IOExecutors.shutdownGracefully().sync();
    }
}
