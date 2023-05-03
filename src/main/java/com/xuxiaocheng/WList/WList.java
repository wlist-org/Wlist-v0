package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigInteger;
import java.util.Random;
import java.util.random.RandomGenerator;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DeepDebugMode = false;
    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

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

    public static void main(final String[] args) {

    }
}
