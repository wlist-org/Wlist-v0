package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Codecs.MessageServerCiphers;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Operations.ServerHandler;
import com.xuxiaocheng.WList.Server.Operations.ServerHandlerManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WListServer {
    public static final @NotNull EventExecutorGroup CodecExecutors =
            new DefaultEventExecutorGroup(Math.max(1, Runtime.getRuntime().availableProcessors() >>> 1), new DefaultThreadFactory("CodecExecutors"));
    public static final @NotNull EventExecutorGroup ServerExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("ServerExecutors"));
    public static final @NotNull EventExecutorGroup IOExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("IOExecutors"));

    private static final @NotNull HLog logger = HLog.create("ServerLogger");

    protected static @NotNull WListServer instance = new WListServer();
    public static synchronized @NotNull WListServer getInstance() {
        return WListServer.instance;
    }

    protected static final WListServer.@NotNull ServerChannelHandler handlerInstance = new ServerChannelHandler();

    protected final @NotNull EventExecutorGroup bossGroup = new NioEventLoopGroup(Math.max(1, Runtime.getRuntime().availableProcessors() >>> 1));
    protected final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1);
    protected final @NotNull CountDownLatch latch = new CountDownLatch(1);
    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("WListServerAddress");

    protected WListServer() {
        super();
    }

    public @NotNull HInitializer<InetSocketAddress> getAddress() {
        return this.address;
    }

    public void awaitStop() throws InterruptedException {
        this.latch.await();
    }

    public boolean awaitStop(final long timeout, final @NotNull TimeUnit unit) throws InterruptedException {
        return this.latch.await(timeout, unit);
    }

    public synchronized void start(final @IntRange(minimum = 0, maximum = 65535) int defaultPort) throws InterruptedException {
        if (this.latch.getCount() == 0) throw new IllegalStateException("Cannot start WList server twice in same process.");
        WListServer.logger.log(HLogLevel.INFO, "WListServer is starting...");
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(this.workerGroup, this.workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, ServerConfiguration.get().maxServerBacklog());
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(WListServer.CodecExecutors, "LengthDecoder", new LengthFieldBasedFrameDecoder(NetworkTransmission.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast(WListServer.CodecExecutors, "LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast(WListServer.CodecExecutors, "Cipher", new MessageServerCiphers());
                pipeline.addLast(WListServer.ServerExecutors, "ServerHandler", WListServer.handlerInstance);
            }
        });
        ChannelFuture future = null;
        boolean flag = false;
        if (defaultPort != 0) {
            future = serverBootstrap.bind(defaultPort).await();
            if (future.cause() != null) {
                flag = true;
                WListServer.logger.log(HLogLevel.WARN, "Failed to bind default port.", ParametersMap.create().add("defaultPort", defaultPort), future.cause());
            }
        }
        if (defaultPort == 0 || flag)
            future = serverBootstrap.bind(0).sync();
        final InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
        this.address.initialize(address);
        WListServer.logger.log(HLogLevel.ENHANCED, "Listening on: ", address);
    }

    public synchronized void stop() {
        if (this.address.uninitializeNullable() == null) return;
        WListServer.logger.log(HLogLevel.ENHANCED, "WListServer is stopping...");
        final Future<?>[] futures = new Future<?>[2];
        futures[0] = this.bossGroup.shutdownGracefully();
        futures[1] = this.workerGroup.shutdownGracefully();
        for (final Future<?> future: futures)
            future.syncUninterruptibly();
        WListServer.logger.log(HLogLevel.INFO, "WListServer stopped gracefully.");
        this.latch.countDown();
    }

    @Override
    public @NotNull String toString() {
        return "WListServer(TcpServer){" +
                "latch=" + this.latch +
                ", address=" + this.address +
                '}';
    }

    @ChannelHandler.Sharable
    public static class ServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelActive(final @NotNull ChannelHandlerContext ctx) {
            WListServer.logger.log(HLogLevel.DEBUG, "Active: ", ctx.channel().remoteAddress(), " (", ctx.channel().id().asLongText(), ')');
        }

        @Override
        public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
            WListServer.logger.log(HLogLevel.DEBUG, "Inactive: ", ctx.channel().remoteAddress(), " (", ctx.channel().id().asLongText(), ')');
        }

        public static void write(final @NotNull Channel channel, final @NotNull MessageProto message) {
            if (!channel.isOpen()) {
                channel.close();
                return;
            }
            final ByteBuf buffer;
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufIOUtil.writeUTF(prefix, message.state().name());
                buffer = message.appender() == null ? prefix : message.appender().apply(prefix);
                buffer.retain();
            } catch (final CodecException | SocketException exception) {
                channel.pipeline().fireExceptionCaught(exception);
                return;
            } catch (final IOException exception) {
                WListServer.logger.log(HLogLevel.ERROR, exception);
                channel.close();
                return;
            } finally {
                prefix.release();
            }
            channel.writeAndFlush(buffer);
        }

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) throws SQLException {
            final Channel channel = ctx.channel();
            final RunnableE core;
            try {
                final String rawType = ByteBufIOUtil.readUTF(msg);
                final OperationType type = OperationType.of(rawType);
                if (type == OperationType.Undefined) {
                    ServerHandler.logOperation(channel, OperationType.Undefined, null, () -> ParametersMap.create().add("type", rawType));
                    ServerChannelHandler.write(channel, MessageProto.Undefined);
                    core = null;
                } else
                    core = ServerHandlerManager.getHandler(type).extra(channel, msg);
                if (msg.readableBytes() != 0)
                    WListServer.logger.log(HLogLevel.MISTAKE, "Unexpected discarded bytes: ", channel.remoteAddress(), ParametersMap.create().add("len", msg.readableBytes()));
            } catch (final IOException exception) { // Read from msg.
                ServerChannelHandler.write(channel, MessageProto.FormatError);
                channel.close();
                return;
            }
            if (core == null)
                return;
            try {
                core.run();
            } catch (final UnsupportedOperationException exception) {
                ServerChannelHandler.write(channel, MessageProto.composeMessage(ResponseState.Unsupported, exception.getMessage()));
            } catch (final Exception exception) {
                ctx.fireExceptionCaught(exception);
            }
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
            final Channel channel = ctx.channel();
            if (cause instanceof CodecException || cause instanceof SocketException) {
                WListServer.logger.log(HLogLevel.MISTAKE, "Codec/Socket Exception at ", channel.remoteAddress(), ": ", cause.getLocalizedMessage());
                channel.close();
                return;
            }
            WListServer.logger.log(HLogLevel.ERROR, "Exception at ", channel.remoteAddress(), ": ", cause);
            ServerChannelHandler.write(channel, MessageProto.ServerError);
        }
    }
}
