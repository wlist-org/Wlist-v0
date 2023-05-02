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

public class CipherForServer extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private @Nullable AesCipher cipher = null;
    private boolean allow = false;
    private final @NotNull AtomicBoolean uninitialized = new AtomicBoolean(true);

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) {
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException, InterruptedException {
        if (this.cipher == null) {
            if (this.allow) {
                msg.retain();
                out.add(msg);
                this.allow = false;
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
            if (!"WList/Cipher".equals(magic)) {
                ctx.close();
                return;
            }
            ctx.fireChannelActive();
            final KeyPair keys = MiscellaneousUtil.generateRsaKeyPair(1024);
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByteArray(buffer, keys.getPublic().getEncoded());
            this.allow = true;
            ctx.channel().writeAndFlush(buffer);
            this.cipher = new AesCipher(keys.getPrivate());
            synchronized (this.uninitialized) {
                this.uninitialized.set(false);
                this.uninitialized.notifyAll();
            }
        } else
            this.cipher.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "CipherForServer{" +
                "cipher=" + this.cipher +
                "} (" + super.toString() + ')';
    }
}
