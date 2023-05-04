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
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException, InterruptedException {
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
        HLog.DefaultLogger.log("DEBUG", "Executors is stopping");
        WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
        WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
        HLog.DefaultLogger.log("INFO", "Executors stopped gracefully.");
    }

    private static void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        //noinspection SpellCheckingInspection
        final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJhZG1pbiIsInN1YiI6IjI2MjIxMjEwMCIsImlzcyI6IldMaXN0IiwiZXhwIjoxNjgzNDc4MzU1LCJqdGkiOiIxNjgzMjE4NTQwIn0.pDoIlk0Titm26qJ4RLSl35yxUkjEvIUJDtNFdVw0JKyI8bB3Oue-zbg5Wq0nbABXDpFBaSqJzKzef96a6ZfSag";
        if (false) {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "Login");
            ByteBufIOUtil.writeUTF(buffer, "admin");
            //noinspection SpellCheckingInspection
            ByteBufIOUtil.writeUTF(buffer, "lJbtzCGp");
            channel.writeAndFlush(buffer);
        }
        if (false){
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "ListFiles");
            ByteBufIOUtil.writeUTF(buffer, token);
            ByteBufIOUtil.writeUTF(buffer, "/123pan");
            ByteBufIOUtil.writeVariableLenInt(buffer, 10);
            ByteBufIOUtil.writeVariableLenInt(buffer, 1);
            ByteBufIOUtil.writeUTF(buffer, "D");
            ByteBufIOUtil.writeUTF(buffer, "D");
            channel.writeAndFlush(buffer);
        }
        if (true){
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "RequestDownloadFile");
            ByteBufIOUtil.writeUTF(buffer, token);
            ByteBufIOUtil.writeUTF(buffer, "/123pan/AutoCopy.zip");
            ByteBufIOUtil.writeVariableLenLong(buffer, 1);
            ByteBufIOUtil.writeVariableLenLong(buffer, 4);
            channel.writeAndFlush(buffer);
        }
        TimeUnit.SECONDS.sleep(60);
    }

    public static void client(final @NotNull ByteBuf buffer) throws IOException {
        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readVariableLenLong(buffer));
        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
    }
}
