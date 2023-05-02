package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.ServerHandler;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class WListTest {
    @Test
    public void server() throws InterruptedException, IOException, SQLException {
        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("run/config.yml")));
        UserSqlHelper.init(ServerHandler.DefaultPermission, ServerHandler.AdminPermission);
        final WListServer server = new WListServer(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
        server.start();
        final WListClient client = new WListClient(new InetSocketAddress(5212));
        client.start();
        this.client(client.getChannel());
        HLog.DefaultLogger.log("MISTAKE", "FINISH!");
        client.stop();
        server.stop();
    }

    protected void client(final @NotNull Channel channel) throws IOException, InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, "Login");
        ByteBufIOUtil.writeUTF(buffer, "admin");
        ByteBufIOUtil.writeUTF(buffer, "hxCi9BvH");
        channel.writeAndFlush(buffer);
        synchronized (WListTest.lock) {
            while (WListTest.wait)
                WListTest.lock.wait();
        }
    }

    protected static volatile boolean wait = true;
    protected static final Object lock = new Object();
}
