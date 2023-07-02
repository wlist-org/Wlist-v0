package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String @NotNull [] args) throws Exception {
//        if (true) return;
        WListTest.wrapServerInitialize(() -> {
            final DriverInterface<?> driver = Objects.requireNonNull(DriverManager.get("123pan"));

//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
            return null;
        });
    }


    @SuppressWarnings("OverlyBroadThrowsClause")
    static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        GlobalConfiguration.initialize(new File("server.yaml"));
        ConstantManager.initialize();
        UserGroupManager.initialize();
        UserManager.initialize();
        DriverManager.initialize();
        try {
            final Object obj = runnable.get();
            if (obj != null)
                HLog.DefaultLogger.log("", obj);
        } finally {
            WListServer.CodecExecutors.shutdownGracefully().syncUninterruptibly();
            WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
            WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
        }
    }
}
