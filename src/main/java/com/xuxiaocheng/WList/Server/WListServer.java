package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.CryptionHandler.AesCipher;
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

import java.io.IOException;
import java.net.SocketAddress;

public class WListServer {
    private static final @NotNull HLog logger = HLog.createInstance("ServerLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, HMergedStream.createNoException(true, null));

    public static final int FileTransferBufferSize = 4 << 20;

    public static final @NotNull EventExecutorGroup ServerExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("ServerExecutors"));
    public static final @NotNull EventExecutorGroup IOExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("IOExecutors"));

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventExecutorGroup bossGroup = new NioEventLoopGroup(1);
    protected final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(0);
    private ChannelFuture channelFuture;

    public WListServer(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    public synchronized @NotNull ChannelFuture start() throws InterruptedException {
        WListServer.logger.log(HLogLevel.DEBUG, "WListServer is starting...");
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(this.workerGroup, this.workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, GlobalConfiguration.getInstance().getMax_connection());
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final @NotNull SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LengthDecoder", new LengthFieldBasedFrameDecoder((64 << 10) + WListServer.FileTransferBufferSize, 0, 4, 0, 4));
                pipeline.addLast("LengthEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("Cipher", new AesCipher(WList.key, WList.vector));
                pipeline.addLast(WListServer.ServerExecutors, "ServerHandler", new ServerChannelInboundHandler());
            }
        });
        this.channelFuture = serverBootstrap.bind(this.address).sync();
        WListServer.logger.log(HLogLevel.INFO, "Listening on: ", this.address);
        return this.channelFuture.channel().closeFuture();
    }

    public synchronized void stop() throws InterruptedException {
        if (this.channelFuture == null)
            return;
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

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) {
            final Channel channel = ctx.channel();
            WListServer.logger.log(HLogLevel.VERBOSE, "Read: ", channel.id().asLongText(), " len: ", msg.readableBytes());
            try {
                final Operation.Type type = Operation.TypeMap.get(ByteBufIOUtil.readUTF(msg));
                WListServer.logger.log(HLogLevel.DEBUG, "Type: ", channel.id().asLongText(), " type: ", type);
                if (type == null || type == Operation.Type.Undefined)
                    ServerHandler.writeMessage(channel, Operation.State.Unsupported, "Undefined operation!");
                else
                    switch (type) {
                        case Login -> ServerHandler.doLogin(msg, channel);
                        case Register -> ServerHandler.doRegister(msg, channel);
                        case ChangePassword -> ServerHandler.doChangePassword(msg, channel);
                        case Logoff -> ServerHandler.doLogoff(msg, channel);
                        case AddPermission -> ServerHandler.doChangePermission(msg, channel, true);
                        case ReducePermission -> ServerHandler.doChangePermission(msg, channel, false);
                        case ListFiles -> ServerHandler.doListFiles(msg, channel);
                        case RequestDownloadFile -> ServerHandler.doRequestDownloadFile(msg, channel);
                        case DownloadFile -> ServerHandler.doDownloadFile(msg, channel);
                        case CancelDownloadFile -> ServerHandler.doCancelDownloadFile(msg, channel);
                        case MakeDirectories -> ServerHandler.doMakeDirectories(msg, channel);
                        case DeleteFile -> ServerHandler.doDeleteFile(msg, channel);
                        case RenameFile -> ServerHandler.doRenameFile(msg, channel);
                        // TODO
                        default -> ServerHandler.writeMessage(channel, Operation.State.Unsupported, "TODO: Unsupported.");
                    }
                if (msg.readableBytes() != 0)
                    WListServer.logger.log(HLogLevel.MISTAKE, "Unexpected discarded bytes: ", channel.id().asLongText(), " len: ", msg.readableBytes());
            } catch (final IOException exception) {
                ServerHandler.doException(channel, exception.getMessage());
            } catch (final ServerException exception) {
                WListServer.logger.log(HLogLevel.WARN, "Exception: ", channel.id().asLongText(), exception);
                ServerHandler.doException(channel, null);
            }
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final Throwable cause) {
            WListServer.logger.log(HLogLevel.WARN, "Exception: ", ctx.channel().id().asLongText(), cause);
            ServerHandler.doException(ctx.channel(), null);
        }

        @Override
        public @NotNull String toString() {
            return "ServerChannelInboundHandler{" + super.toString() + '}';
        }
    }
}
