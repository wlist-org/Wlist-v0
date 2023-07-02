package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageServerCiphers;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerFileHandler;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerHandler;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerStateHandler;
import com.xuxiaocheng.WList.Server.ServerHandlers.ServerUserHandler;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.WList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class WListServer {
    public static final int FileTransferBufferSize = 4 << 20; // const
    public static final int MaxSizePerPacket = (64 << 10) + WListServer.FileTransferBufferSize;
    public static final @NotNull EventExecutorGroup CodecExecutors =
            new DefaultEventExecutorGroup(Math.max(1, Runtime.getRuntime().availableProcessors() >>> 1), new DefaultThreadFactory("CodecExecutors"));
    public static final @NotNull EventExecutorGroup ServerExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("ServerExecutors"));
    public static final @NotNull EventExecutorGroup IOExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 3, new DefaultThreadFactory("IOExecutors"));

    private static final @NotNull HLog logger = HLog.createInstance("ServerLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
            true, WList.InIdeaMode ? null : HMergedStream.getFileOutputStreamNoException(""));

    protected static @Nullable WListServer instance;
    public static synchronized void init(final @NotNull SocketAddress address) {
        if (WListServer.instance != null)
            throw new IllegalStateException("WList server is initialized. instance: " + WListServer.instance + " address: " + address);
        WListServer.instance = new WListServer(address);
    }
    public static synchronized @NotNull WListServer getInstance() {
        if (WListServer.instance == null)
            throw new IllegalStateException("WList server is not initialized.");
        return WListServer.instance;
    }

    protected static final @NotNull WListServer.ServerChannelHandler handlerInstance = new ServerChannelHandler();

    protected final @NotNull SocketAddress address;
    protected final @NotNull EventExecutorGroup bossGroup = new NioEventLoopGroup(Math.max(2, Runtime.getRuntime().availableProcessors() >>> 1));
    protected final @NotNull EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1);
    protected final @NotNull ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    protected final @NotNull CountDownLatch latch = new CountDownLatch(1);

    protected WListServer(final @NotNull SocketAddress address) {
        super();
        this.address = address;
    }

    public @NotNull SocketAddress getAddress() {
        return this.address;
    }

    public void awaitStop() throws InterruptedException {
        this.latch.await();
    }

    public boolean awaitStop(final long timeout, final @NotNull TimeUnit unit) throws InterruptedException {
        return this.latch.await(timeout, unit);
    }

    public synchronized void start() throws InterruptedException {
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
        serverBootstrap.bind(this.address).sync();
        WListServer.logger.log(HLogLevel.ENHANCED, "Listening on: ", this.address);
    }

    public synchronized void stop() {
        if (this.latch.getCount() == 0)
            return;
        WListServer.logger.log(HLogLevel.ENHANCED, "WListServer is stopping...");
        this.channelGroup.close().syncUninterruptibly();
        this.bossGroup.shutdownGracefully().syncUninterruptibly();
        this.workerGroup.shutdownGracefully().syncUninterruptibly();
        WListServer.logger.log(HLogLevel.INFO, "WListServer stopped gracefully.");
        this.latch.countDown();
    }

    public @NotNull ChannelGroupFuture broadcast(final @NotNull ByteBuf msg) {
        return this.channelGroup.writeAndFlush(msg);
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
            WListServer.logger.log(HLogLevel.DEBUG, "Active: ", ctx.channel().id().asLongText(), " (", ctx.channel().remoteAddress(), ')');
        }

        @Override
        public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
            WListServer.logger.log(HLogLevel.DEBUG, "Inactive: ", ctx.channel().id().asLongText(), " (", ctx.channel().remoteAddress(), ')');
        }

        protected static void write(final @NotNull Channel channel, final @NotNull MessageProto message) throws IOException {
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
            prefix.writeByte(message.cipher());
            ByteBufIOUtil.writeUTF(prefix, message.state().name());
            final ByteBuf buffer = message.appender().apply(prefix);
            WListServer.logger.log(HLogLevel.VERBOSE, "Write: ", channel.id().asLongText(), " len: ", buffer.readableBytes(), " cipher: ", MiscellaneousUtil.bin(message.cipher()));
            channel.writeAndFlush(buffer);
        }

        @Override
        protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg) throws ServerException {
            final Channel channel = ctx.channel();
            WListServer.logger.log(HLogLevel.VERBOSE, "Read: ", channel.id().asLongText(), " len: ", msg.readableBytes(), " cipher: ", MiscellaneousUtil.bin(msg.readByte()));
            try {
                final Operation.Type type = Operation.valueOfType(ByteBufIOUtil.readUTF(msg));
                // TODO Better debugger.
                WListServer.logger.log(HLogLevel.DEBUG, "Operate: ", channel.id().asLongText(), " type: ", type, (Supplier<String>) () -> {
                    msg.markReaderIndex();
                    try {
                        return " user: " + ByteBufIOUtil.readUTF(msg);
                    } catch (final IOException ignore) {
                        return "";
                    } finally {
                        msg.resetReaderIndex();
                    }
                });
                final ServerHandler handler = switch (type) {
                    case Undefined -> b -> ServerHandler.composeMessage(Operation.State.Unsupported, "Undefined operation!");
                    case Broadcast -> ServerStateHandler.doBroadcast;
                    case SetBroadcastMode -> b -> ServerStateHandler.doSetBroadcastMode(b, WListServer.getInstance().channelGroup, channel);
                    case CloseServer -> ServerStateHandler.doCloseServer;
                    case Register -> ServerUserHandler.doRegister;
                    case Login -> ServerUserHandler.doLogin;
                    case GetPermissions -> ServerUserHandler.doGetPermissions;
                    case ChangeUsername -> ServerUserHandler.doChangeUsername;
                    case ChangePassword -> ServerUserHandler.doChangePassword;
                    case Logoff -> ServerUserHandler.doLogoff;
                    case ListUsers -> ServerUserHandler.doListUsers;
                    case DeleteUser -> ServerUserHandler.doDeleteUser;
                    case ListGroups -> ServerUserHandler.doListGroups;
                    case AddGroup -> ServerUserHandler.doAddGroup;
                    case DeleteGroup -> ServerUserHandler.doDeleteGroup;
                    case ChangeGroup -> ServerUserHandler.doChangeGroup;
                    case AddPermission -> b -> ServerUserHandler.doChangePermission(b, true);
                    case RemovePermission -> b -> ServerUserHandler.doChangePermission(b, false);
                    // TODO drivers operate. (dynamically modify config file)
                    case BuildIndex -> ServerFileHandler.doBuildIndex;
                    case ListFiles -> ServerFileHandler.doListFiles;
                    case MakeDirectories -> ServerFileHandler.doMakeDirectories;
                    case DeleteFile -> ServerFileHandler.doDeleteFile;
                    case RenameFile -> ServerFileHandler.doRenameFile;
                    case RequestDownloadFile -> ServerFileHandler.doRequestDownloadFile;
                    case DownloadFile -> ServerFileHandler.doDownloadFile;
                    case CancelDownloadFile -> ServerFileHandler.doCancelDownloadFile;
                    case RequestUploadFile -> ServerFileHandler.doRequestUploadFile;
                    case UploadFile -> ServerFileHandler.doUploadFile;
                    case CancelUploadFile -> ServerFileHandler.doCancelUploadFile;
                    case CopyFile -> ServerFileHandler.doCopyFile;
                    case MoveFile -> ServerFileHandler.doMoveFile;
                };
                final MessageProto res = handler.handle(msg);
                if (msg.readableBytes() != 0)
                    WListServer.logger.log(HLogLevel.MISTAKE, "Unexpected discarded bytes: ", channel.id().asLongText(), " len: ", msg.readableBytes());
                ServerChannelHandler.write(channel, res);
            } catch (final IOException exception) {
                ServerChannelHandler.directlyWriteMessage(channel, Operation.State.FormatError, exception.getMessage());
            }
        }

        @Override
        public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final Throwable cause) {
            if (cause instanceof CodecException) {
                WListServer.logger.log(HLogLevel.MISTAKE, "Codec Exception at ", ctx.channel().id().asLongText(), ": ", cause.getMessage());
                ServerChannelHandler.directlyWriteMessage(ctx.channel(), Operation.State.FormatError, "Codec");
                return;
            }
            if (cause instanceof SocketException) {
                WListServer.logger.log(HLogLevel.WARN, "Socket Exception at ", ctx.channel().id().asLongText(), ": ", cause.getMessage());
                ctx.close();
                return;
            }
            WListServer.logger.log(HLogLevel.WARN, "Exception at ", ctx.channel().id().asLongText(), ": ", cause);
            ServerChannelHandler.directlyWriteMessage(ctx.channel(), Operation.State.ServerError, null);
        }

        protected static void directlyWriteMessage(final @NotNull Channel channel, final @NotNull Operation.State state, final @Nullable String message) {
            try {
                final MessageProto composition = ServerHandler.composeMessage(state, message);
                ServerChannelHandler.write(channel, composition);
            } catch (final IOException exception) {
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
                channel.close();
            }
        }
    }
}
