package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

// TODO
public final class BackgroundTask {
    private BackgroundTask() {
        super();
    }

    public static void background(final @NotNull RunnableE runnable) {
        WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(runnable));
    }
}
