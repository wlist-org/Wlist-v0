package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageServerCiphers;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandler;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandlerManager;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
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
import org.jetbrains.annotations.Nullable;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WListServer {
    public static final int FileTransferBufferSize = 4 << 20; // const
    public static final int MaxSizePerPacket = (64 << 10) + WListServer.FileTransferBufferSize;
    public static final @NotNull EventExecutorGroup CodecExecutors =
            new DefaultEventExecutorGroup(Math.max(1, Runtime.getRuntime().availableProcessors() >>> 1), new DefaultThreadFactory("CodecExecutors"));
    public static final @NotNull EventExecutorGroup ServerExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("ServerExecutors"));
    public static final @NotNull EventExecutorGroup IOExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("IOExecutors"));

    private static final @NotNull HLog logger = HLog.createInstance("ServerLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true, HMergedStream.getFileOutputStreamNoException(null));

    protected static @NotNull WListServer instance = new WListServer();
    public static synchronized @NotNull WListServer getInstance() {
        return WListServer.instance;
    }

    protected static final WListServer.@NotNull ServerChannelHandler handlerInstance = new ServerChannelHandler();

    protected final @NotNull EventExecutorGroup bossGroup = new NioEventLoopGroup(Math.max(2, Runtime.getRuntime().availableProcessors() >>> 1));
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
        serverBootstrap.option(ChannelOption.SO_BACKLOG, GlobalConfiguration.getInstance().maxConnection());
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) throws NoSuchPaddingException, NoSuchAlgorithmException {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(WListServer.CodecExecutors, "LengthDecoder", new LengthFieldBasedFrameDecoder(WListServer.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast(WListServer.CodecExecutors, "LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast(WListServer.CodecExecutors, "Cipher", new MessageServerCiphers(WListServer.MaxSizePerPacket));
                pipeline.addLast(WListServer.ServerExecutors, "ServerHandler", WListServer.handlerInstance);
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] flag = new boolean[] {false};
        ChannelFuture future = null;
        if (defaultPort != 0) {
            future = serverBootstrap.bind(defaultPort).addListener(f -> {
                flag[0] = !f.isSuccess();
                if (flag[0])
                    WListServer.logger.log(HLogLevel.WARN, "Failed to bind default port.", ParametersMap.create().add("defaultPort", defaultPort), f.cause());
                latch.countDown();
            }).await();
            latch.await();
        }
        if (defaultPort == 0 || flag[0])
            future = serverBootstrap.bind(0).sync();
        final InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
        this.address.initialize(address);
        WListServer.logger.log(HLogLevel.ENHANCED, "Listening on: ", address);
    }

    public synchronized void stop() {
        if (!this.address.isInitialized()) return;
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
                "address=" + this.address +
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

        protected static void write(final @NotNull Channel channel, final @NotNull MessageProto message) throws IOException {
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
            prefix.writeByte(message.cipher());
            ByteBufIOUtil.writeUTF(prefix, message.state().name());
            final ByteBuf buffer = message.appender().apply(prefix);
            WListServer.logger.log(HLogLevel.VERBOSE, "Write: ", channel.remoteAddress(), " len: ", buffer.readableBytes(), " cipher: ", MiscellaneousUtil.bin(message.cipher()));
            channel.writeAndFlush(buffer);
        }

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) throws ServerException {
            final Channel channel = ctx.channel();
            WListServer.logger.log(HLogLevel.VERBOSE, "Read: ", channel.remoteAddress(), " len: ", msg.readableBytes(), " cipher: ", MiscellaneousUtil.bin(msg.readByte()));
            try {
                msg.markReaderIndex();
                final Operation.Type type = Operation.valueOfType(ByteBufIOUtil.readUTF(msg));
                final ServerHandler handler = ServerHandlerManager.getHandler(type);
                final MessageProto res = handler.handle(channel, msg);
                if (msg.readableBytes() != 0)
                    WListServer.logger.log(HLogLevel.MISTAKE, "Unexpected discarded bytes: ", channel.id().asLongText(), " len: ", msg.readableBytes());
                ServerChannelHandler.write(channel, res);
            } catch (final IOException exception) {
                assert exception.getCause() instanceof IndexOutOfBoundsException;
                assert ByteBufIOUtil.class.getName().equals(exception.getStackTrace()[0].getClassName());
                ServerChannelHandler.directlyWriteMessage(channel, Operation.State.FormatError, exception.getMessage());
            }
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
            if (cause instanceof CodecException) {
                WListServer.logger.log(HLogLevel.MISTAKE, "Codec Exception at ", ctx.channel().remoteAddress(), ": ", cause.getMessage());
                ServerChannelHandler.directlyWriteMessage(ctx.channel(), Operation.State.FormatError, "Codec");
                return;
            }
            if (cause instanceof SocketException) {
                WListServer.logger.log(HLogLevel.WARN, "Socket Exception at ", ctx.channel().remoteAddress(), ": ", cause.getMessage());
                ctx.close();
                return;
            }
            if (cause instanceof ServerException)
                WListServer.logger.log(HLogLevel.WARN, "Exception at ", ctx.channel().remoteAddress(), ": ", cause);
            else
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), cause); // Logged by HUncaughtExceptionHelper.listener.
            ServerChannelHandler.directlyWriteMessage(ctx.channel(), Operation.State.ServerError, null);
        }

        protected static void directlyWriteMessage(final @NotNull Channel channel, final Operation.@NotNull State state, final @Nullable String message) {
            try {
                final MessageProto composition = ServerHandler.composeMessage(state, message);
                ServerChannelHandler.write(channel, composition);
            } catch (final IOException exception) {
                WListServer.logger.log(HLogLevel.ERROR, exception);
                channel.close();
            }
        }
    }
}
