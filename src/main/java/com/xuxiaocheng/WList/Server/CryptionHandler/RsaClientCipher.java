package com.xuxiaocheng.WList.Server.CryptionHandler;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RsaClientCipher extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private @Nullable RsaCipher cipher = null;
    private final @NotNull AtomicBoolean allow = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean uninitialized = new AtomicBoolean(true);

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws IOException {
        if (this.cipher == null) {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "WList/RSA");
            this.allow.set(true);
            ctx.channel().writeAndFlush(buffer);
        }
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
            final byte[] encoded = ByteBufIOUtil.readByteArray(msg);
            final RSAPublicKey key;
            try {
                key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
            } catch (final InvalidKeySpecException | NoSuchAlgorithmException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            this.cipher = new RsaCipher(key);
            synchronized (this.uninitialized) {
                this.uninitialized.set(false);
                this.uninitialized.notifyAll();
            }
            ctx.fireChannelActive();
        } else
            this.cipher.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "RsaClientCipher{" +
                "cipher=" + this.cipher +
                "} (" + super.toString() + ')';
    }
}
