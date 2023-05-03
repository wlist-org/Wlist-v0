package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.ServerHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WListTest {
    public static void main(final String[] args) throws InterruptedException, IOException, SQLException {
        UserSqlHelper.init(ServerHandler.DefaultPermission, ServerHandler.AdminPermission);
        final WListServer server = new WListServer(new InetSocketAddress(5212));
        server.start();
        final WListClient client = new WListClient(new InetSocketAddress(5212));
        client.start();
        HLog.DefaultLogger.log("FINE", "**********START!**********");
        WListTest.client(client.getChannel());
        HLog.DefaultLogger.log("FINE", "**********FINISH!**********");
        client.stop();
        server.stop();
        System.exit(0);
    }

    protected static void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "Register");
            ByteBufIOUtil.writeUTF(buffer, "xxc");
            ByteBufIOUtil.writeUTF(buffer, "123456");
            channel.writeAndFlush(buffer);
        }
        {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "ReducePermission");
            ByteBufIOUtil.writeUTF(buffer, "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJhZG1pbiIsInN1YiI6IjY3MzE2OTUwMCIsImlzcyI6IldMaXN0IiwiZXhwIjoxNjgzMzg1NjM1LCJqdGkiOiIxNjgzMTI1Nzk3In0.IlarqgH21kvdazvR_ZSf5f8CyZrKwexdWQp3n4kmWQpC7nGmJyOkJ5PwbtzDL6v13zOokYS0su8PDu-nNlAgLg");
            ByteBufIOUtil.writeUTF(buffer, "xxc");
            ByteBufIOUtil.writeUTF(buffer, "[\"FilesList\"]");
            channel.writeAndFlush(buffer);
        }
//        synchronized (WListTest.lock) {
//            while (WListTest.lock.get())
//                WListTest.lock.wait();
//        }
        TimeUnit.SECONDS.sleep(5);
    }

    static final AtomicBoolean lock = new AtomicBoolean(true);
}
