package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Server.CryptionHandler.AesCipher;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.WList;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.net.SocketAddress;

public class WListClient {
    private static final @NotNull HLog logger = HLog.createInstance("ClientLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, false));

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventLoopGroup group = new NioEventLoopGroup(1);
    private ChannelFuture channelFuture;

    public WListClient(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    public synchronized @NotNull ChannelFuture start() throws InterruptedException {
        WListClient.logger.log(HLogLevel.DEBUG, "WListClient is starting...");
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LengthDecoder", new LengthFieldBasedFrameDecoder(1 << 20, 0, 4, 0, 4));
                pipeline.addLast("LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("ClientCipher", new AesCipher(WList.key));
                pipeline.addLast("ClientHandler", new ClientChannelInboundHandler());
            }
        });
        this.channelFuture = bootstrap.connect(this.address).sync();
        WListClient.logger.log(HLogLevel.VERBOSE, "WListClient started.");
        return this.channelFuture.channel().closeFuture();
    }

    @TestOnly
    public synchronized Channel getChannel() {
        return this.channelFuture.channel();
    }

    public synchronized void stop() throws InterruptedException {
        if (this.channelFuture == null)
            return;
        WListClient.logger.log(HLogLevel.DEBUG, "WListClient is stopping...");
        this.channelFuture.channel().close().sync();
        this.group.shutdownGracefully().sync();
        WListClient.logger.log(HLogLevel.INFO, "WListClient stopped gracefully.");
    }

    public static class ClientChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) {
            while (true) {
                try {
                    HLog.DefaultLogger.log("FINE", ByteBufIOUtil.readUTF(msg));
                } catch (final IOException exception) {
                    break;
                }
            }
            synchronized (WListTest.lock) {
                WListTest.lock.set(false);
                WListTest.lock.notifyAll();
            }
        }
    }

    @Override
    public @NotNull String toString() {
        return "WListClient{" +
                "address=" + this.address +
                '}';
    }
}
