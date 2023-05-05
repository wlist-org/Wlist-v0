package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Server.DownloadIdHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
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

    public static void main(final String[] args) throws InterruptedException {
        final AtomicInteger f = new AtomicInteger(0);
        CompletableFuture<?> future = CompletableFuture.completedFuture(null);
        for (int k = 0; k < 1000; ++k) {
            final int c = k;
            future = CompletableFuture.allOf(future,
                    CompletableFuture.runAsync(() -> {
                        final ByteBuf in = ByteBufAllocator.DEFAULT.buffer();
                        try {
                            ByteBufIOUtil.writeUTF(in, "A string test.");
                            final long id = DownloadIdHelper.generateId(new ByteBufInputStream(in));
//                            HLog.DefaultLogger.log("INFO", c, ": ", id);
                            for (int i = 0; i < 10; ++i) {
                                final ByteBuf buf = DownloadIdHelper.download(id);
                                if (buf != null) {
//                                    final byte[] b = new byte[3];
//                                    buf.readBytes(b);
                                    buf.release();
//                                    HLog.DefaultLogger.log("DEBUG", c, ": ", new String(b));
                                }
                                else {
//                                    HLog.DefaultLogger.log("FINE", c, ": ", i);
                                    f.addAndGet(i);
                                    break;
                                }
                            }
                        } catch (final InterruptedException | IOException | ExecutionException exception) {
                            HLog.DefaultLogger.log("WARN", exception);
                        } finally {
                            in.release();
                        }
                    }, WListServer.ServerExecutors));
        }
        future.join();
        HLog.DefaultLogger.log("FINE", "Stopping. ", f);
        assert f.get() == 5000;
        WListServer.ServerExecutors.shutdownGracefully().sync();
        WListServer.IOExecutors.shutdownGracefully().sync();
    }
}
