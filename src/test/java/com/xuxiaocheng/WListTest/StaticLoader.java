package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;

public final class StaticLoader {
    private StaticLoader() {
        super();
    }

    public static void load() {
    }  static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        HLog.setLogCaller(true);
        HLog.LoggerCreateCore.reinitialize(n -> HLog.createInstance(n, HLogLevel.DEBUG.getLevel(), true));
    }
}
