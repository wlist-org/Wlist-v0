package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.ServerHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.WList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
        WList.exit();
    }

    private static void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        if (true) {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "Login");
            ByteBufIOUtil.writeUTF(buffer, "admin");
            //noinspection SpellCheckingInspection
            ByteBufIOUtil.writeUTF(buffer, "lJbtzCGp");
            channel.writeAndFlush(buffer);
        }
        if (true){
            synchronized (WListTest._token) {
                WListTest._token.wait();
            }
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "RequestDownloadFile");
            ByteBufIOUtil.writeUTF(buffer, WListTest.token);
            ByteBufIOUtil.writeUTF(buffer, "/123pan/AutoCopy.zip");
            ByteBufIOUtil.writeVariableLenLong(buffer, 1378208);
            ByteBufIOUtil.writeVariableLenLong(buffer, 1378208+19);
            channel.writeAndFlush(buffer);
        }
        if (true){
            synchronized (WListTest.id) {
                while (WListTest.id.get() == 0)
                    WListTest.id.wait();
            }
            for (int i = 0; i < 5; ++i) {
                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
                ByteBufIOUtil.writeUTF(buffer, i == 3 ? "CancelDownloadFile" : "DownloadFile");
                ByteBufIOUtil.writeUTF(buffer, WListTest.token);
                ByteBufIOUtil.writeLong(buffer, WListTest.id.get());
                channel.writeAndFlush(buffer);
                synchronized (WListTest.s) {
                    while (WListTest.s.get() == 3)
                        WListTest.s.wait();
                    WListTest.s.decrementAndGet();
                }
            }
        }
//        TimeUnit.SECONDS.sleep(10);
    }

    private static final AtomicInteger s = new AtomicInteger(1);
    private static final Object _token = new Object();
    private static String token;
    private static final AtomicLong id = new AtomicLong(0);
    public static void client(final @NotNull ByteBuf buffer) throws IOException {
        switch (WListTest.s.getAndIncrement()) {
            case 1 -> {
                HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
                WListTest.token = ByteBufIOUtil.readUTF(buffer);
                HLog.DefaultLogger.log("INFO", WListTest.token);
                synchronized (WListTest._token) {
                    WListTest._token.notify();
                }
            }
            case 2 -> {
                HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readUTF(buffer));
                HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readVariableLenLong(buffer));
                final long id = ByteBufIOUtil.readLong(buffer);
                HLog.DefaultLogger.log("INFO", id);
                synchronized (WListTest.id) {
                    WListTest.id.set(id);
                    WListTest.id.notify();
                }
            }
            case 3 -> {
                final String state = ByteBufIOUtil.readUTF(buffer);
                HLog.DefaultLogger.log("INFO", state);
                if ("Success".equals(state)) {
                    try {
                        HLog.DefaultLogger.log("INFO", ByteBufIOUtil.readVariableLenInt(buffer));
                        final StringBuilder builder = new StringBuilder();
                        for (final byte b : ByteBufIOUtil.readByteArray(buffer)) {
                            final String hex = "0" + Integer.toHexString(b);
                            builder.append(hex.substring(hex.length() - 2)).append(" ");
                        }
                        HLog.DefaultLogger.log("DEBUG", builder.toString());
                    } catch (final IOException exception) {
                        assert exception.getCause() instanceof IndexOutOfBoundsException;
                        HLog.DefaultLogger.log("MISTAKE", exception.getCause().getMessage());
                    }
                }
                synchronized (WListTest.s) {
                    WListTest.s.notify();
                }
            }
        }
    }
}
