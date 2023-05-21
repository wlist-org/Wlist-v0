package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WListClient.Utils.AesCipher;
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

import java.net.SocketAddress;

public class WListClient {
    public static final int FileTransferBufferSize = 4 << 20;
    public static final int MaxSizePerPacket = (64 << 10) + WListClient.FileTransferBufferSize;
    private static final @NotNull HLog logger = HLog.createInstance("ClientLogger",
            Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
            true);

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventLoopGroup group = new NioEventLoopGroup(1);
    private final @NotNull Channel channel;

    public WListClient(final @NotNull SocketAddress address) throws InterruptedException {
        super();
        this.address = address;
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
                pipeline.addLast("LengthDecoder", new LengthFieldBasedFrameDecoder(WListClient.MaxSizePerPacket, 0, 4, 0, 4));
                pipeline.addLast("LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("Cipher", new AesCipher(Main.key, Main.vector, WListClient.MaxSizePerPacket));
                pipeline.addLast("ClientHandler", new ClientChannelInboundHandler(WListClient.this));
            }
        });
        this.channel = bootstrap.connect(this.address).sync().channel();
        WListClient.logger.log(HLogLevel.VERBOSE, "WListClient started.");
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    private @Nullable ByteBuf receive = null;
    protected final @NotNull Object receiveLock = new Object();

    public @NotNull ByteBuf send(final @NotNull ByteBuf buf) throws InterruptedException {
        synchronized (this.receiveLock) {
            this.channel.writeAndFlush(buf);
            while (this.receive == null)
                this.receiveLock.wait();
            final ByteBuf r = this.receive;
            this.receive = null;
            return r;
        }
    }

    public void stop() throws InterruptedException {
        WListClient.logger.log(HLogLevel.DEBUG, "WListClient is stopping...");
        this.channel.close().sync();
        this.group.shutdownGracefully().sync();
        WListClient.logger.log(HLogLevel.INFO, "WListClient stopped gracefully.");
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
            synchronized (this.client.receiveLock) { // TODO support Broadcast
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
    }

    @Override
    public @NotNull String toString() {
        return "WListClient{" +
                "address=" + this.address +
                '}';
    }
}
