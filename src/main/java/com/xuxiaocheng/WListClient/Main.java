package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;

import java.io.File;

public final class Main {
    private Main() {
        super();
    }

    public static final boolean DebugMode = true;
    public static final boolean InIdeaMode = !new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.listenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListClientTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        if (!Main.InIdeaMode && System.getProperty("HLogLevel.color") == null) System.setProperty("HLogLevel.color", "2");
        HLog.setDebugMode(Main.DebugMode);
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger", Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true);

    public static void main(final String[] args) throws Exception {
        Main.logger.log(HLogLevel.FINE, "Hello WList Client Java Library v0.2.2!");
    }
}
