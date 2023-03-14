package com.xuxiaocheng.WList.Internal;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.WList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;

public class Server {
    protected static @Nullable Server instance;

    public static @NotNull Server getInstance(final @NotNull SocketAddress address) {
        if (Server.instance == null)
            Server.instance = new Server(address);
        return Server.instance;
    }

    private final @NotNull HLog logger = HLog.createInstance("ServerLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, false));

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    protected final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(0);

    protected Server(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    private ChannelFuture channelFuture;
    protected static @NotNull EventExecutorGroup executors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("ServerExecutors"));

    public synchronized @NotNull ChannelFuture start() {
        this.logger.log(HLogLevel.INFO, "Server starting...");
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(this.workerGroup, this.workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1 << 10);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast(new IdleStateHandler());
                pipeline.addLast("Decoder", new LengthFieldBasedFrameDecoder(1 << 20, 0, 4, 0, 4));
                pipeline.addLast("Encoder", new LengthFieldPrepender(4));
                pipeline.addLast(Server.executors, "handler", new ServerHandler());
            }
        });
        this.channelFuture = serverBootstrap.bind(this.address).syncUninterruptibly();
        this.logger.log(HLogLevel.FINE, "Server started.");
        this.logger.log(HLogLevel.INFO, "Listening on: ", this.address);
        return this.channelFuture.channel().closeFuture();
    }

    public synchronized void stop() throws InterruptedException {
        this.logger.log(HLogLevel.INFO, "Server stopping...");
        this.channelFuture.channel().close().sync();
        this.bossGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().sync();
        this.logger.log(HLogLevel.FINE, "Server stopped gracefully.");
    }

    @Override
    public @NotNull String toString() {
        return "Server(TcpServer){" +
                "address=" + this.address +
                '}';
    }

    @ChannelHandler.Sharable
    public static class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {


        @Override
        public void channelActive(final @NotNull ChannelHandlerContext ctx) {
            HLog.DefaultLogger.log("INFO", "Active");
        }

        @Override
        public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
            HLog.DefaultLogger.log("INFO", "Inactive");
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
            HLog.DefaultLogger.log("INFO", "Read: ", msg);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            HLog.DefaultLogger.log("INFO", cause);
            ctx.close();
        }
    }
}
