package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

public final class WListTest {
    private WListTest() {
        super();
    }

    private static final @NotNull EventLoopGroup bossGroup = new NioEventLoopGroup(Math.max(2, Runtime.getRuntime().availableProcessors() >>> 1));
    private static final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1);

    public static void main(final String @NotNull [] args) throws Exception {
        GlobalConfiguration.initialize(null);
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(WListTest.bossGroup, WListTest.workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, GlobalConfiguration.getInstance().maxConnection());
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(WListServer.CodecExecutors, "LengthDecoder", new LengthFieldBasedFrameDecoder(WListServer.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast(WListServer.CodecExecutors, "LengthEncoder", new LengthFieldPrepender(4));
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] flag = new boolean[] {false};
        ChannelFuture future = serverBootstrap.bind(5212).addListener(f -> {
            flag[0] = !f.isSuccess();
            if (flag[0])
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to bind default port.", f.cause());
            latch.countDown();
        }).await();
        latch.await();
        if (flag[0])
            future = serverBootstrap.bind(0).sync();
        final SocketAddress address = future.channel().localAddress();
        HLog.DefaultLogger.log(HLogLevel.ENHANCED, "Listening on: ", address);
        HLog.DefaultLogger.log("", address.getClass());
        WListTest.bossGroup.shutdownGracefully();
        WListTest.workerGroup.shutdownGracefully();
        if (true) return;
        WListTest.wrapServerInitialize(() -> {
//            final DriverInterface<?> driver = Objects.requireNonNull(DriverManager.getDriver("123pan_136"));
//            HLog.DefaultLogger.log("",
//                    DriverManager_123pan.getFileInformation((DriverConfiguration_123Pan) driver.getConfiguration(), 2345490, null, null)
//            );
//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
            return null;
        });
    }

    static {
        HExceptionWrapper.addUncaughtExceptionListener((t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        GlobalConfiguration.initialize(null);
        DatabaseUtil.initialize(new File("data.db"));
        ConstantManager.quicklyInitialize(new ConstantSqlHelper(DatabaseUtil.getInstance()), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(DatabaseUtil.getInstance()), "initialize");
        UserManager.quicklyInitialize(new UserSqlHelper(DatabaseUtil.getInstance()), "initialize");
        DriverManager.initialize(new File("configs"));
        try {
            final Object obj = runnable.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            for (final DriverInterface<?> driver: DriverManager.getAllDrivers())
                WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> DriverManager.dumpConfigurationIfModified(driver.getConfiguration())));
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        }
    }
}
