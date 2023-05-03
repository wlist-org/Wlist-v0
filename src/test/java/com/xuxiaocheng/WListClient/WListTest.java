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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class WListTest {
    @Test
    public void server() throws InterruptedException, IOException, SQLException {
        UserSqlHelper.init(ServerHandler.DefaultPermission, ServerHandler.AdminPermission);
        final WListServer server = new WListServer(new InetSocketAddress(5212));
        server.start();
        final WListClient client = new WListClient(new InetSocketAddress(5212));
        client.start();
        HLog.DefaultLogger.log("FINE", "**********START!**********");
        this.client(client.getChannel());
        HLog.DefaultLogger.log("FINE", "**********FINISH!**********");
        client.stop();
        server.stop();
    }

    protected void client(final @NotNull Channel channel) throws IOException, InterruptedException {
//        TimeUnit.SECONDS.sleep(3);
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, "Login");
        ByteBufIOUtil.writeUTF(buffer, "admin");
        ByteBufIOUtil.writeUTF(buffer, "XdWpP1Ow");
        channel.writeAndFlush(buffer);
        synchronized (WListTest.lock) {
            while (WListTest.lock.get())
                WListTest.lock.wait();
        }
    }

    static final AtomicBoolean lock = new AtomicBoolean(true);
}
