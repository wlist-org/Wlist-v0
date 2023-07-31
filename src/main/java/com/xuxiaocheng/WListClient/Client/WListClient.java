package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Main;
import com.xuxiaocheng.WListClient.Server.MessageClientCiphers;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
import org.jetbrains.annotations.Nullable;

import javax.crypto.NoSuchPaddingException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public class WListClient implements WListClientInterface {
    public static final int FileTransferBufferSize = 4 << 20;
    public static final int MaxSizePerPacket = (64 << 10) + WListClient.FileTransferBufferSize;
    private static final @NotNull HLog logger = HLog.createInstance("ClientLogger",
            Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
            true);
    public static final @NotNull EventLoopGroup ClientThreadPool = new NioEventLoopGroup(1);

    protected final @NotNull SocketAddress address;
    private final @NotNull Channel channel;

    public WListClient(final @NotNull SocketAddress address) throws InterruptedException, ConnectException {
        super();
        this.address = address;
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(WListClient.ClientThreadPool);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        final AtomicBoolean uninitialized = new AtomicBoolean(true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) throws NoSuchPaddingException, NoSuchAlgorithmException {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LengthDecoder", new LengthFieldBasedFrameDecoder(WListClient.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast("LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("Cipher", new MessageClientCiphers(WListClient.MaxSizePerPacket, uninitialized));
                pipeline.addLast("ClientHandler", new ClientChannelInboundHandler(WListClient.this));
            }
        });
        this.channel = bootstrap.connect(this.address).sync().channel();
        synchronized (uninitialized) {
            while (uninitialized.get())
                uninitialized.wait();
        }
    }

    @Override
    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    private @Nullable ByteBuf receive = null;
    protected final @NotNull Object receiveLock = new Object();

    @Override
    public @NotNull ByteBuf send(final @Nullable ByteBuf msg) throws InterruptedException {
        if (msg != null) {
            WListClient.logger.log(HLogLevel.VERBOSE, "Write len: ", msg.readableBytes());
            this.channel.writeAndFlush(msg);
        }
        synchronized (this.receiveLock) {
            while (this.receive == null)
                this.receiveLock.wait();
            final ByteBuf r = this.receive;
            this.receive = null;
            return r;
        }
    }

    @Override
    public void close() {
        this.channel.close().syncUninterruptibly();
    }

    @Override
    public boolean isActive() {
        return this.channel.isActive();
    }

    public static class ClientChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final @NotNull WListClient client;

        private ClientChannelInboundHandler(final @NotNull WListClient client) {
            super();
            this.client = client;
        }

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) {
            WListClient.logger.log(HLogLevel.VERBOSE, "Read len: ", msg.readableBytes());
            synchronized (this.client.receiveLock) {
                if (this.client.receive != null)
                    this.client.receive.release();
                msg.retain();
                this.client.receive = msg;
                this.client.receiveLock.notify();
            }
        }

        @Override
        public @NotNull String toString() {
            return "ClientChannelInboundHandler{" +
                    "client=" + this.client +
                    "} " + super.toString();
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
            WListClient.logger.log(HLogLevel.FAULT, "Uncaught exception. thread: ", Thread.currentThread().getName(), cause);
            ctx.close();
        }
    }

    @Override
    public @NotNull String toString() {
        return "WListClient{" +
                "address=" + this.address +
                '}';
    }
}
