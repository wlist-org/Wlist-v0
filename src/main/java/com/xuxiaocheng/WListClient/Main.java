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
            final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiIyIiwic3ViIjoiNjExNzU2MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTI1NzE2NTQsImp0aSI6IjE2OTIzMTI0MjgifQ.fdrnOyY2Dqo2XNMqD_E5zxgBfxLOvj-jIrYONAag3eLUQ76vY2U0mU3kqTvjaBG98uz2X8KvRn04mnxRevhtIg";
            //noinspection SpellCheckingInspection
            final String adminToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiIxIiwic3ViIjoiNzA0ODA0MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTI1NzE1OTAsImp0aSI6IjE2OTIzMTIzNTgifQ.OOkqw70lPiHzdWuOUA6L_HkQ0GtyxpnsWwGsAqIM-jG7H0QCp4vWfkWK6KVEEZhthsqpwKpkJLFZM39V_0imww";
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                Main.logger.log(HLogLevel.FINE, OperateUserHelper.getPermissions(client, token));
            }
        } finally {
            WListClientManager.quicklyUninitialize(address);
        }
    }
}
