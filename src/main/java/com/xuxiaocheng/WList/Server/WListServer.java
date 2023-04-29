package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Exceptions.IllegalNetworkDataException;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.WList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
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

public class WListServer {
    protected static @Nullable WListServer instance;

    public static @NotNull WListServer getInstance(final @NotNull SocketAddress address) {
        if (WListServer.instance == null)
            WListServer.instance = new WListServer(address);
        return WListServer.instance;
    }

    private static final @NotNull HLog logger = HLog.createInstance("ServerLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, false));

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    protected final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(0);

    protected WListServer(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    private ChannelFuture channelFuture;
    protected static @NotNull EventExecutorGroup executors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("ServerExecutors"));

    public synchronized @NotNull ChannelFuture start() {
        WListServer.logger.log(HLogLevel.DEBUG, "WListServer is starting...");
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
                pipeline.addLast(WListServer.executors, "ServerHandler", new ServerChannelInboundHandler());
            }
        });
        this.channelFuture = serverBootstrap.bind(this.address).syncUninterruptibly();
        WListServer.logger.log(HLogLevel.VERBOSE, "WListServer started.");
        WListServer.logger.log(HLogLevel.INFO, "Listening on: ", this.address);
        return this.channelFuture.channel().closeFuture();
    }

    public synchronized void stop() throws InterruptedException {
        WListServer.logger.log(HLogLevel.DEBUG, "WListServer is stopping...");
        this.channelFuture.channel().close().sync();
        this.bossGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().sync();
        WListServer.logger.log(HLogLevel.INFO, "WListServer stopped gracefully.");
    }

    @Override
    public @NotNull String toString() {
        return "WListServer(TcpServer){" +
                "address=" + this.address +
                '}';
    }

    @ChannelHandler.Sharable
    public static class ServerChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(final @NotNull ChannelHandlerContext ctx) {
            final ChannelId id = ctx.channel().id();
            WListServer.logger.log(HLogLevel.DEBUG, "Active: ", id.asLongText());
            ServerHandler.doActive(id);
        }

        @Override
        public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
            final ChannelId id = ctx.channel().id();
            WListServer.logger.log(HLogLevel.DEBUG, "Inactive: ", id.asLongText());
            ServerHandler.doInactive(id);
        }

        @SuppressWarnings("OverlyBroadThrowsClause")
        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) throws Exception {
            final Channel channel = ctx.channel();
            WListServer.logger.log(HLogLevel.VERBOSE, "Read: ", channel.id().asLongText(), " len: ", msg.readableBytes());
            final Operation.Type type = Operation.TypeMap.get(ByteBufIOUtil.readUTF(msg));
            try {
                switch (type) {
                    case Undefined -> throw new IllegalNetworkDataException("Undefined operation!");
                    case LoginIn -> ServerHandler.doLoginIn(msg, channel);
                    case LoginOut -> ServerHandler.doLoginOut(msg, channel);
                    case Registry -> ServerHandler.doRegister(msg, channel);
//                case List -> ServerHandler.doList(msg, channel);
                    // TODO
                }
            } catch (final IllegalNetworkDataException exception) {
                ServerHandler.doException(channel, exception);
            }
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final Throwable cause) {
            WListServer.logger.log(HLogLevel.ERROR, "Exception: ", ctx.channel().id().asLongText(), cause);
            ctx.close();
        }

        @Override
        public @NotNull String toString() {
            return "ServerChannelInboundHandler{}";
        }
    }
}
