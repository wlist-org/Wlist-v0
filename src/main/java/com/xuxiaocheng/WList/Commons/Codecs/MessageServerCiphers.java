package com.xuxiaocheng.WList.Commons.Codecs;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageServerCiphers extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private static final @NotNull String application = "WList@operation=0.2";

    protected final @NotNull AtomicBoolean initialized = new AtomicBoolean(false);
    protected boolean error = false;
    protected NetworkTransmission.AesKeyPair aesKeyPair;

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
        if (this.initialized.get())
            ctx.fireChannelInactive();
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<@NotNull Object> out) {
        if (this.error) {
            ctx.close().addListener(MiscellaneousUtil.exceptionListener());
            return;
        }
        if (!this.initialized.get()) {
            final Pair.ImmutablePair<ByteBuf, NetworkTransmission.AesKeyPair> pair = NetworkTransmission.serverStart(msg, MessageServerCiphers.application);
            final ChannelFuture future = ctx.writeAndFlush(pair.getFirst());
            if (pair.getSecond() == null) {
                future.addListener(ChannelFutureListener.CLOSE);
                this.error = true;
                return;
            }
            this.aesKeyPair = pair.getSecond();
            this.initialized.set(true);
            ctx.fireChannelActive();
            return;
        }
        assert this.aesKeyPair != null;
        final ByteBuf decrypted = NetworkTransmission.serverDecrypt(this.aesKeyPair, msg);
        if (decrypted == null)
            throw new IllegalStateException("Something went wrong when server decrypted message.");
        HLog.getInstance("ServerLogger").log(HLogLevel.VERBOSE, "Read: ", ctx.channel().remoteAddress(),
                ParametersMap.create().add("length", decrypted.readableBytes()).add("network", msg.readableBytes()));
        out.add(decrypted);
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<@NotNull Object> out) {
        if (this.error) {
            ctx.close().addListener(MiscellaneousUtil.exceptionListener());
            return;
        }
        assert this.aesKeyPair != null;
        final ByteBuf encrypted = NetworkTransmission.serverEncrypt(this.aesKeyPair, msg);
        if (encrypted == null)
            throw new IllegalStateException("Something went wrong when server encrypted message.");
        HLog.getInstance("ServerLogger").log(HLogLevel.VERBOSE, "Write: ", ctx.channel().remoteAddress(),
                ParametersMap.create().add("length", msg.readableBytes()).add("network", encrypted.readableBytes()));
        out.add(encrypted);
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "initialized=" + this.initialized +
                ", error=" + this.error +
                ", aesKeyPair=" + this.aesKeyPair +
                '}';
    }
}
