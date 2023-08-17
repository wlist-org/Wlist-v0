package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.GlobalConfiguration;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

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

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String[] args) throws Exception {
//        if (true) return;
        Main.logger.log(HLogLevel.FINE, "Hello WList Client Java Library v0.2.2!");
        GlobalConfiguration.initialize(null);
        final @NotNull SocketAddress address = new InetSocketAddress("127.0.0.1", 5212);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        try {
            //noinspection SpellCheckingInspection
            final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiIyIiwic3ViIjoiMzU3ODk3MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTI1NTAxMDcsImp0aSI6IjE2OTAxMzQwMDAifQ.q6ciMwIz4ooUnPBrz1IBLAfFgbaQsRQKEps28vaSLN-mc2aBKmqt-6omNcfhCkVV-a6oSRWuoSSt6OxmAYO1ng";
            //noinspection SpellCheckingInspection
            final String adminToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiIxIiwic3ViIjoiMzU3ODk3MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTI1NDk3NTMsImp0aSI6IjE2OTAxMzQwMDAifQ.hYoB_Srm1Ix9sdwuOdtKC_FSLOkYI1YW7AejyvVCl_nNdpOg60JUBnMJrcp2WStWgfrkqqVERkK8tM4FkjtRig";
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                Main.logger.log(HLogLevel.FINE, OperateUserHelper.getPermissions(client, token));
            }
        } finally {
            WListClientManager.quicklyUninitialize(address);
        }
    }
}
