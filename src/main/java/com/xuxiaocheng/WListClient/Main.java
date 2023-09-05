package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.GlobalConfiguration;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class Main {
    private Main() {
        super();
    }

    public static final boolean DebugMode = true;
    public static final boolean InIdeaMode = !new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();
    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListClientTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
        if (!Main.InIdeaMode && System.getProperty("HLogLevel.color") == null) System.setProperty("HLogLevel.color", "2");
        HLog.setDebugMode(Main.DebugMode);
    }

    private static final HLog logger = HLog.createInstance("DefaultLogger", Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true);

    public static void main(final String[] args) throws IOException {
        Main.logger.log(HLogLevel.FINE, "Hello WList Client Java Library v0.2.3!");

        GlobalConfiguration.initialize(null);
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 5212);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        try {
            //noinspection SpellCheckingInspection
            final String token = "eyJhdWQiOiIyIiwic3ViIjoiNjExNzU2MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTQyMjM5NDIsImp0aSI6IjE2OTIzMTI0MjgifQ.97KlfhuzrS0O_yW9lM-WXUp65_jMHRi4H1CB8EgowtPJ5Ju2KcHXXUnToOvwUa2o7GXw8UpXddsZIo7GK9ftZQ";
            //noinspection SpellCheckingInspection
            final String admin = "eyJhdWQiOiIxIiwic3ViIjoiNzA0ODA0MzAwIiwiaXNzIjoiV0xpc3QiLCJleHAiOjE2OTQyMjM5NDEsImp0aSI6IjE2OTIzMTIzNTgifQ.HhrdY5avIi6HVCQezklL5MfV7Zr9WZUKmg1TWsqvm20MRlttE6x1xUgrhMmDgysQZVzN_NViJsAvPZCFCBzqLw";
            Main.test(address, token, admin);
        } catch (final RuntimeException | IOException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            WListClientManager.quicklyUninitialize(address);
        }
    }

    public static void test(final @NotNull SocketAddress address, final @NotNull String token, final @NotNull String admin) throws Exception {
//            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
//                Main.logger.log(HLogLevel.FINE, OperateFileHelper.listFiles(client, token, new FileLocation("lanzou_136", -1),
//                        Options.DirectoriesOrFiles.Both, 50, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false));
//            }


        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            OperateServerHelper.closeServer(client, admin);
        }
    }
}
