package com.xuxiaocheng.WListClient;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.ServerHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.WList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException, InterruptedException {
        GlobalConfiguration.init(null);
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
        WList.exit();
    }

    private static String token;
    private static final Object token_lock = new Object();
    private static final AtomicInteger stage = new AtomicInteger(0);

    private static void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "Login");
            ByteBufIOUtil.writeUTF(buffer, "admin");
            ByteBufIOUtil.writeUTF(buffer, "U.jF9u4Z");
            channel.writeAndFlush(buffer);
            synchronized (WListTest.token_lock) {
                while (WListTest.token == null)
                    WListTest.token_lock.wait();
            }
            assert WListTest.stage.get() == 1;
        }
        {
            final String file = "Uploader test.";
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "RequestUploadFile");
            ByteBufIOUtil.writeUTF(buffer, WListTest.token);
            ByteBufIOUtil.writeUTF(buffer, "/123pan/test directory/t.txt");
            ByteBufIOUtil.writeVariableLenLong(buffer, file.getBytes(StandardCharsets.UTF_8).length);
            final String tag = MiscellaneousUtil.getMd5(file.getBytes(StandardCharsets.UTF_8));
            ByteBufIOUtil.writeUTF(buffer, tag);
            ByteBufIOUtil.writeUTF(buffer, JSON.toJSONString(List.of(tag)));
            channel.writeAndFlush(buffer);
            synchronized (WListTest.stage) {
                while (WListTest.stage.get() != 2)
                    WListTest.stage.wait();
            }
        }
    }

    public static void client(final @NotNull ByteBuf buffer) throws IOException {
        try {
            switch (WListTest.stage.get()) {
                case 0 -> {
                    HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
                    synchronized (WListTest.token_lock) {
                        WListTest.token = ByteBufIOUtil.readUTF(buffer);
                        HLog.DefaultLogger.log("INFO", WListTest.token);
                        WListTest.token_lock.notify();
                    }
                }
                case 1 -> {
                    HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
                    HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
                }
            }
        } finally {
            synchronized (WListTest.stage) {
                WListTest.stage.getAndIncrement();
                WListTest.stage.notify();
            }
        }
    }
}
