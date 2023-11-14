package com.xuxiaocheng.WList.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Codecs.MessageClientCiphers;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
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
import io.netty.util.concurrent.DefaultThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WListClient implements WListClientInterface {
    private static final @NotNull HLog logger = HLog.create("ClientLogger");

    protected final @NotNull EventLoopGroup clientEventLoop = new NioEventLoopGroup(1, new DefaultThreadFactory("ClientEventLoop"));
    protected final @NotNull SocketAddress address;
    private final @NotNull HInitializer<Channel> channel = new HInitializer<>("WListClientChannel");

    public WListClient(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    /**
     * Require call {@link #close()} even this throws any exception.
     */
    @Override
    public void open() throws IOException {
        this.channel.requireUninitialized(null);
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.clientEventLoop);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        final AtomicBoolean initialized = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LengthDecoder", new LengthFieldBasedFrameDecoder(NetworkTransmission.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast("LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("Cipher", new MessageClientCiphers(initialized, error));
                pipeline.addLast("ClientHandler", new ClientChannelInboundHandler(WListClient.this));
            }
        });
        try {
            final ChannelFuture future = bootstrap.connect(this.address).await();
            final Throwable throwable = future.cause();
            if (throwable != null) {
                if (throwable instanceof final IOException ioException)
                    throw ioException;
                throw new IOException(throwable);
            }
            this.channel.initialize(future.channel());
            synchronized (initialized) {
                while (!initialized.get())
                    initialized.wait();
            }
        } catch (final InterruptedException exception) {
            throw new IOException(exception);
        }
        if (error.get() != null) {
            if (error.get() instanceof final IOException exception)
                throw exception;
            throw new IOException(error.get());
        }
    }

    @Override
    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    private final @NotNull BlockingQueue<@NotNull ByteBuf> queue = new LinkedBlockingQueue<>();

    @Override
    public @NotNull ByteBuf send(final @Nullable ByteBuf msg) throws IOException, InterruptedException {
        if (!this.isActive() || this.channel.isNotInitialized())
            throw new IOException(I18NUtil.get("client.network.closed_client", this.address));
        if (msg != null) {
            final ChannelFuture future = this.channel.getInstance().writeAndFlush(msg).await();
            final Throwable throwable = future.cause();
            if (throwable != null) {
                if (throwable instanceof final IOException ioException)
                    throw ioException;
                throw new IOException(throwable);
            }
        }
        ByteBuf receive = null;
        while (receive == null && this.isActive())
            receive = this.queue.poll(1, TimeUnit.SECONDS);
        if (receive == null)
            throw new IOException(I18NUtil.get("client.network.closed_client", this.address));
        return receive;
    }

    @Override
    public void close() {
        final Channel channel = this.channel.uninitializeNullable();
        if (channel == null)
            return;
        channel.close().addListener(MiscellaneousUtil.exceptionListener()).addListener(f -> this.clientEventLoop.shutdownGracefully());
        if (!this.queue.isEmpty())
            HLog.getInstance("ClientLogger").log(HLogLevel.ERROR, "Still something in receiving queue.", ParametersMap.create().add("queue", this.queue));
        while (true) {
            final ByteBuf deleted = this.queue.poll();
            if (deleted == null)
                break;
            deleted.release();
        }
    }

    @Override
    public boolean isActive() {
        return this.channel.getInstance().isActive();
    }

    public static class ClientChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final @NotNull WListClient client;

        private ClientChannelInboundHandler(final @NotNull WListClient client) {
            super();
            this.client = client;
        }

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) {
            this.client.queue.add(msg.retain());
        }

        @Override
        public @NotNull String toString() {
            return "ClientChannelInboundHandler{" +
                    "client=" + this.client +
                    ", super=" + super.toString() +
                    '}';
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
            WListClient.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", Thread.currentThread().getName(), cause);
            this.client.close();
        }
    }

    @Override
    public @NotNull String toString() {
        return "WListClient{" +
                "address=" + this.address +
                '}';
    }
}
