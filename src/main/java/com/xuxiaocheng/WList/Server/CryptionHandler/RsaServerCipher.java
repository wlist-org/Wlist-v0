package com.xuxiaocheng.WList.Server.CryptionHandler;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RsaServerCipher extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private @Nullable RsaCipher cipher = null;
    private final @NotNull AtomicBoolean allow = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean uninitialized = new AtomicBoolean(true);

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) {
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException, InterruptedException {
        if (this.cipher == null) {
            if (this.allow.get()) {
                msg.retain();
                out.add(msg);
                this.allow.set(false);
                return;
            }
            synchronized (this.uninitialized) {
                while (this.uninitialized.get())
                    this.uninitialized.wait();
            }
        }
        this.cipher.encode(ctx, msg, out);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.cipher == null) {
            assert this.uninitialized.get();
            final String magic = ByteBufIOUtil.readUTF(msg);
            if (!"WList/RSA".equals(magic)) {
                ctx.close();
                return;
            }
            ctx.fireChannelActive();
            final KeyPair keys = MiscellaneousUtil.generateRsaKeyPair(1024);
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByteArray(buffer, keys.getPublic().getEncoded());
            this.allow.set(true);
            ctx.channel().writeAndFlush(buffer);
            this.cipher = new RsaCipher(keys.getPrivate());
            synchronized (this.uninitialized) {
                this.uninitialized.set(false);
                this.uninitialized.notifyAll();
            }
        } else
            this.cipher.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "RsaServerCipher{" +
                "cipher=" + this.cipher +
                "} (" + super.toString() + ')';
    }
}
