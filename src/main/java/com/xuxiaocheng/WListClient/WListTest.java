package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.ServerHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, java.io.FileNotFoundException, java.sql.SQLException, InterruptedException {
        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("config.yml")));
        DriverManager.init();
        UserSqlHelper.init(ServerHandler.DefaultPermission, ServerHandler.AdminPermission);
        final WListServer server = new WListServer(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
        server.start();

        final WListClient client = new WListClient(new InetSocketAddress(5212));
        client.start();
        HLog.DefaultLogger.log("FINE", "**********START!**********");
        WListTest.client(client.getChannel());
        HLog.DefaultLogger.log("FINE", "**********FINISH!**********");
        client.stop();
        server.stop();
//        System.exit(0);
    }

    private static void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        if (false) {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "Login");
            ByteBufIOUtil.writeUTF(buffer, "admin");
            //noinspection SpellCheckingInspection
            ByteBufIOUtil.writeUTF(buffer, "kBTMFQei");
            channel.writeAndFlush(buffer);
        }
        if (true){
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "DownloadFile");
            //noinspection SpellCheckingInspection
            ByteBufIOUtil.writeUTF(buffer, "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJhZG1pbiIsInN1YiI6IjIwNzY4NTQwMCIsImlzcyI6IldMaXN0IiwiZXhwIjoxNjgzNDE1NjA3LCJqdGkiOiIxNjgzMTU2MzY0In0.HX9rWUPSGoa2gYjJSeyrsgHYxpOwiZrhszXEZJQukFapUhaYV95RGf86OkYixMS7YCvhotTvU21T818o0krqqg");
            ByteBufIOUtil.writeUTF(buffer, "123pan");
            ByteBufIOUtil.writeUTF(buffer, "/AutoCopy");
            channel.writeAndFlush(buffer);
        }
        TimeUnit.SECONDS.sleep(5);
    }

    public static void client(final @NotNull ByteBuf buffer) throws IOException {
        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
    }
}
